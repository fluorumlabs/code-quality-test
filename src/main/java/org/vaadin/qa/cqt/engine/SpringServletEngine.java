package org.vaadin.qa.cqt.engine;

import javax.servlet.*;
import javax.servlet.http.HttpSession;

/**
 * Created by Artem Godin on 9/28/2020.
 */
public class SpringServletEngine extends SpringEngine {
    @Override
    public String getRealmFromAnnotations(Class<?> clazz) {
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

        return super.getRealmFromAnnotations(clazz);
    }

    @Override
    public String getName() {
        return super.getName()+"-servlet";
    }
}
