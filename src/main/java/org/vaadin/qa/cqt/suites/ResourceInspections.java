package org.vaadin.qa.cqt.suites;

import org.vaadin.qa.cqt.Reference;
import org.vaadin.qa.cqt.ReferenceType;
import org.vaadin.qa.cqt.Suite;
import org.vaadin.qa.cqt.annotations.Advice;
import org.vaadin.qa.cqt.annotations.Disabled;
import org.vaadin.qa.cqt.annotations.Scopes;
import org.vaadin.qa.cqt.annotations.Warning;

import java.lang.annotation.Annotation;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Timer;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;

/**
 * Created by Artem Godin on 9/21/2020.
 */
@SuppressWarnings("unchecked")
public final class ResourceInspections extends Suite {

    /**
     * java.util.Timer spwans a new thread in its constructor.
     * The new Thread will inherit some properties from its parent: context classloader, inheritable ThreadLocals, and some security properties (access rights).
     * It is therefore rarely desireable to have those property set in an uncontrolled way.
     * This may for instance prevent GC of a class loader.
     * The static initializer is executed by the thread that first loads the class (in any given ClassLoader), which may be a totally random thread from a thread pool of a webserver for example.
     * If you want to control these thread properties you will have to start threads in a static method, and take control of who is calling that method.
     * <p>
     * See: https://www.odi.ch/prog/design/newbies.php#54
     *
     * @return
     */
    @Warning("Spawning thread from static initializers")
    @Scopes("static")
    public Predicate<Reference> staticTimer() {
        return and(
                targetType(is(Timer.class)),
                field(isStatic(), isFinal())
        );
    }

    /**
     * In a dynamic system like an application server or OSGI, you should take good care not to prevent ClassLoaders from garbage collection.
     * As you undeploy and redeploy individual applications in an application server you create new class loaders for them.
     * The old ones are unused and should be collected. Java isn't going to let that happen if there is a single dangling reference from container code into your application code.
     * Note that Class stores reference to ClassLoader internally.
     * <p>
     * See: https://www.odi.ch/prog/design/newbies.php#56
     *
     * @return
     */
    @Warning("Holding strong references to ClassLoader")
    @Scopes({"static", "singleton"})
    public Predicate<Reference> classLoaderReference() {
        return and(
                targetType(is(ClassLoader.class)),
                field(isStatic()),
                or(
                        and(
                                referenceTypeIs(ReferenceType.MAP_KEY),
                                field(type(isNot(WeakHashMap.class)))
                        ),
                        and(
                                referenceTypeIs(ReferenceType.REFERENCE_VALUE),
                                field(type(isNot(WeakReference.class, SoftReference.class)))
                        ),
                        and(
                                referenceTypeIs(ReferenceType.ACTUAL_VALUE, ReferenceType.MAP_VALUE),
                                ownerType(isNot(Annotation.class))
                        )
                )
        );
    }

    /**
     * In a dynamic system like an application server or OSGI, you should take good care not to prevent ClassLoaders from garbage collection.
     * As you undeploy and redeploy individual applications in an application server you create new class loaders for them.
     * The old ones are unused and should be collected. Java isn't going to let that happen if there is a single dangling reference from container code into your application code.
     *
     * Note that Class stores reference to ClassLoader internally.
     * <p>
     * See: https://www.odi.ch/prog/design/newbies.php#56
     *
     * @return
     */
    @Warning("Holding strong references to Class, Field, Method or Annotation")
    @Scopes({"static", "singleton"})
    public Predicate<Reference> classReference() {
        return and(
                targetType(is(Class.class), clazz -> !clazz.getPackage().getName().startsWith("java.")),
                field(isStatic()),
                or(
                        and(
                                referenceTypeIs(ReferenceType.MAP_KEY),
                                field(type(isNot(WeakHashMap.class)))
                        ),
                        and(
                                referenceTypeIs(ReferenceType.REFERENCE_VALUE),
                                field(type(isNot(WeakReference.class, SoftReference.class)))
                        ),
                        and(
                                referenceTypeIs(ReferenceType.ACTUAL_VALUE, ReferenceType.MAP_VALUE),
                                ownerType(isNot(Annotation.class))
                        )
                )
        );
    }

    /**
     * ForkJoinPool is more scalable because internally it maintains one queue per each worker thread, whereas ThreadPoolExecutor has a single, blocking task queue shared among all threads.
     * ForkJoinPool implements ExecutorService as well as ThreadPoolExecutor, so could often be a drop in replacement.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#fjp-instead-tpe
     *
     * @return
     */
    @Advice("Use a ForkJoinPool instead of a ThreadPoolExecutor with N threads")
    public Predicate<Reference> useForkJoinPool() {
        return and(
                targetType(is(ThreadPoolExecutor.class)),
                target(Objects::nonNull),
                target(o -> {
                    if (o instanceof ThreadPoolExecutor) {
                        ThreadPoolExecutor tpe = (ThreadPoolExecutor) o;
                        return tpe.getCorePoolSize() == tpe.getMaximumPoolSize();
                    }
                    return false;
                })
        );
    }

    /**
     * ExecutorService is a resource and must be closed explicitly via try-with-resources or try-finally statement.
     * Failure to shutdown an ExecutorService might lead to a thread leak even if an ExecutorService object is no longer accessible, because some implementations (such as ThreadPoolExecutor) shutdown themselves in a finalizer, while finalize() is not guaranteed to ever be called by the JVM.
     * To make explicit shutdown possible, first, ExecutorService objects must not be assinged into variables and fields of Executor type.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#explicit-shutdown
     *
     * @return
     */
    @Disabled
    @Warning("Store ExecutorService in fields of type ExecutorService or more specific")
    public Predicate<Reference> executorServiceFieldType() {
        return and(
                targetType(is(ExecutorService.class)),
                field(
                        type(is(ExecutorService.class)),
                        isNotStatic()
                )
        );
    }

    /**
     * It possible to reuse executors by creating them one level up the stack and passing shared executors to constructors of the short-lived objects, or a shared ExecutorService stored in a static field.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#reuse-threads
     *
     * @return
     */
    @Warning("Thread or ExecutorService must not be managed by short-lived objects")
    @Scopes(exclude = {"static", "singleton"})
    public Predicate<Reference> shortLivedThreadReference() {
        return targetType(is(Thread.class, ExecutorService.class));
    }

    @Disabled
    @Warning("AutoClosable resource stored in a field")
    public Predicate<Reference> autoClosableField() {
        return targetType(is(AutoCloseable.class));
    }

    /**
     * If one of the application classes stores a value in ThreadLocal variable and doesn’t remove it after the task at hand is completed, a copy of that Object will remain with the Thread (from the application server thread pool). Since lifespan of the pooled Thread surpasses that of the application, it will prevent the object and thus a ClassLoader being responsible for loading the application from being garbage collected. And we have created a leak, which has a chance to surface in a good old java.lang.OutOfMemoryError: PermGen space form.
     * <p>
     * See: https://plumbr.io/blog/locked-threads/how-to-shoot-yourself-in-foot-with-threadlocals
     *
     * @return
     */
    @Warning("ThreadLocal value is not removed after work is done")
    public Predicate<Reference> nonCleanThreadLocal() {
        return and(
                referenceTypeIs(ReferenceType.WAITING_THREAD_LOCAL, ReferenceType.TERMINATED_THREAD_LOCAL),
                target(Objects::nonNull)
        );
    }

}
