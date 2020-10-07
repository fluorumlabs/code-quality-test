package org.vaadin.qa.cqt.engine;

import java.util.Collections;
import java.util.List;

/**
 * Created by Artem Godin on 9/28/2020.
 */
public class DefaultEngine extends Engine {
    private static final String VERSION = "1.0.0";

    @Override
    public String detectScope(Class<?> clazz) {
        return "";
    }

    @Override
    public Object unwrap(Object proxy) {
        return proxy;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public List<String> getScopeOrder() {
        return Collections.singletonList("static");
    }
}
