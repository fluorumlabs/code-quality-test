package org.vaadin.qa.cqt.data;

import org.vaadin.qa.cqt.annotations.Level;

import java.util.function.Predicate;

/**
 * Inspection definition
 */
public class Inspection {

    /**
     * Instantiate a new Inspection.
     *
     * @param level     the severity level (see {@link Level})
     * @param predicate the predicate
     * @param category  the category
     * @param message   the message
     * @param id        the id
     */
    public Inspection(Level level,
                      Predicate<Reference> predicate,
                      String category,
                      String message,
                      String id) {
        this.level     = level;
        this.predicate = predicate;
        this.message   = message;
        this.category  = category;
        this.id        = id;
    }

    /**
     * Get category.
     *
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Get id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Get severity level.
     *
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Get message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get predicate.
     *
     * @return the predicate
     */
    public Predicate<Reference> getPredicate() {
        return predicate;
    }

    private final String category;

    private final String id;

    private final Level level;

    private final String message;

    private final Predicate<Reference> predicate;

}
