package org.vaadin.qa.cqt.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify scopes to which annotated {@link org.vaadin.qa.cqt.Suite} inspection
 * applies.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scopes {

    /**
     * List of object scopes for which inspection should not be performed.
     *
     * @return the list of scopes
     */
    String[] exclude() default {};

    /**
     * List of object scopes for which inspection should be performed.
     *
     * @return the list of scopes
     */
    String[] value() default {};

}
