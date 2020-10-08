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

package org.vaadin.qa.cqt.predicates;

import org.vaadin.qa.cqt.engine.Engine;

import java.lang.reflect.Modifier;
import java.util.function.Predicate;

/**
 * Predicates for dealing with {@link Class}.
 */
public interface TypePredicates {

    /**
     * Predicate testing if {@link Class} is assignable to specified class.
     *
     * @param clazz the clazz
     *
     * @return the predicate
     */
    default Predicate<Class<?>> is(String clazz) {
        return type -> Engine.getClass(clazz).isAssignableFrom(type);
    }

    /**
     * Predicate testing if {@link Class} is assignable to one of specified
     * classes.
     *
     * @param classes the classes
     *
     * @return the predicate
     */
    default Predicate<Class<?>> is(String... classes) {
        return type -> {
            for (String aClass : classes) {
                if (Engine.getClass(aClass).isAssignableFrom(type)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Predicate testing if {@link Class} is assignable to specified class.
     *
     * @param clazz the clazz
     *
     * @return the predicate
     */
    default Predicate<Class<?>> is(Class<?> clazz) {
        return clazz::isAssignableFrom;
    }

    /**
     * Predicate testing if {@link Class} is assignable to one of specified
     * classes.
     *
     * @param classes the classes
     *
     * @return the predicate
     */
    default Predicate<Class<?>> is(Class<?>... classes) {
        return type -> {
            for (Class<?> aClass : classes) {
                if (aClass.isAssignableFrom(type)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Predicate testing if {@link Class} is abstract.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isAbstractClass() {
        return clazz -> Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is annotation.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isAnnotation() {
        return Class::isAnnotation;
    }

    /**
     * Predicate testing if {@link Class} is anonymous.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isAnonymousClass() {
        return Class::isAnonymousClass;
    }

    /**
     * Predicate testing if {@link Class} is array.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isArray() {
        return Class::isArray;
    }

    /**
     * Predicate testing if {@link Class} is enum.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isEnum() {
        return Class::isEnum;
    }

    /**
     * Predicate testing if {@link Class} is exactly specified class.
     *
     * @param clazz the clazz
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isExactly(String clazz) {
        return type -> Engine.getClass(clazz).getName().equals(type.getName());
    }

    /**
     * Predicate testing if {@link Class} is exactly one of specified classes.
     *
     * @param classes the classes
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isExactly(String... classes) {
        return type -> {
            for (String aClass : classes) {
                if (Engine.getClass(aClass).getName().equals(type.getName())) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Predicate testing if {@link Class} is exactly specified class.
     *
     * @param clazz the clazz
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isExactly(Class<?> clazz) {
        return type -> clazz.getName().equals(type.getName());
    }

    /**
     * Predicate testing if {@link Class} is exactly one of specified classes.
     *
     * @param classes the classes
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isExactly(Class<?>... classes) {
        return type -> {
            for (Class<?> aClass : classes) {
                if (aClass.getName().equals(type.getName())) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Predicate testing if {@link Class} is final.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isFinalClass() {
        return clazz -> Modifier.isFinal(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is inner class.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isInnerClass() {
        return Class::isMemberClass;
    }

    /**
     * Predicate testing if {@link Class} is interface.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isInterface() {
        return Class::isInterface;
    }

    /**
     * Predicate testing if {@link Class} is lambda.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isLambda() {
        return clazz -> clazz.getSimpleName().contains("$Lambda");
    }

    /**
     * Predicate testing if {@link Class} is local class.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isLocalClass() {
        return Class::isLocalClass;
    }

    /**
     * Predicate testing if {@link Class} is not assignable to specified class.
     *
     * @param clazz the clazz
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNot(String clazz) {
        return type -> !Engine.getClass(clazz).isAssignableFrom(type);
    }

    /**
     * Predicate testing if {@link Class} is not assignable to one of specified
     * classes.
     *
     * @param classes the classes
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNot(String... classes) {
        return type -> {
            for (String aClass : classes) {
                if (Engine.getClass(aClass).isAssignableFrom(type)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Predicate testing if {@link Class} is not assignable to specified class.
     *
     * @param clazz the clazz
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNot(Class<?> clazz) {
        return type -> !clazz.isAssignableFrom(type);
    }

    /**
     * Predicate testing if {@link Class} is not assignable to one of specified
     * classes.
     *
     * @param classes the classes
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNot(Class<?>... classes) {
        return type -> {
            for (Class<?> aClass : classes) {
                if (aClass.isAssignableFrom(type)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Predicate testing if {@link Class} is not abstract.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotAbstractClass() {
        return clazz -> !Modifier.isAbstract(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is not annotation.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotAnnotation() {
        return clazz -> !clazz.isAnnotation();
    }

    /**
     * Predicate testing if {@link Class} is not anonymous.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotAnonymousClass() {
        return clazz -> !clazz.isAnonymousClass();
    }

    /**
     * Predicate testing if {@link Class} is not array.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotArray() {
        return clazz -> !clazz.isArray();
    }

    /**
     * Predicate testing if {@link Class} is not enum.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotEnum() {
        return clazz -> !clazz.isEnum();
    }

    /**
     * Predicate testing if {@link Class} is exactly not specified class.
     *
     * @param clazz the clazz
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotExactly(String clazz) {
        return type -> !Engine.getClass(clazz).getName().equals(type.getName());
    }

    /**
     * Predicate testing if {@link Class} is exactly not one of specified
     * classes.
     *
     * @param classes the classes
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotExactly(String... classes) {
        return type -> {
            for (String aClass : classes) {
                if (Engine.getClass(aClass).getName().equals(type.getName())) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Predicate testing if {@link Class} is exactly not specified class.
     *
     * @param clazz the clazz
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotExactly(Class<?> clazz) {
        return type -> !clazz.getName().equals(type.getName());
    }

    /**
     * Predicate testing if {@link Class} is exactly not one of specified
     * classes.
     *
     * @param classes the classes
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotExactly(Class<?>... classes) {
        return type -> {
            for (Class<?> aClass : classes) {
                if (aClass.getName().equals(type.getName())) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Predicate testing if {@link Class} is not final.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotFinalClass() {
        return clazz -> !Modifier.isFinal(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is not inner class.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotInnerClass() {
        return clazz -> !clazz.isMemberClass();
    }

    /**
     * Predicate testing if {@link Class} is not interface.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotInterface() {
        return clazz -> !clazz.isInterface();
    }

    /**
     * Predicate testing if {@link Class} is not lambda.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotLambda() {
        return clazz -> !clazz.getSimpleName().contains("$Lambda");
    }

    /**
     * Predicate testing if {@link Class} is not local class.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotLocalClass() {
        return clazz -> !clazz.isLocalClass();
    }

    /**
     * Predicate testing if {@link Class} is not primitive.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotPrimitive() {
        return clazz -> !clazz.isPrimitive();
    }

    /**
     * Predicate testing if {@link Class} is not private.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotPrivateClass() {
        return clazz -> !Modifier.isPrivate(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is not protected.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotProtectedClass() {
        return clazz -> !Modifier.isProtected(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is not public.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotPublicClass() {
        return clazz -> !Modifier.isPublic(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is not static.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotStaticClass() {
        return clazz -> !Modifier.isStatic(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is not strict.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotStrictClass() {
        return clazz -> !Modifier.isStrict(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is not synthetic.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isNotSyntheticClass() {
        return clazz -> !clazz.isSynthetic();
    }

    /**
     * Predicate testing if {@link Class} is primitive.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isPrimitive() {
        return Class::isPrimitive;
    }

    /**
     * Predicate testing if {@link Class} is private.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isPrivateClass() {
        return clazz -> Modifier.isPrivate(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is protected.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isProtectedClass() {
        return clazz -> Modifier.isProtected(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is public.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isPublicClass() {
        return clazz -> Modifier.isPublic(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is static.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isStaticClass() {
        return clazz -> Modifier.isStatic(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is strict.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isStrictClass() {
        return clazz -> Modifier.isStrict(clazz.getModifiers());
    }

    /**
     * Predicate testing if {@link Class} is synthetic.
     *
     * @return the predicate
     */
    default Predicate<Class<?>> isSyntheticClass() {
        return Class::isSynthetic;
    }

}
