package org.vaadin.qa.cqt;

import org.apache.commons.text.StringEscapeUtils;
import org.vaadin.qa.cqt.engine.EngineInstance;
import org.vaadin.qa.cqt.suites.Classes;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Created by Artem Godin on 9/23/2020.
 */
public class Scanner {
    private static final long DISPLAY_INTERVAL = TimeUnit.MILLISECONDS.toNanos(200);

    private final Queue<Object> scannerQueue = new ArrayDeque<>();
    private final Map<Object, ObjectData> visitedObjects = new IdentityHashMap<>();
    private final Map<String, Object> objectHashes = new HashMap<>();
    private final Map<Object, List<Reference>> backreferences = new IdentityHashMap<>();
    private final Predicate<Class<?>> filter;
    private final List<Inspection> inspections = new ArrayList<>();
    private PrintWriter output;

    private int maxReferences = 10;

    public Scanner(Predicate<Class<?>> filter) {
        this.filter = filter;
    }

    public Scanner(String... packages) {
        this.filter = Stream.of(packages)
                .<Predicate<Class<?>>>map(p -> (clazz -> clazz.getName().startsWith(p)))
                .reduce(Predicate::or)
                .orElse(x -> true);
    }

    public void setOutput(PrintWriter output) {
        this.output = output;
    }

    public static String computeHash(Object x) {
        Class<?> clazz = x.getClass();
        if (shouldIgnore(clazz)) {
            return "";
        }
        return clazz.getName()+"-"+System.identityHashCode(x);
    }

