package org.vaadin.qa.cqt.engine;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.*;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.qa.cqt.ScopeDetector;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * Created by Artem Godin on 9/28/2020.
 */
public class SpringEngine extends DefaultEngine {
    @Override
    public String getRealmFromAnnotations(Class<?> clazz) {
        if (Component.class.isAssignableFrom(clazz)) {
            return "singleton";
        }
        Annotation[] annotations;
        try {
            annotations = clazz.getAnnotations();
        } catch (Exception e) {
            // ignore
            return "";
        }

        boolean isComponent = Arrays.stream(annotations)
                .map(Annotation::annotationType)
                .filter(cz -> !cz.getName().equals(clazz.getName()))
                .map(ScopeDetector::getRealmFromCache)
                .anyMatch("singleton"::equals);
        if (isComponent) {
            // Found @Component
            Scope scope = clazz.getAnnotation(Scope.class);
            if (scope != null) {
                if (!scope.value().isEmpty()) {
                    return scope.value();
                } else if (!scope.scopeName().isEmpty()) {
                    return scope.scopeName();
                }
            }
            return "singleton";
        }
        return super.getRealmFromAnnotations(clazz);
    }

    @Override
    public Object unwrap(Object proxy) {
        while ((AopUtils.isAopProxy(proxy))) {
            try {
                return unwrap(((Advised) proxy).getTargetSource().getTarget());
            } catch (Exception e) {
                return proxy;
            }
        }
        return proxy;
    }

    @Override
    public String getName() {
        return "Spring";
    }
}
