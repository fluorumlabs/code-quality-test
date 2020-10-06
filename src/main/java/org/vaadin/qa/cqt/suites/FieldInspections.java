package org.vaadin.qa.cqt.suites;

import org.vaadin.qa.cqt.Reference;
import org.vaadin.qa.cqt.ReferenceType;
import org.vaadin.qa.cqt.Suite;
import org.vaadin.qa.cqt.annotations.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.Format;
import java.util.Calendar;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Created by Artem Godin on 9/21/2020.
 */
@SuppressWarnings("unchecked")
public final class FieldInspections extends Suite {

    /**
     * All non-static f`ields of object implementing `Serializable` must be either serializable or transient.
     * Failure to do so will lead to `NotSerializableException` when an attempt to serialize will be made.
     *
     * @return
     */
    @Warning("Actual value of non-transient field in serializable object is not Serializable")
    @Scopes(exclude = "static")
    public Predicate<Reference> nonSerializableField() {
        return and(
                field(
                        isNotStatic(),
                        isNotTransient(),
                        declaringClass(is(Serializable.class))
                ),
                targetType(
                        isNotPrimitive(),
                        isNot(Serializable.class)
                ),
                target(Objects::nonNull),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE),
                ownerType(hasMethod("writeObject", ObjectOutputStream.class).negate())
        );
    }

    @ProbableError("Transient field is not initialized after deserialization")
    @Scopes(exclude = "static")
    public Predicate<Reference> transientFieldNotInitialized() {
        return and(
                field(
                        isNotStatic(),
                        isTransient(),
                        declaringClass(is(Serializable.class)),
                        modifiedByNonConstructor().negate().or(isFinal())
                ),
                ownerType(hasMethod("readObject", ObjectInputStream.class).negate())
        );
    }

    /**
     * Non-final fields make service stateful and allow the state to be mutated externally.
     *
     * Care must be taken to properly initialize fields of static, singleton- and session-scoped objects.
     * Consider using double-checked locking following SafeLocalDCL (http://hg.openjdk.java.net/code-tools/jcstress/file/9270b927e00f/tests-custom/src/main/java/org/openjdk/jcstress/tests/singletons/SafeLocalDCL.java#l71), or
     * UnsageLocalDCL (http://hg.openjdk.java.net/code-tools/jcstress/file/9270b927e00f/tests-custom/src/main/java/org/openjdk/jcstress/tests/singletons/UnsafeLocalDCL.java#l71).
     *
     * See: https://github.com/code-review-checklists/java-concurrency#safe-local-dcl
     *
     * @return
     */
    @Warning("Non-final instance field can be modified: stateful shared object")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> nonFinal() {
        return and(
                field(
                        isNotStatic(),
                        isNotAnnotatedWith("org.springframework.beans.factory.annotation.Value"),
                        isNotFinal(),
                        isNotTransient(),
                        declaringClass(isNotSyntheticClass()),
                        modifiedByNonConstructor()
                                .or(isUpdatedInOtherClasses())
                ),
                ownerType(isNotAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties"))
        );
    }
    /**
     * Non-final static fields make class stateful and allow the state to be mutated externally.
     *
     * Care must be taken to properly initialize fields of static, singleton- and session-scoped objects.
     * Consider using double-checked locking following SafeLocalDCL (http://hg.openjdk.java.net/code-tools/jcstress/file/9270b927e00f/tests-custom/src/main/java/org/openjdk/jcstress/tests/singletons/SafeLocalDCL.java#l71), or
     * UnsageLocalDCL (http://hg.openjdk.java.net/code-tools/jcstress/file/9270b927e00f/tests-custom/src/main/java/org/openjdk/jcstress/tests/singletons/UnsafeLocalDCL.java#l71).
     *
     * See: https://github.com/code-review-checklists/java-concurrency#safe-local-dcl
     *
     * @return
     */
    @ProbableError("Static non-final instance field can be modified: stateful shared object")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> nonFinalStatic() {
        return and(
                field(
                        isStatic(),
                        isNotAnnotatedWith("org.springframework.beans.factory.annotation.Value"),
                        isNotFinal(),
                        declaringClass(isNotSyntheticClass()),
                        modifiedByNonClassInit()
                                .or(isUpdatedInOtherClasses())
                )
        );
    }

    @ProbableError("Not thread-safe class")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> nonThreadSafe() {
        return and(
                referenceTypeIsNot(ReferenceType.THREAD_LOCAL, ReferenceType.TERMINATED_THREAD_LOCAL, ReferenceType.WAITING_THREAD_LOCAL),
                targetType(is(
                        Format.class,
                        Calendar.class,
                        StringBuilder.class,
                        ThreadGroup.class
                ))
        );
    }
}
