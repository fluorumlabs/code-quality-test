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
     * @return the predicate
     */
    public static <E, T extends Predicate<E>> T and(T... predicates) {
        return Stream.of(predicates)
                .reduce((x, y) -> (T) x.and(y))
                .orElseGet(PredicateUtils::alwaysTrue);
    }

    /**
     * Predicate or'ing all arguments.
     *
     * @param <E>        the type parameter
     * @param <T>        the type parameter
     * @param predicates the predicates
     * @return the predicate
     */
    public static <E, T extends Predicate<E>> T or(T... predicates) {
        return Stream.of(predicates)
                .reduce((x, y) -> (T) x.or(y))
                .orElseGet(PredicateUtils::alwaysTrue);
    }

    private static <T extends Predicate<?>> T alwaysTrue() {
        Predicate<?> predicate = x -> true;
        return (T) predicate;
    }

}
