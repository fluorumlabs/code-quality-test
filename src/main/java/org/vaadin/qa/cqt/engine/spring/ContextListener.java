package org.vaadin.qa.cqt.engine.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

/**
 * The type Context listener to intercept Spring {@code ApplicationContext} instance.
 */
public class ContextListener
        implements ApplicationListener<ApplicationContextEvent> {

    /**
     * Gets Spring {@code ApplicationContext}.
     *
     * @return the context
     */
    public static ApplicationContext getContext() {
        return context;
    }

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
        context = event.getApplicationContext();
    }

    private static ApplicationContext context;

}
