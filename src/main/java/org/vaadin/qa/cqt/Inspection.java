package org.vaadin.qa.cqt;

import org.vaadin.qa.cqt.annotations.Level;

import java.util.function.Predicate;

/**
 * Created by Artem Godin on 9/24/2020.
 */
public class Inspection {
    private final Level level;
    private final Predicate<Reference> predicate;
    private final String message;
    private final String id;

    private final String category;

    public Inspection(Level level, Predicate<Reference> predicate, String category, String message, String id) {
        this.level = level;
        this.predicate = predicate;
        this.message = message;
        this.category = category;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Level getLevel() {
        return level;
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
