package org.vaadin.qa.cqt;

import org.apache.commons.text.StringEscapeUtils;
import org.vaadin.qa.cqt.engine.EngineInstance;
import org.vaadin.qa.cqt.suites.Classes;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Created by Artem Godin on 9/23/2020.
 */
public class Scanner {
    private static final long DISPLAY_INTERVAL = TimeUnit.MILLISECONDS.toNanos(100);

    private final Queue<Object> scannerQueue = new ArrayDeque<>();
    private final Map<Object, ObjectData> visitedObjects = new IdentityHashMap<>();
    private final Map<Object, List<Reference>> backreferences = new IdentityHashMap<>();
    private final Predicate<Class<?>> filter;
    private final List<Inspection> inspections = new ArrayList<>();
    private PrintWriter output;

    private int maxReferences = 10;

    public Scanner(Predicate<Class<?>> filter) {
        this.filter = filter;
    }

    private static List<Class<?>> list(ClassLoader classLoader) {
        Class<?> classLoaderClass = classLoader.getClass();
        while (classLoaderClass != ClassLoader.class) {
            classLoaderClass = classLoaderClass.getSuperclass();
        }
        return Unreflection.readField(classLoader, ClassLoader.class, "classes", Vector.class);
    }

    private static boolean shouldIgnore(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz.isAnnotation()
                || ClassLoader.class.isAssignableFrom(clazz)
                || clazz.equals(Pattern.class)
                || clazz.equals(String.class)
                || clazz.equals(Void.class)
                || clazz.equals(Boolean.class)
                || clazz.equals(Character.class)
                || clazz.equals(Byte.class)
                || clazz.equals(Short.class)
                || clazz.equals(Integer.class)
                || clazz.equals(Long.class)
                || clazz.equals(Float.class)
                || clazz.equals(Double.class)
                || clazz.getName().contains("CGLIB$$")
                || clazz.getName().contains("$Proxy")
                || (clazz.getPackage() != null && clazz.getPackage().equals(Classes.class.getPackage()))
                || (clazz.getPackage() != null && clazz.getPackage().equals(Scanner.class.getPackage()))
                || (clazz.getPackage() != null && clazz.getPackage().equals(Field.class.getPackage()));
    }

    private static boolean shouldCascade(Class<?> clazz) {
        return !clazz.isPrimitive()
                && !clazz.isArray()
                && !clazz.isEnum();
    }

