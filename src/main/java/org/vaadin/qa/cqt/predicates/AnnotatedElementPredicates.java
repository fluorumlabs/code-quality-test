package org.vaadin.qa.cqt.predicates;

import org.vaadin.qa.cqt.engine.Engine;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.function.Predicate;

/**
 * Created by Artem Godin on 9/24/2020.
 */
public interface AnnotatedElementPredicates {
    default <T extends AnnotatedElement> Predicate<T> isAnnotatedWith(String... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (String aClass : classes) {
                    if (Engine.getClass(aClass).isAssignableFrom(annotation.annotationType())) return true;
                }
            }
            return false;
        };
    }

    default <T extends AnnotatedElement> Predicate<T> isNotAnnotatedWith(String... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (String aClass : classes) {
                    if (Engine.getClass(aClass).isAssignableFrom(annotation.annotationType())) return false;
                }
            }
            return true;
        };
    }

    default <T extends AnnotatedElement> Predicate<T> isAnnotatedWith(Class<? extends Annotation>... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (Class<?> aClass : classes) {
                    if (aClass.isAssignableFrom(annotation.annotationType())) return true;
                }
            }
            return false;
        };
    }

    default <T extends AnnotatedElement> Predicate<T> isNotAnnotatedWith(Class<? extends Annotation>... classes) {
        return element -> {
            for (Annotation annotation : element.getDeclaredAnnotations()) {
                for (Class<?> aClass : classes) {
                    if (aClass.isAssignableFrom(annotation.annotationType())) return false;
                }
            }
            return true;
        };
    }
}
