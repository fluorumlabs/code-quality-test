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
     * Predicate testing if {@link AnnotatedElement} is annotated with specified annotations
     *
     * @param <T>     the type parameter
     * @param classes the classes
     * @return the predicate
     */
    default <T extends AnnotatedElement> Predicate<T> isAnnotatedWith(String... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (String aClass : classes) {
                    if (Engine.getClass(aClass).isAssignableFrom(annotation.annotationType()))
                        return true;
                }
            }
            return false;
        };
    }

    /**
     * Predicate testing if {@link AnnotatedElement} is annotated with specified annotations
     *
     * @param <T>     the type parameter
     * @param classes the classes
     * @return the predicate
     */
    default <T extends AnnotatedElement> Predicate<T> isAnnotatedWith(Class<? extends Annotation>... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (Class<?> aClass : classes) {
                    if (aClass.isAssignableFrom(annotation.annotationType()))
                        return true;
                }
            }
            return false;
        };
    }

    /**
     * Predicate testing if {@link AnnotatedElement} is not annotated with specified annotations
     *
     * @param <T>     the type parameter
     * @param classes the classes
     * @return the predicate
     */
    default <T extends AnnotatedElement> Predicate<T> isNotAnnotatedWith(String... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (String aClass : classes) {
                    if (Engine.getClass(aClass).isAssignableFrom(annotation.annotationType()))
                        return false;
                }
            }
            return true;
        };
    }

    /**
     * Predicate testing if {@link AnnotatedElement} is not annotated with specified annotations
     *
     * @param <T>     the type parameter
     * @param classes the classes
     * @return the predicate
     */
    default <T extends AnnotatedElement> Predicate<T> isNotAnnotatedWith(Class<? extends Annotation>... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (Class<?> aClass : classes) {
                    if (aClass.isAssignableFrom(annotation.annotationType()))
                        return false;
                }
            }
            return true;
        };
    }
}
