package org.vaadin.qa.cqt.engine;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.qa.cqt.engine.spring.ContextListener;
import org.vaadin.qa.cqt.internals.ScopeDetector;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * {@link Engine} with Spring support
 */
public class SpringEngine extends DefaultEngine {

    @Override
    public void addSystemObjects(BiConsumer<Object, String> consumer) {
        ApplicationContext context = ContextListener.getContext();
        if (context instanceof ConfigurableApplicationContext) {
            ConfigurableListableBeanFactory clbf = ((ConfigurableApplicationContext) context)
                    .getBeanFactory();
            for (String singletonName : clbf.getSingletonNames()) {
                Object singleton = clbf.getSingleton(singletonName);
                consumer.accept(singleton, "singleton");
            }
        }
    }

    @Override
    public String detectScope(Class<?> clazz) {
        if (Component.class.isAssignableFrom(clazz)
                || ApplicationContext.class.isAssignableFrom(clazz)) {
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
                .map(ScopeDetector::detectScope)
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
        return super.detectScope(clazz);
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
    public List<String> getScopeOrder() {
        return Arrays.asList(
                "request",
                "session",
                "singleton",
                "application",
                "restart",
                "static"
        );
    }

    @Override
    public String getVersion() {
        return super.getVersion() + "-spring";
    }

}
