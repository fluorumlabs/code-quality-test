package org.vaadin.qa.cqt.engine.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

/**
 * Created by Artem Godin on 10/6/2020.
 */
public class ContextListener implements ApplicationListener<ApplicationContextEvent> {
    private static ApplicationContext context;

    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @Override
    public void onApplicationEvent(ApplicationContextEvent event) {
        context = event.getApplicationContext();
    }

    public static ApplicationContext getContext() {
        return context;
    }
}