    private static Object unwrap(Object proxy) {
        return EngineInstance.get().unwrap(proxy);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> T getThreadLocalEntries(Object instance, ThreadLocal<?> threadLocal) {
        try {
            Method declaredMethod = Unreflection.getDeclaredMethod(instance.getClass(), "getEntry", ThreadLocal.class);
            declaredMethod.setAccessible(true);
            return (T) declaredMethod.invoke(instance, threadLocal);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T readDeclaredField(Object instance, String field) {
        Class<?> visitingClass = instance.getClass();

        while (visitingClass != null && !Object.class.equals(visitingClass)) {
            Field declaredField = null;
            try {
                declaredField = Unreflection.getDeclaredField(visitingClass, field);
            } catch (NoSuchFieldException e) {
                // ignore
            }
            if (declaredField != null) {
                declaredField.setAccessible(true);
                try {
                    return (T) declaredField.get(instance);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Unable to read ThreadGroup." + field, e);
                }
            }
            visitingClass = visitingClass.getSuperclass();
        }
        throw new IllegalStateException("Cannot find ThreadGroup." + field);
    }

    public void setOutput(PrintWriter output) {
        this.output = output;
    }

    public void addSuite(Supplier<Suite> suiteSupplier) {
        inspections.addAll(suiteSupplier.get().register(this));
    }

    public List<InspectionResult> analyze() {
        List<InspectionResult> results = new ArrayList<>();
        List<Reference> references = listAllReferences();
        for (Inspection inspection : inspections) {
            InspectionResult result = new InspectionResult(inspection);
            references.stream()
                    .filter(inspection.getPredicate())
                    .forEach(result::add);
            if (result.hasReferences()) {
                results.add(result);
            }
        }
        return results;
    }

    public boolean isInScope(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Class) {
            return filter.test((Class<?>) object);
        } else {
            return filter.test(object.getClass());
        }
    }

    private List<Reference> listAllReferences() {
        List<Reference> result = new ArrayList<>();
        for (Map.Entry<Object, ObjectData> entry : visitedObjects.entrySet()) {
            boolean ownerInScope = isInScope(entry.getKey());
            ObjectData data = entry.getValue();
            for (ObjectValue value : data.getValues()) {
                if (ownerInScope && (value.getField() == null || isInScope(value.getField().getDeclaringClass()))) {
                    result.add(Reference.from(entry.getKey(), value, this));
                }
            }
        }
        return result;
    }

    @Nullable
    public ObjectData getData(Object object) {
        return visitedObjects.get(object);
    }

    public List<Reference> getBackreferences(Object object) {
        return backreferences.getOrDefault(object, Collections.emptyList());
    }

    public void clear() {
        visitedObjects.clear();
        backreferences.clear();
    }

    public void visit(Object someObject) {
        output.println("visiting: " + StringEscapeUtils.escapeHtml4(Objects.toString(someObject)));
        scannerQueue.add(someObject);
        processQueue();
    }

    public void visitClassLoaders() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        while (classLoader != null) {
            output.println("class loader: " + StringEscapeUtils.escapeHtml4(Objects.toString(classLoader)));
            list(classLoader).forEach(scannerQueue::add);
            classLoader = classLoader.getParent();
        }
        output.println();
        processQueue();
    }

    private void processQueue() {
        output.println("<div class='container'>");
        long stamp = System.nanoTime() - DISPLAY_INTERVAL;
        int before = visitedObjects.size();
        Object objectToVisit = scannerQueue.poll();
        while (objectToVisit != null) {
            long now = System.nanoTime();
            long diff = now - stamp;
            if (diff >= DISPLAY_INTERVAL) {
                output.println("<div class='frame'>instance: " + (visitedObjects.size() - before) + "</div>");
                stamp = now;
            }
            if (objectToVisit instanceof Class) {
                visitClass((Class<?>) objectToVisit);
            } else {
                if (!visitedObjects.containsKey(objectToVisit.getClass())) {
                    visitClass(objectToVisit.getClass());
                }
                visitObject(objectToVisit);
            }
            objectToVisit = scannerQueue.poll();
        }
        propagateScopes();
        output.println("</div>");
    }

    private void propagateScopes() {
        Queue<Object> propagationQueue = new ArrayDeque<>();
        Map<String, AtomicInteger> scopeStats = new LinkedHashMap<>();
        visitedObjects.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue().getScope() != null)
                .forEach(entry -> {
                    scopeStats.computeIfAbsent(entry.getValue().getScope(), s -> new AtomicInteger(0)).incrementAndGet();
                    propagationQueue.add(entry.getKey());
                });
        long stamp = System.nanoTime() - DISPLAY_INTERVAL;
        Object objectToVisit = propagationQueue.poll();
        while (objectToVisit != null) {
            long now = System.nanoTime();
            long diff = now - stamp;
            if (diff >= DISPLAY_INTERVAL) {
                output.print("<div class='frame'>");
                for (Map.Entry<String, AtomicInteger> stat : scopeStats.entrySet()) {
                    output.println(stat.getKey() + ": " + stat.getValue().get());
                }
                output.println("</div>");
                stamp = now;
            }
            ObjectData objectData = visitedObjects.get(objectToVisit);
            if (!objectToVisit.getClass().getName().contains("$Lambda")) { // Do not propagate through lambdas
                for (ObjectValue objectValue : objectData.getValues()) {
                    if (objectValue != null) { // Don't propagate scope to values
                        ObjectData other = visitedObjects.get(objectValue.getValue());
                        if (other != null
                                && EngineInstance.get().shouldPropagateContext(other.getScope(), objectData.getScope())
                                && !other.hasOwnScope()) {
                            scopeStats.computeIfAbsent(other.getScope(), s -> new AtomicInteger(0)).decrementAndGet();
                            other.setInheritedScope(objectData.getScope());
                            scopeStats.computeIfAbsent(other.getScope(), s -> new AtomicInteger(0)).incrementAndGet();
                            propagationQueue.add(objectValue.getValue());
                        }
                    }
                }
            }
            objectToVisit = propagationQueue.poll();
        }
        output.print("<div class='frame'>");
        for (Map.Entry<String, AtomicInteger> stat : scopeStats.entrySet()) {
            output.println(stat.getKey() + ": " + stat.getValue().get());
        }
        output.println();
        output.println("analyzing...");
        output.println("</div>");
    }

