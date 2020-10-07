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
