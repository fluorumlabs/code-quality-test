package org.vaadin.qa.cqt.engine;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.*;
import org.graalvm.compiler.lir.CompositeValue;
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
public class VaadinEngine extends ServletEngine {
    @Override
    public String getRealmFromAnnotations(Class<?> clazz) {
        if (VaadinService.class.isAssignableFrom(clazz)
                || RequestHandler.class.isAssignableFrom(clazz)
        ) {
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

        return super.getRealmFromAnnotations(clazz);
    }

    @Override
    public String getName() {
        return super.getName()+"-vaadin";
    }

    @Override
    public List<String> getContextOrder() {
        return Arrays.asList("request", "vaadin-ui", "vaadin-session", "session", "singleton", "static");
    }

}
