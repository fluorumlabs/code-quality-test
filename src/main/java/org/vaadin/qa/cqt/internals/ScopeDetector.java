/*
 * Copyright (c) 2020 Artem Godin
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.vaadin.qa.cqt.internals;

import org.vaadin.qa.cqt.engine.EngineInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper to detect scope of class with a help of {@link
 * org.vaadin.qa.cqt.engine.Engine#detectScope(Class)}
 */
public final class ScopeDetector {

    private static final Map<Class<?>, String> scopeCache = new ConcurrentHashMap<>();

    private ScopeDetector() {}

    /**
     * Decect scope for specified class
     *
     * @param clazz the class
     *
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
            scopeCache.put(
                    clazz,
                    result
            );
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
        scopeCache.put(
                clazz,
                scope
        );
    }

}
