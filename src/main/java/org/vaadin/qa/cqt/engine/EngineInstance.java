package org.vaadin.qa.cqt.engine;

/**
 * {@link Engine} instance initializer and holder.
 *
 * Particular instantiated engine is detected based on the environment.
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

    /**
     * Get {@link Engine} instance.
     *
     * @return the {@link Engine} instance
     */
    public static Engine get() {
        return engine;
    }

}
