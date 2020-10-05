package org.vaadin.qa.cqt.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Artem Godin on 9/24/2020.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Report(name = "Warning", level = Level.WARNING)
public @interface Warning {
    String value();
}
