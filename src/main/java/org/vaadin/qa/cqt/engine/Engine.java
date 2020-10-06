package org.vaadin.qa.cqt.engine;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Created by Artem Godin on 9/28/2020.
 */
public abstract class Engine {
    public static Class<?> getClass(String name) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            return void.class;
        }
    }

    public abstract String getRealmFromAnnotations(Class<?> clazz);

    public abstract Object unwrap(Object proxy);

    public abstract String getName();

    public abstract List<String> getContextOrder();

    public void enqueObjects(BiConsumer<Object, String> consumer) {

    }

    public boolean shouldPropagateContext(String currentContext, String newContext) {
        List<String> contextOrder = getContextOrder();
        int currentPos = contextOrder.indexOf(currentContext);
        int newPos = contextOrder.indexOf(newContext);
        return newPos >= 0 && (currentPos < newPos);
    }
}
