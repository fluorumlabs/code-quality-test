package org.vaadin.qa.cqt.predicates;

import org.vaadin.qa.cqt.engine.Engine;

import java.lang.reflect.Modifier;
import java.util.function.Predicate;

/**
 * Created by Artem Godin on 9/24/2020.
 */
public interface TypePredicates {
    default Predicate<Class<?>> isArray() {
        return Class::isArray;
    }

    default Predicate<Class<?>> isNotArray() {
        return clazz -> !clazz.isArray();
    }

    default Predicate<Class<?>> isAnnotation() {
        return Class::isAnnotation;
    }

    default Predicate<Class<?>> isNotAnnotation() {
        return clazz -> !clazz.isAnnotation();
    }

    default Predicate<Class<?>> isSyntheticClass() {
        return Class::isSynthetic;
    }

    default Predicate<Class<?>> isNotSyntheticClass() {
        return clazz -> !clazz.isSynthetic();
    }

    default Predicate<Class<?>> isPrimitive() {
        return Class::isPrimitive;
    }

    default Predicate<Class<?>> isNotPrimitive() {
        return clazz -> !clazz.isPrimitive();
    }

    default Predicate<Class<?>> isEnum() {
        return Class::isEnum;
    }

    default Predicate<Class<?>> isNotEnum() {
        return clazz -> !clazz.isEnum();
    }

    default Predicate<Class<?>> isAnonymousClass() {
        return Class::isAnonymousClass;
    }

    default Predicate<Class<?>> isNotAnonymousClass() {
        return clazz -> !clazz.isAnonymousClass();
    }

    default Predicate<Class<?>> isInterface() {
        return Class::isInterface;
    }

    default Predicate<Class<?>> isNotInterface() {
        return clazz -> !clazz.isInterface();
    }

    default Predicate<Class<?>> isMemberClass() {
        return Class::isMemberClass;
    }

    default Predicate<Class<?>> isNotMemberClass() {
        return clazz -> !clazz.isMemberClass();
    }

    default Predicate<Class<?>> isLocalClass() {
        return Class::isLocalClass;
    }

    default Predicate<Class<?>> isNotLocalClass() {
        return clazz -> !clazz.isLocalClass();
    }

    default Predicate<Class<?>> isPublicClass() {
        return clazz -> Modifier.isPublic(clazz.getModifiers());
    }

    default Predicate<Class<?>> isNotPublicClass() {
        return clazz -> !Modifier.isPublic(clazz.getModifiers());
    }

    default Predicate<Class<?>> isProtectedClass() {
        return clazz -> Modifier.isProtected(clazz.getModifiers());
    }

    default Predicate<Class<?>> isNotProtectedClass() {
        return clazz -> !Modifier.isProtected(clazz.getModifiers());
    }

    default Predicate<Class<?>> isPrivateClass() {
        return clazz -> Modifier.isPrivate(clazz.getModifiers());
    }

    default Predicate<Class<?>> isNotPrivateClass() {
        return clazz -> !Modifier.isPrivate(clazz.getModifiers());
    }

    default Predicate<Class<?>> isStaticClass() {
        return clazz -> Modifier.isStatic(clazz.getModifiers());
    }

    default Predicate<Class<?>> isNotStaticClass() {
        return clazz -> !Modifier.isStatic(clazz.getModifiers());
    }

    default Predicate<Class<?>> isFinalClass() {
        return clazz -> Modifier.isFinal(clazz.getModifiers());
    }

    default Predicate<Class<?>> isNotFinalClass() {
        return clazz -> !Modifier.isFinal(clazz.getModifiers());
    }

    default Predicate<Class<?>> isAbstractClass() {
        return clazz -> Modifier.isAbstract(clazz.getModifiers());
    }

    default Predicate<Class<?>> isNotAbstractClass() {
        return clazz -> !Modifier.isAbstract(clazz.getModifiers());
    }

    default Predicate<Class<?>> isStrictClass() {
        return clazz -> Modifier.isStrict(clazz.getModifiers());
    }

    default Predicate<Class<?>> isNotStrictClass() {
        return clazz -> !Modifier.isStrict(clazz.getModifiers());
    }

    default Predicate<Class<?>> isLambda() {
        return clazz -> clazz.getSimpleName().contains("$Lambda");
    }

    default Predicate<Class<?>> isNotLambda() {
        return clazz -> !clazz.getSimpleName().contains("$Lambda");
    }

    default Predicate<Class<?>> is(String clazz) {
        return type -> Engine.getClass(clazz).isAssignableFrom(type);
    }

    default Predicate<Class<?>> isNot(String clazz) {
        return type -> !Engine.getClass(clazz).isAssignableFrom(type);
    }

    default Predicate<Class<?>> isExactly(String clazz) {
        return type -> Engine.getClass(clazz).getName().equals(type.getName());
    }

    default Predicate<Class<?>> isNotExactly(String clazz) {
        return type -> !Engine.getClass(clazz).getName().equals(type.getName());
    }

    default Predicate<Class<?>> is(String... classes) {
        return type -> {
            for (String aClass : classes) {
                if (Engine.getClass(aClass).isAssignableFrom(type)) return true;
            }
            return false;
        };
    }

    default Predicate<Class<?>> isNot(String... classes) {
        return type -> {
            for (String aClass : classes) {
                if (Engine.getClass(aClass).isAssignableFrom(type)) return false;
            }
            return true;
        };
    }

    default Predicate<Class<?>> isExactly(String... classes) {
        return type -> {
            for (String aClass : classes) {
                if (Engine.getClass(aClass).getName().equals(type.getName())) return true;
            }
            return false;
        };
    }

    default Predicate<Class<?>> isNotExactly(String... classes) {
        return type -> {
            for (String aClass : classes) {
                if (Engine.getClass(aClass).getName().equals(type.getName())) return false;
            }
            return true;
        };
    }

    default Predicate<Class<?>> is(Class<?> clazz) {
        return clazz::isAssignableFrom;
    }

    default Predicate<Class<?>> isNot(Class<?> clazz) {
        return type -> !clazz.isAssignableFrom(type);
    }

    default Predicate<Class<?>> isExactly(Class<?> clazz) {
        return type -> clazz.getName().equals(type.getName());
    }

    default Predicate<Class<?>> isNotExactly(Class<?> clazz) {
        return type -> !clazz.getName().equals(type.getName());
    }

    default Predicate<Class<?>> is(Class<?>... classes) {
        return type -> {
            for (Class<?> aClass : classes) {
                if (aClass.isAssignableFrom(type)) return true;
            }
            return false;
        };
    }

    default Predicate<Class<?>> isNot(Class<?>... classes) {
        return type -> {
            for (Class<?> aClass : classes) {
                if (aClass.isAssignableFrom(type)) return false;
            }
            return true;
        };
    }

    default Predicate<Class<?>> isExactly(Class<?>... classes) {
        return type -> {
            for (Class<?> aClass : classes) {
                if (aClass.getName().equals(type.getName())) return true;
            }
            return false;
        };
    }

    default Predicate<Class<?>> isNotExactly(Class<?>... classes) {
        return type -> {
            for (Class<?> aClass : classes) {
                if (aClass.getName().equals(type.getName())) return false;
            }
            return true;
        };
    }

}
