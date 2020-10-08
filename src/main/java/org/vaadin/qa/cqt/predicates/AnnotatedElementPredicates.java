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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.function.Predicate;

/**
 * Predicates for dealing with {@link AnnotatedElement}.
 */
public interface AnnotatedElementPredicates {

    /**
     * Predicate testing if {@link AnnotatedElement} is annotated with specified
     * annotations
     *
     * @param <T>     the type parameter
     * @param classes the classes
     *
     * @return the predicate
     */
    default <T extends AnnotatedElement> Predicate<T> isAnnotatedWith(String... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (String aClass : classes) {
                    if (Engine
                            .getClass(aClass)
                            .isAssignableFrom(annotation.annotationType())) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

    /**
     * Predicate testing if {@link AnnotatedElement} is annotated with specified
     * annotations
     *
     * @param <T>     the type parameter
     * @param classes the classes
     *
     * @return the predicate
     */
    default <T extends AnnotatedElement> Predicate<T> isAnnotatedWith(Class<? extends Annotation>... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (Class<?> aClass : classes) {
                    if (aClass.isAssignableFrom(annotation.annotationType())) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

    /**
     * Predicate testing if {@link AnnotatedElement} is not annotated with
     * specified annotations
     *
     * @param <T>     the type parameter
     * @param classes the classes
     *
     * @return the predicate
     */
    default <T extends AnnotatedElement> Predicate<T> isNotAnnotatedWith(String... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (String aClass : classes) {
                    if (Engine
                            .getClass(aClass)
                            .isAssignableFrom(annotation.annotationType())) {
                        return false;
                    }
                }
            }
            return true;
        };
    }

    /**
     * Predicate testing if {@link AnnotatedElement} is not annotated with
     * specified annotations
     *
     * @param <T>     the type parameter
     * @param classes the classes
     *
     * @return the predicate
     */
    default <T extends AnnotatedElement> Predicate<T> isNotAnnotatedWith(Class<? extends Annotation>... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (Class<?> aClass : classes) {
                    if (aClass.isAssignableFrom(annotation.annotationType())) {
                        return false;
                    }
                }
            }
            return true;
        };
    }

}
