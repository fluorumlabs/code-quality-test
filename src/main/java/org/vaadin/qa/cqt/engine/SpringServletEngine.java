package org.vaadin.qa.cqt.engine;

import javax.servlet.*;
import javax.servlet.http.HttpSession;

/**
 * {@link Engine} with Spring and Servlet support
 */
public class SpringServletEngine extends SpringEngine {
    @Override
    public String detectScope(Class<?> clazz) {
        if (Servlet.class.isAssignableFrom(clazz)
                || Filter.class.isAssignableFrom(clazz)
                || ServletContext.class.isAssignableFrom(clazz)
                || ServletContextListener.class.isAssignableFrom(clazz)
                || ServletContainerInitializer.class.isAssignableFrom(clazz)
        ) {
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
    public String getVersion() {
        return super.getVersion()+"-servlet";
    }
}
