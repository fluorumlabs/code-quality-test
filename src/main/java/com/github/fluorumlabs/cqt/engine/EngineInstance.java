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

package com.github.fluorumlabs.cqt.engine;

/**
 * {@link Engine} instance initializer and holder.
 * <p>
 * Particular instantiated engine is detected based on the environment.
 */
public final class EngineInstance {

    private static final Engine engine;

    static {
        boolean hasServlet = !void.class
                .getName()
                .equals(Engine.getClass("javax.servlet.Servlet").getName());
        boolean hasVaadin = !void.class
                .getName()
                .equals(Engine
                                .getClass("com.vaadin.flow.server.VaadinService")
                                .getName());
        boolean hasSpring = !void.class
                .getName()
                .equals(Engine
                                .getClass("org.springframework.stereotype.Component")
                                .getName());

        if (hasSpring) {
            if (hasVaadin) {
                engine = new SpringVaadinEngine();
            } else if (hasServlet) {
                engine = new SpringServletEngine();
            } else {
                engine = new SpringEngine();
            }
        } else {
            if (hasVaadin) {
                engine = new VaadinEngine();
            } else if (hasServlet) {
                engine = new ServletEngine();
            } else {
                engine = new DefaultEngine();
            }
        }
    }

    private EngineInstance() {}

    /**
     * Get {@link Engine} instance.
     *
     * @return the {@link Engine} instance
     */
    public static Engine get() {
        return engine;
    }

}
