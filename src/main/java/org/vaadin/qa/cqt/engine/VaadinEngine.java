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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.*;

import java.util.Arrays;
import java.util.List;

/**
 * {@link Engine} with Servlet and Vaadin support
 */
public class VaadinEngine extends ServletEngine {

    @Override
    public String detectScope(Class<?> clazz) {
        if (VaadinService.class.isAssignableFrom(clazz)
            || RequestHandler.class.isAssignableFrom(clazz)
            || VaadinContext.class.isAssignableFrom(clazz)) {
            return "singleton";
        }
        if (VaadinSession.class.isAssignableFrom(clazz)) {
            return "vaadin-session";
        }
        if (UI.class.isAssignableFrom(clazz)) {
            return "vaadin-ui";
        }
        if (VaadinRequest.class.isAssignableFrom(clazz)
            || VaadinResponse.class.isAssignableFrom(clazz)) {
            return "request";
        }

        return super.detectScope(clazz);
    }

    @Override
    public List<String> getScopeOrder() {
        return Arrays.asList(
                "request",
                "vaadin-ui",
                "vaadin-session",
                "session",
                "singleton",
                "static"
        );
    }

    @Override
    public String getVersion() {
        return super.getVersion() + "-vaadin";
    }

}
