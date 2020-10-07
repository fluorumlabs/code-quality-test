package org.vaadin.qa.cqt.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark annotation to be usable for {@link org.vaadin.qa.cqt.Suite} inspections.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Report {

    /**
     * Report severity level (for example {@link Level#WARNING})
     *
     * @return severity level
     */
    Level level();

    /**
     * Report category (for example {@literal "Warning"})
     *
     * @return report category
     */
    String name();

}