    private static Iterator<Class<?>> list(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
        Class<?> classLoaderClass = classLoader.getClass();
        while (classLoaderClass != ClassLoader.class) {
            classLoaderClass = classLoaderClass.getSuperclass();
        }
        List<Class<?>> classes = Unreflection.readField(classLoader, ClassLoader.class, "classes", Vector.class);
        return classes.iterator();
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

    public static <T> T invokeDeclaredMethod(Object instance, String method) {
        try {
            Method declaredMethod = Unreflection.getDeclaredMethod(instance.getClass(), method);
            declaredMethod.setAccessible(true);
            return (T) declaredMethod.invoke(instance);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    public static <T, A> T invokeDeclaredMethod(Object instance, String method, Class<A> arg1Type, A arg1) {
        try {
            Method declaredMethod = Unreflection.getDeclaredMethod(instance.getClass(),method, arg1Type);
            declaredMethod.setAccessible(true);
            return (T) declaredMethod.invoke(instance, arg1);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    public static <T> T readDeclaredField(Object instance, String field) {
        Class<?> visitingClass = instance.getClass();

        while (visitingClass != null && !Object.class.equals(visitingClass)) {
            Field declaredField = null;
            try {
                declaredField = Unreflection.getDeclaredField(visitingClass,field);
            } catch (NoSuchFieldException e) {
                // ignore
            }
            if (declaredField != null) {
                declaredField.setAccessible(true);
                try {
                    return (T) declaredField.get(instance);
                } catch (IllegalAccessException e) {
                    return null;
                }
            }
            visitingClass = visitingClass.getSuperclass();
        }
        return null;
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
            if (!result.isEmpty()) {
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

    public void dumpObject(String hash) {
        Object object = objectHashes.get(hash);
        if (object == null || !visitedObjects.containsKey(object)) {
            output.println("No printable object instance found with specified id");
            return;
        }
        Reference reference = Reference.from(object, this);
        List<Reference> values = listUnfilteredReferences(object);
        List<Reference> staticValues = listUnfilteredReferences(object.getClass());

        String contextPath = reference.formatPathToContext();
        output.append(String.format("Class:         %s\n", escapeHtml4(reference.formatOwnerClass())));
        output.append(String.format("Context:       %s\n", escapeHtml4(reference.formatScope())));
        if (!contextPath.isEmpty()) {
            output.append(String.format("Context path:  %s\n", escapeHtml4(contextPath)));
        }
        if (reference.getTarget() != null) {
            output.append(String.format("Value:         %s\n", reference.formatValue()));
        }
        List<String> backrefs = reference.formatBackreferences().stream()
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        int counter = 0;
        for (String backref : backrefs) {
            if (counter == 0) {
                output.append(String.format("Referenced by: %s\n", backref));
            } else {
                output.append(String.format("               %s\n", backref));
            }
            counter++;
            if (counter> maxReferences) {
                output.append(String.format("               ... %d more\n", backrefs.size()-counter));
                break;
            }
        }
        output.println();
        List<Reference> orderedStaticValues = staticValues.stream()
                .sorted(Comparator.comparing(Reference::formatPartial))
                .collect(Collectors.toList());

        int maxLength = Stream.concat(values.stream(), staticValues.stream()).map(Reference::formatField).map(String::length).max(Integer::compareTo).orElse(0);

        for (Reference value : orderedStaticValues) {
            String fieldName = value.formatField();
            if (value.getReferenceType()!=ReferenceType.ACTUAL_VALUE) {
                fieldName = new String(new char[fieldName.length() - 2 * value.getReferenceType().toString().length()]).replace("\0", " ") + value.getReferenceType();
            }
            String padding = new String(new char[maxLength - fieldName.length()]).replace("\0", " ");
            output.format("%s%s = %s\n", escapeHtml4(fieldName), padding, value.formatValue());
        }

        if (!orderedStaticValues.isEmpty()) {
            output.println();
        }

        List<Reference> orderedValues = values.stream()
                .sorted(Comparator.comparing(Reference::formatPartial))
                .collect(Collectors.toList());

        for (Reference value : orderedValues) {
            String fieldName = value.formatField();
            if (value.getReferenceType()!=ReferenceType.ACTUAL_VALUE) {
                fieldName = new String(new char[fieldName.length() - 2 * value.getReferenceType().toString().length()]).replace("\0", " ") + value.getReferenceType();
            }
            String padding = new String(new char[maxLength-fieldName.length()]).replace("\0", " ");
            output.format("%s%s = %s\n", escapeHtml4(fieldName), padding, value.formatValue());
        }
    }

    public List<Reference> listUnfilteredReferences(Object object) {
        if (object == null) {
            return Collections.emptyList();
        }
        List<Reference> result = new ArrayList<>();

        ObjectData data = visitedObjects.get(object);

        for (ObjectValue value : data.getValues()) {
            result.add(Reference.from(object, value, this));
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
        objectHashes.clear();
        backreferences.clear();
    }

    public void visit(Object someObject) {
        output.println("Visiting " + StringEscapeUtils.escapeHtml4(Objects.toString(someObject)));
        scannerQueue.add(someObject);
        processQueue();
    }

    public void visit(Object... someObjects) {
        for (Object someObject : someObjects) {
            output.println("Visiting " + StringEscapeUtils.escapeHtml4(Objects.toString(someObject)));
            scannerQueue.add(someObject);
        }
        processQueue();
    }

    public void visitClassLoaders() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        while (classLoader != null) {
            output.println("Visiting " + StringEscapeUtils.escapeHtml4(Objects.toString(classLoader)));
            try {
                Iterator<Class<?>> iter = list(classLoader);
                while (iter.hasNext()) {
                    scannerQueue.add(iter.next());
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // ignore
            }
            classLoader = classLoader.getParent();
        }
        output.println();
        processQueue();
    }

    private void processQueue() {
        output.println("Scanning objects...");
        long stamp = System.nanoTime() - DISPLAY_INTERVAL;
        int before = visitedObjects.size();
        Object objectToVisit = scannerQueue.poll();
        while (objectToVisit != null) {
            long now = System.nanoTime();
            long diff = now - stamp;
            if (diff >= DISPLAY_INTERVAL) {
                output.println("Queued: " + scannerQueue.size() + "\tVisited: " + (visitedObjects.size() - before));
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
        int after = visitedObjects.size();
        output.println("Added " + (after - before) + " objects");
        output.println();
        propagateScopes();
    }

    private void propagateScopes() {
        output.println("Propagating scopes...");
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
                for (Map.Entry<String, AtomicInteger> stat : scopeStats.entrySet()) {
                    output.print(stat.getKey() + ": " + stat.getValue().get() + "\t");
                }
                output.println("Queued: " + propagationQueue.size());
                stamp = now;
            }
            ObjectData objectData = visitedObjects.get(objectToVisit);
            for (ObjectValue objectValue : objectData.getValues()) {
                if (objectValue != null) { // Don't propagate scope to values
                    ObjectData other = visitedObjects.get(objectValue.getValue());
                    if (other != null
                            && ("instance".equals(other.getScope())
                            || ("singleton".equals(objectData.getScope()) && !"singleton".equals(other.getScope())))
                            && !objectData.getScope().equals(other.getScope())) {
                        scopeStats.computeIfAbsent(other.getScope(), s -> new AtomicInteger(0)).decrementAndGet();
                        other.setInheritedScope(objectData.getScope());
                        scopeStats.computeIfAbsent(other.getScope(), s -> new AtomicInteger(0)).incrementAndGet();
                        propagationQueue.add(objectValue.getValue());
                    }
                }
            }
            objectToVisit = propagationQueue.poll();
        }
        for (Map.Entry<String, AtomicInteger> stat : scopeStats.entrySet()) {
            output.print(stat.getKey() + ": " + stat.getValue().get() + "\t");
        }
        output.println();
    }

    private void visitClass(Class<?> objectToVisit) {
        Class<?> visitingClass = objectToVisit;
        if (shouldIgnore(visitingClass)) {
            // Deliberately skip primitives and self
            return;
        }

        if (visitingClass.getSuperclass() != null && !visitedObjects.containsKey(visitingClass.getSuperclass()) && !shouldIgnore(visitingClass.getSuperclass())) {
            scannerQueue.add(visitingClass.getSuperclass());
        }

        ObjectData objectData = new ObjectData("static"); //FIXME
        String hash = computeHash(objectToVisit);
        if (!hash.isEmpty()) {
            objectHashes.put(hash, objectToVisit);
        }
        visitedObjects.put(objectToVisit, objectData);

        for (Field field : Unreflection.getDeclaredFields(visitingClass)) {
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
        ObjectData objectData = new ObjectData(detectedScope.orElse(null)); //FIXME
        String hash = computeHash(objectToVisit);
        if (!hash.isEmpty()) {
            objectHashes.put(hash, objectToVisit);
        }
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
                for (int i = 0; i < ngroups; i++) {
                    queue.add(groups[i]);
                }
                for (int i = 0; i < nthreads; i++) {
                    allThreads.add(threads[i]);
                }
            }
        }

        // Get the ThreadLocalMap for each Thread
        for (Thread thread : allThreads) {
            Object threadLocals = readDeclaredField(thread, "threadLocals");
            if (threadLocals != null) {
                Object entry = invokeDeclaredMethod(threadLocals, "getEntry", ThreadLocal.class, value);
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
