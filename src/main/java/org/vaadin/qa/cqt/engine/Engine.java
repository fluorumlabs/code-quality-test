package org.vaadin.qa.cqt.engine;

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
}
