package org.vaadin.qa.cqt.engine;

import org.vaadin.qa.cqt.ScopeDetector;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * Created by Artem Godin on 9/28/2020.
 */
public class DefaultEngine extends Engine {
    @Override
    public String getRealmFromAnnotations(Class<?> clazz) {
        return "";
    }

    @Override
    public Object unwrap(Object proxy) {
        return proxy;
    }

    @Override
    public String getName() {
        return "Default";
    }
}
