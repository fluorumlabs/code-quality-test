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

import javax.servlet.*;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;

/**
 * {@link Engine} with Servlet support
 */
public class ServletEngine extends DefaultEngine {

    @Override
    public String detectScope(Class<?> clazz) {
        if (Servlet.class.isAssignableFrom(clazz)
            || Filter.class.isAssignableFrom(clazz)
            || ServletContext.class.isAssignableFrom(clazz)
            || ServletContextListener.class.isAssignableFrom(clazz)
            || ServletContainerInitializer.class.isAssignableFrom(clazz)) {
            return "singleton";
        }
        if (HttpSession.class.isAssignableFrom(clazz)) {
            return "session";
        }
        if (ServletRequest.class.isAssignableFrom(clazz)
            || ServletResponse.class.isAssignableFrom(clazz)) {
            return "request";
        }

        return super.detectScope(clazz);
    }

    @Override
    public List<String> getScopeOrder() {
        return Arrays.asList(
                "request",
                "session",
                "singleton",
                "static"
        );
    }

    @Override
    public String getVersion() {
        return super.getVersion() + "-servlet";
    }

}
