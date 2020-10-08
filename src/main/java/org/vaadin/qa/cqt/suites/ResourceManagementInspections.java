/*
 * Copyright (c) 2020 Artem Godin
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.vaadin.qa.cqt.suites;

import org.vaadin.qa.cqt.Suite;
import org.vaadin.qa.cqt.annotations.Advice;
import org.vaadin.qa.cqt.annotations.ProbableError;
import org.vaadin.qa.cqt.annotations.Scopes;
import org.vaadin.qa.cqt.annotations.Warning;
import org.vaadin.qa.cqt.data.Reference;
import org.vaadin.qa.cqt.data.ReferenceType;

import java.lang.annotation.Annotation;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Timer;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;

/**
 * Created by Artem Godin on 9/21/2020.
 */
@SuppressWarnings("unchecked")
public final class ResourceManagementInspections extends Suite {

    /**
     * {@link AutoCloseable} resource stored in a field makes it impossible to
     * use {@literal try-with-resources} pattern and might result in resource
     * leak.
     *
     * @return predicate
     */
    @ProbableError("AutoClosable resource stored in a field")
    @Scopes(exclude = {"static", "singleton"})
    public Predicate<Reference> field_holds_AutoCloseable() {
        return targetType(is(AutoCloseable.class));
    }

    /**
     * In a dynamic system like an application server or OSGI, you should take
     * good care not to prevent {@link ClassLoader} from garbage collection. As
     * you undeploy and redeploy individual applications in an application
     * server you create new class loaders for them. The old ones are unused and
     * should be collected. Java isn't going to let that happen if there is a
     * single dangling reference from container code into your application
     * code.
     * <p>
     * See: https://www.odi.ch/prog/design/newbies.php#56
     *
     * @return predicate
     */
    @Warning("ClassLoader stored in a static field")
    @Scopes({"static", "singleton"})
    public Predicate<Reference> field_holds_ClassLoader() {
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
                                field(type(isNot(
                                        WeakReference.class,
                                        SoftReference.class
                                )))
                        ),
                        and(
                                referenceTypeIs(
                                        ReferenceType.ACTUAL_VALUE,
                                        ReferenceType.POSSIBLE_VALUE,
                                        ReferenceType.MAP_VALUE
                                ),
                                ownerType(isNot(Annotation.class))
                        )
                )
        );
    }

    /**
     * In a dynamic system like an application server or OSGI, you should take
     * good care not to prevent {@link ClassLoader} from garbage collection. As
     * you undeploy and redeploy individual applications in an application
     * server you create new class loaders for them. The old ones are unused and
     * should be collected. Java isn't going to let that happen if there is a
     * single dangling reference from container code into your application
     * code.
     * <p>
     * Note that {@link Class} stores reference to {@link ClassLoader}
     * internally.
     * <p>
     * See: https://www.odi.ch/prog/design/newbies.php#56
     *
     * @return predicate
     */
    @Warning("Class stored in static field")
    @Scopes({"static", "singleton"})
    public Predicate<Reference> classReference() {
        return and(
                targetType(
                        is(Class.class),
                        clazz -> !clazz
                                .getPackage()
                                .getName()
                                .startsWith("java.")
                ),
                field(isStatic()),
                or(
                        and(
                                referenceTypeIs(ReferenceType.MAP_KEY),
                                field(type(isNot(WeakHashMap.class)))
                        ),
                        and(
                                referenceTypeIs(ReferenceType.REFERENCE_VALUE),
                                field(type(isNot(
                                        WeakReference.class,
                                        SoftReference.class
                                )))
                        ),
                        and(
                                referenceTypeIs(
                                        ReferenceType.ACTUAL_VALUE,
                                        ReferenceType.POSSIBLE_VALUE,
                                        ReferenceType.MAP_VALUE
                                ),
                                ownerType(isNot(Annotation.class))
                        )
                )
        );
    }

    /**
     * {@link ExecutorService} is a resource and must be closed explicitly via
     * {@literal try-with-resources} or {@literal try-finally} statement.
     * Failure to shutdown an {@link ExecutorService} might lead to a thread
     * leak even if an {@link ExecutorService} object is no longer accessible,
     * because some implementations (such as {@link ThreadPoolExecutor})
     * shutdown themselves in a finalizer, while {@code finalize()} is not
     * guaranteed to ever be called by the JVM. To make explicit shutdown
     * possible, first, {@link ExecutorService} objects must not be assinged
     * into variables and fields of {@link Executor} type.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#explicit-shutdown
     *
     * @return predicate
     */
    @ProbableError("ExecutorService resource stored in a field")
    public Predicate<Reference> field_holds_ExecutorService() {
        return and(
                targetType(is(ExecutorService.class)),
                field(isNotStatic())
        );
    }

    /**
     * If one of the application classes stores a value in {@link ThreadLocal}
     * variable and doesn’t remove it after the task at hand is completed, a
     * copy of that Object will remain with the {@link Thread} (from the
     * application server thread pool). Since lifespan of the pooled {@link
     * Thread} surpasses that of the application, it will prevent the object and
     * thus a {@link ClassLoader} being responsible for loading the application
     * from being garbage collected. And we have created a leak, which has a
     * chance to surface in a good old {@code java.lang.OutOfMemoryError:
     * PermGen space form}.
     * <p>
     * See: https://plumbr.io/blog/locked-threads/how-to-shoot-yourself-in-foot-with-threadlocals
     *
     * @return predicate
     */
    @ProbableError("ThreadLocal value is not removed after work is done")
    public Predicate<Reference> non_clean_ThreadLocal() {
        return and(
                referenceTypeIs(
                        ReferenceType.WAITING_THREAD_LOCAL,
                        ReferenceType.TERMINATED_THREAD_LOCAL
                ),
                target(Objects::nonNull)
        );
    }

    /**
     * It possible to reuse executors by creating them one level up the stack
     * and passing shared executors to constructors of the short-lived objects,
     * or a shared {@link ExecutorService} stored in a static field.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#reuse-threads
     *
     * @return predicate
     */
    @Warning("Thread must not be managed by short-lived objects")
    @Scopes(exclude = {"static", "singleton"})
    public Predicate<Reference> field_holds_Thread() {
        return and(
                targetType(is(Thread.class)),
                field(isNotStatic())
        );
    }

    /**
     * {@link Timer} spwans a new thread in its constructor. The new {@link
     * Thread} will inherit some properties from its parent: context
     * classloader, inheritable {@code ThreadLocals}, and some security
     * properties (access rights). It is therefore rarely desireable to have
     * those property set in an uncontrolled way. This may for instance prevent
     * GC of a class loader. The static initializer is executed by the thread
     * that first loads the class (in any given {@link ClassLoader}), which may
     * be a totally random thread from a thread pool of a webserver for example.
     * If you want to control these thread properties you will have to start
     * threads in a static method, and take control of who is calling that
     * method.
     * <p>
     * See: https://www.odi.ch/prog/design/newbies.php#54
     *
     * @return predicate
     */
    @ProbableError("Spawning thread from static initializers")
    @Scopes("static")
    public Predicate<Reference> thread_started_in_class_initializer() {
        return and(
                targetType(is(Timer.class)),
                field(
                        isStatic(),
                        isFinal()
                )
        );
    }

    /**
     * {@link ForkJoinPool} is more scalable because internally it maintains one
     * queue per each worker thread, whereas {@link ThreadPoolExecutor} has a
     * single, blocking task queue shared among all threads. {@link
     * ForkJoinPool} implements {@link ExecutorService} as well as {@link
     * ThreadPoolExecutor}, so could often be a drop in replacement.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#fjp-instead-tpe
     *
     * @return predicate
     */
    @Advice("Use a ForkJoinPool instead of a ThreadPoolExecutor with N threads")
    public Predicate<Reference> use_ForkJoinPool_instead_of_fixed_ThreadPoolExecutor() {
        return and(
                targetType(is(ThreadPoolExecutor.class)),
                target(Objects::nonNull),
                target(o -> {
                    if (o instanceof ThreadPoolExecutor) {
                        ThreadPoolExecutor tpe = (ThreadPoolExecutor) o;
                        return tpe.getCorePoolSize()
                                == tpe.getMaximumPoolSize();
                    }
                    return false;
                })
        );
    }

}
