package org.vaadin.qa.cqt.predicates;

import org.vaadin.qa.cqt.PredicateUtils;
import org.vaadin.qa.cqt.Unreflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Created by Artem Godin on 9/24/2020.
 */
@SuppressWarnings("unchecked")
public interface FieldPredicates {
    default Predicate<Field> getter(Predicate<Method>... rules) {
        return getter(PredicateUtils.and(rules));
    }

    default Predicate<Field> getter(Predicate<Method> rule) {
        return field -> {
            String name = field.getName();
            String getter = (boolean.class.equals(field.getType()) || Boolean.class.equals(field.getType()) ? "is" : "get")
                    + name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
            try {
                return rule.test(Unreflection.getDeclaredMethod(field.getDeclaringClass(),getter));
            } catch (NoSuchMethodException e) {
                return false;
            }
        };
    }

    default Predicate<Field> setter(Predicate<Method>... rules) {
        return setter(PredicateUtils.and(rules));
    }

    default Predicate<Field> setter(Predicate<Method> rule) {
        return field -> {
            String name = field.getName();
            String setter = "set" + name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
            try {
                return rule.test(Unreflection.getDeclaredMethod(field.getDeclaringClass(),setter, field.getType()));
            } catch (NoSuchMethodException e) {
                return false;
            }
        };
    }

    default Predicate<Field> hasSetter() {
        return setter(method -> true);
    }

    default Predicate<Field> hasGetter() {
        return getter(method -> true);
    }

    default Predicate<Field> isEnumConstant() {
        return Field::isEnumConstant;
    }

    default Predicate<Field> isNotEnumConstant() {
        return field -> !field.isEnumConstant();
    }

    default Predicate<Field> type(Predicate<Class<?>>... rules) {
        return type(PredicateUtils.and(rules));
    }

    default Predicate<Field> type(Predicate<Class<?>> rule) {
        return field -> rule.test(field.getType());
    }

    default Predicate<Field> genericType(int index, Predicate<Class<?>>... rules) {
        return genericType(index, PredicateUtils.and(rules));
    }

    default Predicate<Field> genericType(int index, Predicate<Class<?>> rule) {
        return field -> {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) (genericType)).getActualTypeArguments();
                if (actualTypeArguments.length > index) {
                    if (actualTypeArguments[index] instanceof Class<?>) {
                        return rule.test((Class<?>) actualTypeArguments[index]);
                    } else if (actualTypeArguments[index] instanceof ParameterizedType) {
                        return rule.test((Class<?>) ((ParameterizedType) actualTypeArguments[index]).getRawType());
                    }
                }
            }
            return false;
        };
    }

    default Predicate<Field> declaringClass(Predicate<Class<?>>... rules) {
        return declaringClass(PredicateUtils.and(rules));
    }

    default Predicate<Field> declaringClass(Predicate<Class<?>> rule) {
        return field -> rule.test(field.getDeclaringClass());
    }
}
