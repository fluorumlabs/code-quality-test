package org.vaadin.qa.cqt;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by Artem Godin on 9/24/2020.
 */
public final class PredicateUtils {
    private PredicateUtils(){}

    public static <T extends Predicate<?>> T alwaysTrue() {
        Predicate<?> predicate = x -> true;
        return (T)predicate;
    }

    public static <E, T extends Predicate<E>> T and(T... predicates) {
        return Stream.of(predicates).reduce((x, y) -> (T)x.and(y)).orElseGet(PredicateUtils::alwaysTrue);
    }

    public static <E, T extends Predicate<E>> T or(T... predicates) {
        return Stream.of(predicates).reduce((x, y) -> (T)x.or(y)).orElseGet(PredicateUtils::alwaysTrue);
    }

}
