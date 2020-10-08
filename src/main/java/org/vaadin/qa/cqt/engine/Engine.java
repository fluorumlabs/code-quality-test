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

package org.vaadin.qa.cqt.engine;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Engine providing various environment-specific features.
 */
public abstract class Engine {

    /**
     * Load the class using the default class loader.
     *
     * @param name the name
     *
     * @return the class or {@code void} if loading failed for any reason
     */
    public static Class<?> getClass(String name) {
        try {
            ClassLoader classLoader = Thread
                    .currentThread()
                    .getContextClassLoader();
            return classLoader.loadClass(name);
        } catch (Throwable e) {
            return void.class;
        }
    }

    /**
     * Add (system) objects to scanner. This method is invoked by {@link
     * org.vaadin.qa.cqt.internals.Scanner} before visiting class loaders and
     * can be used e.g. for adding Spring singletons.
     *
     * @param consumer the consumer function taking object and scope as
     *                 arguments.
     */
    public void addSystemObjects(BiConsumer<Object, String> consumer) {

    }

    /**
     * Detect class scope (type or annotation based).
     *
     * @param clazz the clazz
     *
     * @return the scope
     */
    public abstract String detectScope(Class<?> clazz);

    /**
     * Test if new scope has greater coverage than the current. For example,
     * {@code shouldPropagateScope("session", "static")} will return {@code
     * true} because {@literal static} is more shared then {@literal session}
     * scope.
     * <p>
     * Any scopes not listed in {@link Engine#getScopeOrder()} are considered on
     * the same level as {@literal instance}.
     *
     * @param currentScope the current scope
     * @param newScope     the new scope
     *
     * @return {@code true} if {@code newScope} has greater coverage then {@code
     *         currentScope}
     */
    public boolean shouldPropagateScope(String currentScope, String newScope) {
        List<String> contextOrder = getScopeOrder();
        int          currentPos   = contextOrder.indexOf(currentScope);
        int          newPos       = contextOrder.indexOf(newScope);
        return newPos >= 0 && (currentPos < newPos);
    }

    /**
     * Get scope order, from least shared to static, excluding {@literal
     * instance} scope.
     *
     * @return the scope order
     */
    public abstract List<String> getScopeOrder();

    /**
     * Unwrap CGLIB/AOP proxy object.
     *
     * @param proxy the proxy
     *
     * @return the unwrapped object or argument itself if it's not proxy
     */
    public abstract Object unwrap(Object proxy);

    /**
     * Get Engine version.
     *
     * @return the version
     */
    public abstract String getVersion();

}
