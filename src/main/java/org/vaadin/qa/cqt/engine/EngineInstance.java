package org.vaadin.qa.cqt.engine;

/**
 * Created by Artem Godin on 9/28/2020.
 */
public final class EngineInstance {
    private EngineInstance() {}

    private static final Engine engine;

    static {
        boolean hasServlet = !void.class.getName().equals(Engine.getClass("javax.servlet.Servlet").getName());
        boolean hasVaadin = !void.class.getName().equals(Engine.getClass("com.vaadin.flow.server.VaadinService").getName());
        boolean hasSpring = !void.class.getName().equals(Engine.getClass("org.springframework.stereotype.Component").getName());

        if (hasSpring) {
            if (hasVaadin) {
                engine = new SpringVaadinEngine();
            } else if (hasServlet) {
                engine = new SpringServletEngine();
            } else {
                engine = new SpringEngine();
            }
        } else {
            if (hasVaadin) {
                engine = new VaadinEngine();
            } else if (hasServlet) {
                engine = new ServletEngine();
            } else {
                engine = new DefaultEngine();
            }
        }
    }

    public static Engine get() {
        return engine;
    }

}
