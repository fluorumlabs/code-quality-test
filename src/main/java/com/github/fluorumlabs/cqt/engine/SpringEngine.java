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

import com.github.fluorumlabs.cqt.internals.ScopeDetector;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.github.fluorumlabs.cqt.engine.spring.ContextListener;

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
                consumer.accept(
                        singleton,
                        "singleton"
                );
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

        boolean isComponent = Arrays
                .stream(annotations)
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
    public String getVersion() {
        return super.getVersion() + "-spring";
    }

}
