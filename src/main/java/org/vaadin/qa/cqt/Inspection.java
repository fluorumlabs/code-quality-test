package org.vaadin.qa.cqt;

import java.util.function.Predicate;

/**
 * Created by Artem Godin on 9/24/2020.
 */
public class Inspection {
    private final Predicate<Reference> predicate;
    private final String message;

    private final String category;

    public Inspection(Predicate<Reference> predicate, String category, String message) {
        this.predicate = predicate;
        this.message = message;
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public String getCategory() {
        return category;
    }

    public Predicate<Reference> getPredicate() {
        return predicate;
    }
}
