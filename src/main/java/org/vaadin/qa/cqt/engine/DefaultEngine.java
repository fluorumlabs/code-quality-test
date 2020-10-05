package org.vaadin.qa.cqt.engine;

import org.vaadin.qa.cqt.ScopeDetector;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Artem Godin on 9/28/2020.
 */
public class DefaultEngine extends Engine {
    private static final String VERSION = "1.0.0";

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
        return VERSION;
    }

    @Override
    public List<String> getContextOrder() {
        return Collections.singletonList("static");
    }
}
