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

package com.github.fluorumlabs.cqt.suites;

import com.github.fluorumlabs.cqt.Suite;
import com.github.fluorumlabs.cqt.annotations.ProbableError;
import com.github.fluorumlabs.cqt.annotations.Scopes;
import com.github.fluorumlabs.cqt.annotations.Warning;
import com.github.fluorumlabs.cqt.data.ReferenceType;
import com.github.fluorumlabs.cqt.utils.PredicateUtils;
import com.github.fluorumlabs.cqt.data.Reference;

import java.io.NotSerializableException;
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
     * Non-final fields make service stateful and allow the state to be mutated
     * externally.
     * <p>
     * Care must be taken to properly initialize fields of static, singleton-
     * and session-scoped objects. Consider using double-checked locking
     * following {@literal SafeLocalDCL} (http://hg.openjdk.java.net/code-tools/jcstress/file/9270b927e00f/tests-custom/src/main/java/org/openjdk/jcstress/tests/singletons/SafeLocalDCL.java#l71),
     * or {@literal UnsageLocalDCL} (http://hg.openjdk.java.net/code-tools/jcstress/file/9270b927e00f/tests-custom/src/main/java/org/openjdk/jcstress/tests/singletons/UnsafeLocalDCL.java#l71).
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#safe-local-dcl
     *
     * @return predicate
     */
    @Warning("Non-final field can be modified: stateful shared object")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> mutable_object() {
        return and(
                field(
                        isNotAnnotatedWith("org.springframework.beans.factory.annotation.Value"),
                        isNotFinal(),
                        isNotTransient(),
                        declaringClass(isNotSyntheticClass()),
                        PredicateUtils.or(
                                modifiedByNonConstructor()
                                        .or(isUpdatedInOtherClasses())
                                        .and(isNotStatic()),
                                modifiedByNonClassInit()
                                        .or(isUpdatedInOtherClasses())
                                        .and(isStatic())
                        )
                ),
                field(isStatic()).or(ownerType(isNotAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")))
        );
    }

    /**
     * All non-static fields of object implementing {@link Serializable} must be
     * either serializable or transient. Failure to do so will lead to {@link
     * NotSerializableException} when an attempt to serialize will be made.
     *
     * @return predicate
     */
    @Warning(
            "Actual value of non-transient field in serializable object is not Serializable")
    @Scopes(exclude = "static")
    public Predicate<Reference> non_serializable() {
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
                ownerType(hasMethod(
                        "writeObject",
                        ObjectOutputStream.class
                ).negate())
        );
    }

    /**
     * Non-thread-safe classes must not be used in a shared environment.
     *
     * @return predicate
     */
    @ProbableError("Not thread-safe class")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> unsafe_class() {
        return and(
                referenceTypeIsNot(
                        ReferenceType.THREAD_LOCAL,
                        ReferenceType.TERMINATED_THREAD_LOCAL,
                        ReferenceType.WAITING_THREAD_LOCAL
                ),
                targetType(is(
                        Format.class,
                        Calendar.class,
                        StringBuilder.class,
                        ThreadGroup.class
                ))
        );
    }

    /**
     * Transient field is not initialized after deserialization.
     *
     * When {@link Serializable} object is deserialized, it's constructor is not
     * called and all transient fields have no value. If transient field is
     * {@literal final} or is initialized only from constructor, this will leave
     * object in incomplete state.
     *
     * @return predicate
     */
    @ProbableError("Transient field is not initialized after deserialization")
    @Scopes(exclude = "static")
    public Predicate<Reference> uninitializaed_transient_field() {
        return and(
                field(
                        isNotStatic(),
                        isTransient(),
                        declaringClass(is(Serializable.class)),
                        modifiedByNonConstructor().negate().or(isFinal())
                ),
                ownerType(hasMethod(
                        "readObject",
                        ObjectInputStream.class
                ).negate())
        );
    }

}
