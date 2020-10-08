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

package com.github.fluorumlabs.cqt.predicates;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

/**
 * Predicates for dealing with {@link Member}.
 */
public interface MemberPredicates {

    /**
     * Predicate testing if {@link Member} is abstract.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isAbstract() {
        return member -> Modifier.isAbstract(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is final.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isFinal() {
        return member -> Modifier.isFinal(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is native.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNative() {
        return member -> Modifier.isNative(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is not abstract.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotAbstract() {
        return member -> !Modifier.isAbstract(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is not final.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotFinal() {
        return member -> !Modifier.isFinal(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is not native.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotNative() {
        return member -> !Modifier.isNative(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is not private.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotPrivate() {
        return member -> !Modifier.isPrivate(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is not protected.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotProtected() {
        return member -> !Modifier.isProtected(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is not public.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotPublic() {
        return member -> !Modifier.isPublic(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is not static.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotStatic() {
        return member -> !Modifier.isStatic(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is not strict.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotStrict() {
        return member -> !Modifier.isStrict(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is not synchronized.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotSynchronized() {
        return member -> !Modifier.isSynchronized(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is not synthetic.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotSynthetic() {
        return member -> !member.isSynthetic();
    }

    /**
     * Predicate testing if {@link Member} is not transient.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotTransient() {
        return member -> !Modifier.isTransient(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is not volatile.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isNotVolatile() {
        return member -> !Modifier.isVolatile(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is private.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isPrivate() {
        return member -> Modifier.isPrivate(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is protected.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isProtected() {
        return member -> Modifier.isProtected(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is public.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isPublic() {
        return member -> Modifier.isPublic(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is static.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isStatic() {
        return member -> Modifier.isStatic(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is strict.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isStrict() {
        return member -> Modifier.isStrict(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is synchronized.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isSynchronized() {
        return member -> Modifier.isSynchronized(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is synthetic.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isSynthetic() {
        return Member::isSynthetic;
    }

    /**
     * Predicate testing if {@link Member} is transient.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isTransient() {
        return member -> Modifier.isTransient(member.getModifiers());
    }

    /**
     * Predicate testing if {@link Member} is volatile.
     *
     * @param <T> the type parameter
     *
     * @return the predicate
     */
    default <T extends Member> Predicate<T> isVolatile() {
        return member -> Modifier.isVolatile(member.getModifiers());
    }

}
