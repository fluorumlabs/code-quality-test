package org.vaadin.qa.cqt.suites;

import org.vaadin.qa.cqt.*;
import org.vaadin.qa.cqt.annotations.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

import static org.vaadin.qa.cqt.suites.Classes.*;

@SuppressWarnings("unchecked")
public class CollectionInspections extends Suite {

    private static final Field UNDERLYING_MAP_FIELD;
    private static final Field UNDERLYING_SET_FROM_MAP_FIELD;
    private static final Field UNDERLYING_COLLECTION_FIELD;
    private static final String[] MAP_MUTATION_METHODS = {"put", "remove", "putAll", "clear", "replaceAll", "putIfAbsent", "replace", "computeIfAbsent", "computeIfPresent", "compute", "merge"};
    private static final String[] COLLECTION_MUTATION_METHODS = {"add", "remove", "addAll", "removeAll", "removeIf", "retainAll", "clear"};

    static {
        try {
            UNDERLYING_MAP_FIELD = Unreflection.getDeclaredField(SYNCHRONIZED_MAP, "m");
            UNDERLYING_MAP_FIELD.setAccessible(true);
            UNDERLYING_SET_FROM_MAP_FIELD = Unreflection.getDeclaredField(SET_FROM_MAP, "m");
            UNDERLYING_SET_FROM_MAP_FIELD.setAccessible(true);
            UNDERLYING_COLLECTION_FIELD = Unreflection.getDeclaredField(SYNCHRONIZED_COLLECTION, "c");
            UNDERLYING_COLLECTION_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private static Predicate<Reference> underlyingMap(Predicate<Class<?>> predicate) {
        return reference -> {
            if (reference.getTarget() == null) {
                return false;
            }

            Field field;
            if (Classes.SYNCHRONIZED_MAP.isAssignableFrom(reference.getTargetClass())) {
                field = UNDERLYING_MAP_FIELD;
            } else if (Classes.SET_FROM_MAP.isAssignableFrom(reference.getTargetClass())) {
                field = UNDERLYING_SET_FROM_MAP_FIELD;
            } else {
                return false;
            }

            try {
                Object map = field.get(reference.getTarget());
                if (map != null) {
                    return predicate.test(map.getClass());
                }
            } catch (IllegalAccessException e) {
                // ignore
            }
            return false;
        };
    }

    private static Predicate<Reference> underlyingCollection(Predicate<Class<?>> predicate) {
        return reference -> {
            if (reference.getTarget() == null || !(Classes.SYNCHRONIZED_COLLECTION.isAssignableFrom(reference.getTargetClass()))) {
                return false;
            }
            try {
                Object map = UNDERLYING_COLLECTION_FIELD.get(reference.getTarget());
                if (map != null) {
                    return predicate.test(map.getClass());
                }
            } catch (IllegalAccessException e) {
                // ignore
            }
            return false;
        };
    }

    private Predicate<Reference> exposedOrModified() {
        return or(
                and(
                        field(isNotPrivate()).or(fieldIsExposedViaGetter().and(field(getter(isNotPrivate())))),
                        backreference(
                                field(isNotPrivate()).or(fieldIsExposedViaGetter().and(field(getter(isNotPrivate()))))
                        )
                ),
                field(
                        isStatic(),
                        type(is(Map.class)),
                        calledByNonClassInit(MAP_MUTATION_METHODS)
                ),
                field(
                        isStatic(),
                        type(is(Collection.class)),
                        calledByNonClassInit(COLLECTION_MUTATION_METHODS)
                ),
                field(
                        isNotStatic(),
                        type(is(Map.class)),
                        calledByNonConstructor(MAP_MUTATION_METHODS)
                ),
                field(
                        isNotStatic(),
                        type(is(Collection.class)),
                        calledByNonConstructor(COLLECTION_MUTATION_METHODS)
                )
        );
    }

    /**
     * @return
     */
    @ProbableError("Non-thread-safe mutable collection")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> nonThreadSafeCollection() {
        return and(
                ownerType(isNotAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")),
                targetType(
                        is(Map.class, Collection.class),
                        isNot(THREAD_SAFE_COLLECTIONS),
                        isNot(UNMODIFIABLE_COLLECTIONS),
                        isNot(SET_FROM_MAP)
                ),
                field(isNotAnnotatedWith("org.springframework.beans.factory.annotation.Value")),
                target(Objects::nonNull),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE),
                exposedOrModified()
        );
    }

    @ProbableError("Non-thread-safe mutable collection")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> nonThreadSafeSetFromMap() {
        return and(
                ownerType(isNotAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")),
                targetType(is(SET_FROM_MAP)),
                underlyingMap(
                        isNot(THREAD_SAFE_COLLECTIONS)
                                .and(isNot(UNMODIFIABLE_COLLECTIONS))
                                .and(isNot(SET_FROM_MAP))
                ),
                field(isNotAnnotatedWith("org.springframework.beans.factory.annotation.Value")),
                target(Objects::nonNull),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE),
                exposedOrModified()
        );
    }

    @Warning("Potentially non-thread-safe mutable collection (was null)")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> nonThreadSafeCollectionWasNull() {
        return and(
                ownerType(isNotAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")),
                field(type(
                        is(Map.class, Collection.class),
                        isNot(THREAD_SAFE_COLLECTIONS),
                        isNot(UNMODIFIABLE_COLLECTIONS),
                        isNot(SET_FROM_MAP)
                )),
                field(isNotAnnotatedWith("org.springframework.beans.factory.annotation.Value")),
                target(Objects::isNull),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE),
                exposedOrModified()
        );
    }

    /**
     * By explicitly specifying type of the fields, it would be easier to spot problematic patterns like following:
     * ConcurrentMap<String, Entity> entities = getEntities();
     * if (!entities.containsKey(key)) {
     * entities.put(key, entity);
     * } else {
     * ...
     * }
     * It should be pretty obvious that there might be a race condition because an entity may be put into the map by a concurrent thread between the calls to containsKey() and put() (see https://github.com/code-review-checklists/java-concurrency#chm-race about this type of race conditions).
     * While if the type of the entities variable was just Map<String, Entity> it would be less obvious and readers might think this is only slightly suboptimal code and pass by.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#concurrent-map-type
     *
     * @return
     */
    @Advice("Store ConcurrentSkipListMap in fields of type ConcurrentMap or ConcurrentSkipListMap")
    public Predicate<Reference> cslmFieldType() {
        return and(
                targetType(is(ConcurrentSkipListMap.class)),
                field(type(isNot(ConcurrentMap.class))),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * ConcurrentHashMap (unlike the generic ConcurrentMap interface) guarantees that the lambdas passed into compute()-like methods are performed atomically per key, and the thread safety of the class may depend on that guarantee.
     * If used in conjunction with a static analysis rule that prohibits calling compute()-like methods on ConcurrentMap-typed objects that are not ConcurrentHashMaps it could prevent some bugs:
     * e. g. calling compute() on a ConcurrentSkipListMap might be a race condition and it’s easy to overlook that for somebody who is used to rely on the strong semantics of compute() in ConcurrentHashMap.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#chm-type
     *
     * @return
     */
    @Advice("Store ConcurrentHashMap in fields of type ConcurrentHashMap")
    public Predicate<Reference> chmFieldType() {
        return and(
                targetType(is(ConcurrentHashMap.class)),
                field(type(isNot(ConcurrentHashMap.class))),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return
     */
    @Advice("Use non-blocking ConcurrentHashMap instead of Hashtable")
    public Predicate<Reference> chmForHashtable() {
        return and(
                targetType(isExactly(Hashtable.class)),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return
     */
    @Advice("Use non-blocking ConcurrentHashMap instead of Collections.synchronizedMap(HashMap)")
    public Predicate<Reference> chmForSynchronizedHashMap() {
        return and(
                targetType(is(Classes.SYNCHRONIZED_MAP)),
                underlyingMap(is(HashMap.class)),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * Note, however, that unlike ConcurrentHashMap with its computeIfAbsent() method ClassValue doesn’t guarantee that per-class value is computed only once, i. e. ClassValue.computeValue() might be executed by multiple concurrent threads.
     * So if the computation inside computeValue() is not thread-safe, it should be synchronized separately.
     * On the other hand, ClassValue does guarantee that the same value is always returned from ClassValue.get() (unless remove() is called).
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#use-class-value
     *
     * @return
     */
    @Suggestion("Consider using ClassValue instead of Map<Class, ...>")
    public Predicate<Reference> cvForClassMap() {
        return and(
                field(
                        isStatic(),
                        type(is(Map.class)),
                        genericType(0, is(Class.class)),
                        genericType(1, clazz -> getScanner().isInScope(clazz))
                ),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * Note: ConcurrentSkipListMap is not the state of the art concurrent sorted dictionary implementation.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return
     */
    @Advice("Use non-blocking ConcurrentSkipListMap instead of Collections.synchronizedMap(TreeMap)")
    public Predicate<Reference> cslmForSynchronizedTreeMap() {
        return and(
                targetType(is(Classes.SYNCHRONIZED_MAP)),
                underlyingMap(is(TreeMap.class)),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return
     */
    @Advice("Use non-blocking ConcurrentHashMap.newKeySet() instead of Collections.synchronizedSet(HashSet)")
    public Predicate<Reference> chmKeySetForSynchronizedHashSet() {
        return and(
                targetType(is(Classes.SYNCHRONIZED_SET)),
                underlyingCollection(is(HashSet.class)),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return
     */
    @Advice("Use non-blocking ConcurrentSkipListSet instead of Collections.synchronizedSet(TreeSet)")
    public Predicate<Reference> cslsForSynchronizedTreeSet() {
        return and(
                targetType(is(Classes.SYNCHRONIZED_SET)),
                underlyingCollection(is(TreeSet.class)),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return
     */
    @Advice("Use non-blocking CopyOnWriteArrayList instead of Collections.synchronizedList(ArrayList)")
    public Predicate<Reference> cowalForSynchronizedArrayList() {
        return and(
                targetType(is(Classes.SYNCHRONIZED_LIST)),
                underlyingCollection(is(ArrayList.class)),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return
     */
    @Advice("Use non-blocking CopyOnWriteArrayList instead of Vector")
    public Predicate<Reference> cowalForVector() {
        return and(
                targetType(is(Vector.class)),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return
     */
    @Advice("Use non-blocking ConcurrentLinkedQueue instead of LinkedBlockingQueue")
    public Predicate<Reference> clqForLinkedBlockingQueue() {
        return and(
                targetType(is(LinkedBlockingQueue.class)),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return
     */
    @Advice("Use non-blocking ConcurrentLinkedDeque instead of LinkedBlockingDeque")
    public Predicate<Reference> cldForLinkedBlockingQueue() {
        return and(
                targetType(is(LinkedBlockingDeque.class)),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * @return
     */
    @Advice("Use EnumMap instead of Map<Enum, ...>")
    public Predicate<Reference> emForMapEnum() {
        return and(
                field(
                        type(is(Map.class)),
                        genericType(0, isEnum())
                ),
                targetType(isNot(EnumMap.class), isNot(THREAD_SAFE_COLLECTIONS), isNot(UNMODIFIABLE_COLLECTIONS)),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * @return
     */
    @Advice("Use EnumSet instead of Set<Enum>")
    public Predicate<Reference> esForSetEnum() {
        return and(
                field(
                        type(is(Set.class)),
                        genericType(0, isEnum())
                ),
                targetType(isNot(EnumSet.class), isNot(THREAD_SAFE_COLLECTIONS), isNot(UNMODIFIABLE_COLLECTIONS)),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * @return
     */
    @Warning("Modifiable collection exposed via non-private field or non-private getter")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> exposedModifiableCollection() {
        return and(
                ownerType(isNotPrivateClass()),
                targetType(is(Collection.class, Map.class), isNot(UNMODIFIABLE_COLLECTIONS)),
                field(isNotPrivate()).or(fieldIsExposedViaGetter().and(field(getter(isNotPrivate())))),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

    /**
     * @return
     */
    @Warning("Array exposed via non-private field or non-private getter")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> exposedArray() {
        return and(
                ownerType(isNotPrivateClass()),
                targetType(isArray()),
                field(isNotPrivate()).or(fieldIsExposedViaGetter().and(field(getter(isNotPrivate())))),
                referenceTypeIs(ReferenceType.ACTUAL_VALUE)
        );
    }

}
