package org.vaadin.qa.cqt;

import org.vaadin.qa.cqt.engine.Engine;
import org.vaadin.qa.cqt.engine.EngineInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Artem Godin on 9/23/2020.
 */
public final class ScopeDetector {
    private static final Map<Class<?>, String> scopeCache = new ConcurrentHashMap<>();

    private ScopeDetector(){}

    public static void hint(Class<?> clazz, String scope) {
        scopeCache.put(clazz, scope);
    }

    public static Optional<String> detect(Class<?> clazz) {
        String realm = getRealmFromCache(clazz);
        if (realm.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(realm);
        }
    }

    public static String getRealmFromCache(Class<?> clazz) {
        String result = scopeCache.get(clazz);
        if (result == null) {
            if (clazz.isArray() || clazz.isPrimitive() || clazz.getName().startsWith("java.lang.annotation.")) {
                return "";
            }
            result = EngineInstance.get().getRealmFromAnnotations(clazz);
            scopeCache.put(clazz, result);
        }
        return result;
    }

//    private static String getRealmFromAnnotations(Class<?> clazz) {
//        if (Component.class.isAssignableFrom(clazz)) {
//            return "singleton";
//        }
//        if (clazz.isArray() || clazz.isPrimitive() || clazz.getName().startsWith("java.lang.annotation.")) {
//            return "";
//        }
//        if (Servlet.class.isAssignableFrom(clazz)
//                || Filter.class.isAssignableFrom(clazz)
//                || VaadinService.class.isAssignableFrom(clazz)
//                || RequestHandler.class.isAssignableFrom(clazz)
//        ) {
//            return "singleton";
//        }
//        if (HttpSession.class.isAssignableFrom(clazz)) {
//            return "session";
//        }
//        if (VaadinSession.class.isAssignableFrom(clazz)) {
//            return "vaadin-session";
//        }
//        if (UI.class.isAssignableFrom(clazz)) {
//            return "vaadin-ui";
//        }
//        if (ServletRequest.class.isAssignableFrom(clazz)
//                || ServletResponse.class.isAssignableFrom(clazz)
//                || VaadinRequest.class.isAssignableFrom(clazz)
//                || VaadinResponse.class.isAssignableFrom(clazz)) {
//            return "request";
//        }
//
//
//        Annotation[] annotations;
//        try {
//            annotations = clazz.getAnnotations();
//        } catch (Exception e) {
//            // ignore
//            return "";
//        }
//
//        boolean isComponent = Arrays.stream(annotations)
//                .map(Annotation::annotationType)
//                .map(ScopeDetector::getRealmFromCache)
//                .anyMatch("singleton"::equals);
//        if (isComponent) {
//            // Found @Component
//            Scope scope = clazz.getAnnotation(Scope.class);
//            if (scope != null) {
//                if (!scope.value().isEmpty()) {
//                    return scope.value();
//                } else if (!scope.scopeName().isEmpty()) {
//                    return scope.scopeName();
//                }
//            }
//            return "singleton";
//        }
//        return "";
//    }
}
