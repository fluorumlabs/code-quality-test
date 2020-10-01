package org.vaadin.qa.cqt.suites;

import org.vaadin.qa.cqt.Reference;
import org.vaadin.qa.cqt.ReferenceType;
import org.vaadin.qa.cqt.Suite;
import org.vaadin.qa.cqt.annotations.Advice;
import org.vaadin.qa.cqt.annotations.Disabled;
import org.vaadin.qa.cqt.annotations.Scopes;
import org.vaadin.qa.cqt.annotations.Warning;

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
    @Disabled
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
                target(Objects::nonNull)
        );
    }

    /**
     * Having non-private setters for fields makes class/service stateful and allows the state to be mutated externally.
     * This opens up possibilities for race conditions that are hard to trace.
     *
     * @return
     */
    @Disabled
    @Warning("Non-final field exposed via non-private setter")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> exposedSetterNonFinal() {
        return field(
                isNotFinal(),
                setter(isNotPrivate())
        );
    }

    /**
     * Having non-private fields allows the state to be mutated externally.
     * This opens up possibilities for race conditions that are hard to trace.
     *
     * @return
     */
    @Disabled
    @Warning("Non-final field exposed via non-private setter")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> exposedNonFinal() {
        return field(
                isNotFinal(),
                isNotPrivate()
        );
    }

    /**
     * Care must be taken to properly initialize fields of static, singleton- and session-scoped objects.
     * Consider using double-checked locking following SafeLocalDCL (http://hg.openjdk.java.net/code-tools/jcstress/file/9270b927e00f/tests-custom/src/main/java/org/openjdk/jcstress/tests/singletons/SafeLocalDCL.java#l71), or
     * UnsageLocalDCL (http://hg.openjdk.java.net/code-tools/jcstress/file/9270b927e00f/tests-custom/src/main/java/org/openjdk/jcstress/tests/singletons/UnsafeLocalDCL.java#l71).
     * See: https://github.com/code-review-checklists/java-concurrency#safe-local-dcl
     *
     * @return
     */
    @Disabled
    @Advice("Non-final field: check for correct initialization and synchronization")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> nonFinal() {
        return field(
                isNotAnnotatedWith("org.springframework.beans.factory.annotation.Value"),
                isNotFinal(),
                isNotTransient(),
                type(isNotPrivateClass(), isNot(String.class)),
                declaringClass(isNotSyntheticClass())
        );
    }

    @Warning("Not thread-safe class")
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
