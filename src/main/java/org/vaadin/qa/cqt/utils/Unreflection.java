package org.vaadin.qa.cqt.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection helper.
 */
public final class Unreflection {
    /**
     * Get declared constructors of Class.
     *
     * @param clazz the clazz
     * @return the array of constructors
     */
    public static Constructor<?>[] getDeclaredConstructors(Class<?> clazz) {
        try {
            return (Constructor<?>[]) GET_DECLARED_CONSTRUCTORS.bindTo(clazz).invokeWithArguments();
        } catch (NoClassDefFoundError e) {
            return new Constructor<?>[0];
        } catch (Throwable e) {
            throw new UnsupportedOperationException("Cannot get getDeclaredConstructors of " + clazz.getName(), e);
        }
    }

    /**
     * Gets declared field of class.
     *
     * @param clazz the clazz
     * @param name  the name
     * @return the declared field
     * @throws NoSuchFieldException field does not exist
     */
    public static Field getDeclaredField(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return (Field) GET_DECLARED_FIELD.bindTo(clazz).invokeWithArguments(name);
        } catch (Throwable e) {
            throw new NoSuchFieldException("Cannot get getDeclaredField for " + clazz.getName() + "." + name);
        }
    }

    /**
     * Get declared fields of class.
     *
     * @param clazz the clazz
     * @return the array of fields
     */
    public static Field[] getDeclaredFields(Class<?> clazz) {
        try {
            return (Field[]) GET_DECLARED_FIELDS.bindTo(clazz).invokeWithArguments();
        } catch (NoClassDefFoundError e) {
            return new Field[0];
        } catch (Throwable e) {
            throw new UnsupportedOperationException("Cannot get getDeclaredFields of " + clazz.getName(), e);
        }
    }

    /**
     * Gets declared method of class.
     *
     * @param clazz the clazz
     * @param name  the name
     * @param args  the args
     * @return the declared method
     * @throws NoSuchMethodException method does not exist
     */
    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... args) throws NoSuchMethodException {
        try {
            return (Method) GET_DECLARED_METHOD.bindTo(clazz).invokeWithArguments(name, args);
        } catch (Throwable e) {
            throw new NoSuchMethodException("Cannot get getDeclaredMethod for " + clazz.getName() + "." + name);
        }
    }

    /**
     * Read field of object
     *
     * @param <T>            the type parameter
     * @param instance       the instance
     * @param declaringClass the declaring class
     * @param name           the name
     * @param type           the type
     * @return read value
     */
    public static <T> T readField(Object instance, Class<?> declaringClass, String name, Class<?> type) {
        try {
            return (T) lookupAll().findGetter(declaringClass, name, type).bindTo(instance).invokeWithArguments();
        } catch (Throwable e) {
            throw new UnsupportedOperationException("Cannot read field " + name + " of " + declaringClass.getName(), e);
        }
    }

    /**
     * Read static field of class.
     *
     * @param <T>            the type parameter
     * @param declaringClass the declaring class
     * @param name           the name
     * @param type           the type
     * @return the t
     */
    public static <T> T readStaticField(Class<?> declaringClass, String name, Class<?> type) {
        try {
            return (T) lookupAll().findStaticGetter(declaringClass, name, type).invokeWithArguments();
        } catch (Throwable e) {
            throw new UnsupportedOperationException("Cannot read static field " + name + " of " + declaringClass.getName(), e);
        }
    }
    private static final MethodHandle GET_DECLARED_CONSTRUCTORS;
    private static final MethodHandle GET_DECLARED_FIELD;
    private static final MethodHandle GET_DECLARED_FIELDS;
    private static final MethodHandle GET_DECLARED_METHOD;
    /*
     * This holds MethodHandles.Lookup that can see all public, protected, private and static methods.
     */
    private static final MethodHandles.Lookup LOOKUP_TRUSTED;

    static {
        try {
            // Get the protected static field IMPL_LOOKUP of MethodHandles.Lookup
            Field lookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            lookup.setAccessible(true);
            // Expose a static IMPL_LOOKUP
            LOOKUP_TRUSTED = (MethodHandles.Lookup) lookup.get(null);

            GET_DECLARED_FIELDS = lookupAll().findVirtual(Class.class, "getDeclaredFields", MethodType.methodType(Field[].class));
            GET_DECLARED_CONSTRUCTORS = lookupAll().findVirtual(Class.class, "getDeclaredConstructors", MethodType.methodType(Constructor[].class));
            GET_DECLARED_FIELD = lookupAll().findVirtual(Class.class, "getDeclaredField", MethodType.methodType(Field.class, String.class));
            GET_DECLARED_METHOD = lookupAll().findVirtual(Class.class, "getDeclaredMethod", MethodType.methodType(Method.class, String.class, Class[].class));
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException | NoSuchMethodException e) {
            throw new UnsupportedOperationException("Unable to initialize UnReflection", e);
        }
    }

    /*
     * Accessor for LOOKUP_TRUSTED. This can be used instead of MethodHandles.lookup() / MethodHandles.publicLookup(),
     * if unrestricted access is required
     * <p>
     * Note: all security checks are bypassed.
     */
    private static MethodHandles.Lookup lookupAll() {
        return LOOKUP_TRUSTED;
    }

    private Unreflection() {
    }
}
