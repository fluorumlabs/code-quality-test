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

package org.vaadin.qa.cqt.utils;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Predicate helpers.
 */
public final class PredicateUtils {

    private PredicateUtils() {
    }

    /**
     * Predicate and'ing all arguments.
     *
     * @param <E>        the type parameter
     * @param <T>        the type parameter
     * @param predicates the predicates
     *
     * @return the predicate
     */
    public static <E, T extends Predicate<E>> T and(T... predicates) {
        return Stream
                .of(predicates)
                .reduce((x, y) -> (T) x.and(y))
                .orElseGet(PredicateUtils::alwaysTrue);
    }

    private static <T extends Predicate<?>> T alwaysTrue() {
        Predicate<?> predicate = x -> true;
        return (T) predicate;
    }

    /**
     * Predicate or'ing all arguments.
     *
     * @param <E>        the type parameter
     * @param <T>        the type parameter
     * @param predicates the predicates
     *
     * @return the predicate
     */
    public static <E, T extends Predicate<E>> T or(T... predicates) {
        return Stream
                .of(predicates)
                .reduce((x, y) -> (T) x.or(y))
                .orElseGet(PredicateUtils::alwaysTrue);
    }

}