    private void visitClass(Class<?> objectToVisit) {
        if (shouldIgnore(objectToVisit)) {
            // Deliberately skip primitives and self
            return;
        }

        if (objectToVisit.getSuperclass() != null && !visitedObjects.containsKey(objectToVisit.getSuperclass()) && !shouldIgnore(objectToVisit.getSuperclass())) {
            scannerQueue.add(objectToVisit.getSuperclass());
        }

        ObjectData objectData = new ObjectData("static");
        visitedObjects.put(objectToVisit, objectData);

        for (Field field : Unreflection.getDeclaredFields(objectToVisit)) {
            if (Modifier.isStatic(field.getModifiers())
                    && !(field.isSynthetic() && "$assertionsDisabled".equals(field.getName()))) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    processFieldValue(objectToVisit, objectData, field, value);
                } catch (Exception e) {
                    // ignore and skip
                }
            }
        }
    }

    private void visitObject(Object objectToVisit) {
        Class<?> visitingClass = objectToVisit.getClass();
        if (shouldIgnore(visitingClass)) {
            // Deliberately skip primitives and self
            return;
        }
        Optional<String> detectedScope = ScopeDetector.detect(visitingClass);
        ObjectData objectData = new ObjectData(detectedScope.orElse(null));
        visitedObjects.put(objectToVisit, objectData);
        Object unwrappedObject = unwrap(objectToVisit);
        if (unwrappedObject != objectToVisit && !shouldIgnore(unwrappedObject.getClass()) && shouldCascade(unwrappedObject.getClass())) {
            scannerQueue.add(unwrappedObject);
        }

        if (visitingClass.isArray() && !shouldIgnore(visitingClass.getComponentType())) {
            for (Object value : (Object[]) objectToVisit) {
                if (value != null) {
                    ObjectValue objectValue = new ObjectValue(ReferenceType.ARRAY_ITEM, value);
                    objectData.addValue(objectValue);
                    if (!shouldIgnore(value.getClass())) {
                        backreferences.computeIfAbsent(value, v -> new ArrayList<>())
                                .add(Reference.from(objectToVisit, objectValue, this));
                    }
                    if (!visitedObjects.containsKey(value) && shouldCascade(value.getClass())) {
                        scannerQueue.add(value);
                    }
                }
            }
        } else {
            while (!Object.class.equals(visitingClass)) {
                for (Field field : Unreflection.getDeclaredFields(visitingClass)) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(objectToVisit);
                            processFieldValue(objectToVisit, objectData, field, value);
                        } catch (Exception e) {
                            // ignore and skip
                        }
                    }
                }

                visitingClass = visitingClass.getSuperclass();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processFieldValue(Object objectToVisit, ObjectData objectData, Field field, Object value) {
        boolean cascade = true;
        if (value != null) {
            Class<?> valueClass = value.getClass();
            if (valueClass.isArray() && !shouldIgnore(valueClass.getComponentType())) {
                for (Object o : (Object[]) value) {
                    pushValue(objectToVisit, objectData, field, ReferenceType.ARRAY_ITEM, o, true);
                }
                cascade = false;
            } else if (value instanceof Optional) {
                ((Optional<?>) value).ifPresent(o -> {
                    pushValue(objectToVisit, objectData, field, ReferenceType.OPTIONAL_VALUE, o, true);
                });
                cascade = false;
            } else if (value instanceof java.lang.ref.Reference) {
                pushValue(objectToVisit, objectData, field, ReferenceType.REFERENCE_VALUE, ((java.lang.ref.Reference<?>) value).get(), true);
                cascade = false;
            } else if (value instanceof AtomicReference) {
                pushValue(objectToVisit, objectData, field, ReferenceType.ATOMIC_REFERENCE_VALUE, ((AtomicReference<?>) value).get(), true);
                cascade = false;
            } else if (value instanceof Collection) {
                List<Object> values = new ArrayList<>((Collection<?>) value);
                for (Object o : values) {
                    pushValue(objectToVisit, objectData, field, ReferenceType.COLLECTION_ITEM, o, true);
                }
                cascade = false;
            } else if (value instanceof ThreadLocal) {
                ThreadLocal<Object> tlValue = (ThreadLocal<Object>) value;
                pushThreadLocalValues(objectToVisit, objectData, field, tlValue);
                cascade = false;
            } else if (value instanceof Map) {
                List<? extends Map.Entry<?, ?>> entries = new ArrayList<>(((Map<?, ?>) value).entrySet());
                for (Map.Entry<?, ?> e : entries) {
                    ObjectValue keyValue = pushValue(objectToVisit, objectData, field, ReferenceType.MAP_KEY, e.getKey(), true);
                    pushValue(objectToVisit, objectData, field, ReferenceType.MAP_VALUE, e.getValue(), true);
                    if (e.getValue() != null && !shouldIgnore(e.getValue().getClass())) {
                        backreferences.computeIfAbsent(e.getValue(), v -> new ArrayList<>())
                                .add(Reference.from(e.getKey(), keyValue, this));
                    }

                }
                cascade = false;
            }
        }
        pushValue(objectToVisit, objectData, field, ReferenceType.ACTUAL_VALUE, value, cascade);
    }

    private void pushThreadLocalValues(Object objectToVisit, ObjectData objectData, Field field, ThreadLocal<Object> value) {
        // Get the root ThreadGroup
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        while (group.getParent() != null) {
            group = group.getParent();
        }
        // Collect all Thread instances out there
        List<Thread> allThreads = new ArrayList<>();
        Queue<ThreadGroup> queue = new ArrayDeque<>();
        queue.add(group);

        while (queue.peek() != null) {
            ThreadGroup poll = queue.poll();
            synchronized (poll) {
                int ngroups = readDeclaredField(poll, "ngroups");
                ThreadGroup[] groups = readDeclaredField(poll, "groups");
                int nthreads = readDeclaredField(poll, "nthreads");
                Thread[] threads = readDeclaredField(poll, "threads");
                if (groups != null && ngroups > 0) {
                    queue.addAll(Arrays.asList(groups).subList(0, ngroups));
                }
                if (threads != null && nthreads > 0) {
                    allThreads.addAll(Arrays.asList(threads).subList(0, nthreads));
                }
            }
        }

        // Get the ThreadLocalMap for each Thread
        for (Thread thread : allThreads) {
            Object threadLocals = readDeclaredField(thread, "threadLocals");
            if (threadLocals != null) {
                Object entry = getThreadLocalEntries(threadLocals, value);
                if (entry != null) {
                    Object threadLocalValue = readDeclaredField(entry, "value");
                    if (threadLocalValue != value) {
                        if (thread.getState() == Thread.State.TERMINATED) {
                            pushValue(objectToVisit, objectData, field, ReferenceType.TERMINATED_THREAD_LOCAL, threadLocalValue, true);
                        } else if (thread.getState() == Thread.State.WAITING) {
                            pushValue(objectToVisit, objectData, field, ReferenceType.WAITING_THREAD_LOCAL, threadLocalValue, true);
                        } else {
                            pushValue(objectToVisit, objectData, field, ReferenceType.THREAD_LOCAL, threadLocalValue, true);
                        }
                    }
                }
            }
        }
    }

    private ObjectValue pushValue(Object objectToVisit, ObjectData objectData, Field field, ReferenceType name, Object value, boolean cascade) {
        ObjectValue objectValue = new ObjectValue(name, field, value);
        objectData.addValue(objectValue);
        if (value != null) {
            if (!shouldIgnore(value.getClass())) {
                backreferences.computeIfAbsent(value, v -> new ArrayList<>())
                        .add(Reference.from(objectToVisit, objectValue, this));
            }
            if (cascade && !visitedObjects.containsKey(value) && shouldCascade(value.getClass())) {
                scannerQueue.add(value);
            }
        }
        return objectValue;
    }

    public int getMaxReferences() {
        return maxReferences;
    }

    public void setMaxReferences(int maxReferences) {
        this.maxReferences = maxReferences;
    }
}
