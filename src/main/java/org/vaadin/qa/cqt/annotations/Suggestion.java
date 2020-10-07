package org.vaadin.qa.cqt.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When annotated {@link org.vaadin.qa.cqt.Suite} inspection predicate evaluates
 * to {@code true}, the produced report will have {@literal "Suggestion"}
 * category
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Report(name = "Suggestion", level = Level.INFO)
public @interface Suggestion {

    /**
     * Specifies report message.
     *
     * @return the message
     */
    String value();

}
