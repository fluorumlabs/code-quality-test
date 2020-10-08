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

import org.vaadin.qa.cqt.utils.PredicateUtils;
import org.vaadin.qa.cqt.utils.Unreflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Predicates for dealing with {@link Field}.
 */
@SuppressWarnings("unchecked")
public interface FieldPredicates {

    /**
     * Predicate testing if declaring class of {@link Field} is conforms to
     * rules.
     *
     * @param rules the rules
     *
     * @return the predicate
     */
    default Predicate<Field> declaringClass(Predicate<Class<?>>... rules) {
        return declaringClass(PredicateUtils.and(rules));
    }

    /**
     * Predicate testing if declaring class of {@link Field} is conforms to
     * rules.
     *
     * @param rule the rule
     *
     * @return the predicate
     */
    default Predicate<Field> declaringClass(Predicate<Class<?>> rule) {
        return field -> rule.test(field.getDeclaringClass());
    }

    /**
     * Predicate testing if generic type argument of {@link Field} is conforms
     * to rules.
     *
     * @param index the index
     * @param rules the rules
     *
     * @return the predicate
     */
    default Predicate<Field> genericType(int index,
                                         Predicate<Class<?>>... rules) {
        return genericType(
                index,
                PredicateUtils.and(rules)
        );
    }

    /**
     * Predicate testing if generic type argument of {@link Field} is conforms
     * to rules.
     *
     * @param index the index
     * @param rule  the rule
     *
     * @return the predicate
     */
    default Predicate<Field> genericType(int index, Predicate<Class<?>> rule) {
        return field -> {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) (genericType))
                        .getActualTypeArguments();
                if (actualTypeArguments.length > index) {
                    if (actualTypeArguments[index] instanceof Class<?>) {
                        return rule.test((Class<?>) actualTypeArguments[index]);
                    } else if (actualTypeArguments[index] instanceof ParameterizedType) {
                        return rule.test((Class<?>) ((ParameterizedType) actualTypeArguments[index])
                                .getRawType());
                    }
                }
            }
            return false;
        };
    }

    /**
     * Predicate testing if getter for {@link Field} is conforms to rules.
     *
     * @param rules the rules
     *
     * @return the
     */
    default Predicate<Field> getter(Predicate<Method>... rules) {
        return getter(PredicateUtils.and(rules));
    }

    /**
     * Predicate testing if getter for {@link Field} is conforms to rules.
     *
     * @param rule the rule
     *
     * @return the
     */
    default Predicate<Field> getter(Predicate<Method> rule) {
        return field -> {
            String name = field.getName();
            String getter = (boolean.class.equals(field.getType())
                                     || Boolean.class.equals(field.getType())
                             ? "is"
                             : "get") + name.substring(
                    0,
                    1
            ).toUpperCase(Locale.ENGLISH) + name.substring(1);
            try {
                return rule.test(Unreflection.getDeclaredMethod(
                        field.getDeclaringClass(),
                        getter
                ));
            } catch (NoSuchMethodException e) {
                return false;
            }
        };
    }

    /**
     * Predicate testing if getter for {@link Field} exists.
     *
     * @return the predicate
     */
    default Predicate<Field> hasGetter() {
        return getter(method -> true);
    }

    /**
     * Predicate testing if setter for {@link Field} exists.
     *
     * @return the predicate
     */
    default Predicate<Field> hasSetter() {
        return setter(method -> true);
    }

    /**
     * Predicate testing if setter for {@link Field} is conforms to rules.
     *
     * @param rule the rule
     *
     * @return the predicate
     */
    default Predicate<Field> setter(Predicate<Method> rule) {
        return field -> {
            String name = field.getName();
            String setter = "set" + name.substring(
                    0,
                    1
            ).toUpperCase(Locale.ENGLISH) + name.substring(1);
            try {
                return rule.test(Unreflection.getDeclaredMethod(
                        field.getDeclaringClass(),
                        setter,
                        field.getType()
                ));
            } catch (NoSuchMethodException e) {
                return false;
            }
        };
    }

    /**
     * Predicate testing if {@link Field} is enum constant.
     *
     * @return the predicate
     */
    default Predicate<Field> isEnumConstant() {
        return Field::isEnumConstant;
    }

    /**
     * Predicate testing if {@link Field} is not enum constant.
     *
     * @return the predicate
     */
    default Predicate<Field> isNotEnumConstant() {
        return field -> !field.isEnumConstant();
    }

    /**
     * Predicate testing if setter for {@link Field} is conforms to rules.
     *
     * @param rules the rules
     *
     * @return the predicate
     */
    default Predicate<Field> setter(Predicate<Method>... rules) {
        return setter(PredicateUtils.and(rules));
    }

    /**
     * Predicate testing if type for {@link Field} is conforms to rules.
     *
     * @param rules the rules
     *
     * @return the predicate
     */
    default Predicate<Field> type(Predicate<Class<?>>... rules) {
        return type(PredicateUtils.and(rules));
    }

    /**
     * Predicate testing if type for {@link Field} is conforms to rules.
     *
     * @param rule the rule
     *
     * @return the predicate
     */
    default Predicate<Field> type(Predicate<Class<?>> rule) {
        return field -> rule.test(field.getType());
    }

}
