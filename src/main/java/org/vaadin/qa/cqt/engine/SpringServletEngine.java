package org.vaadin.qa.cqt.engine;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Created by Artem Godin on 9/28/2020.
 */
public class SpringServletEngine extends SpringEngine {
    @Override
    public String getRealmFromAnnotations(Class<?> clazz) {
        if (Servlet.class.isAssignableFrom(clazz)
                || Filter.class.isAssignableFrom(clazz)
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
        return "Spring+Servlet";
    }
}
