package org.vaadin.qa.cqt.engine;

import org.vaadin.qa.cqt.ScopeDetector;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Artem Godin on 9/28/2020.
 */
public class ServletEngine extends DefaultEngine {
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
        return super.getName()+"-servlet";
    }

    @Override
    public List<String> getContextOrder() {
        return Arrays.asList("request", "session", "singleton", "static");
    }
}
