package org.vaadin.qa.cqt.internals;

import org.vaadin.qa.cqt.engine.EngineInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper to detect scope of class with a help of {@link org.vaadin.qa.cqt.engine.Engine#detectScope(Class)}
 */
public final class ScopeDetector {

    /**
     * Decect scope for specified class
     *
     * @param clazz the class
     * @return the scope or {@code ""} if scope is unknown
     */
    public static String detectScope(Class<?> clazz) {
        String result = scopeCache.get(clazz);
        if (result == null) {
            if (clazz.isArray() || clazz.isPrimitive() || clazz
                    .getName()
                    .startsWith("java.lang.annotation.")) {
                return "";
            }
            result = EngineInstance.get().detectScope(clazz);
            scopeCache.put(clazz, result);
        }
        return result;
    }

    /**
     * Hint ScopeDetector of scope for specified class.
     *
     * @param clazz the class
     * @param scope the scope
     */
    public static void hint(Class<?> clazz, String scope) {
        scopeCache.put(clazz, scope);
    }

    private static final Map<Class<?>, String> scopeCache
            = new ConcurrentHashMap<>();

    private ScopeDetector() {}

}
