/*
 * Copyright (c) 2020 Artem Godin
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.vaadin.qa.cqt.suites;

import org.vaadin.qa.cqt.Suite;
import org.vaadin.qa.cqt.annotations.*;
import org.vaadin.qa.cqt.data.Reference;
import org.vaadin.qa.cqt.data.ReferenceType;
import org.vaadin.qa.cqt.utils.Unreflection;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

import static org.vaadin.qa.cqt.utils.Classes.*;

/**
 * The type Collection inspections.
 */
@SuppressWarnings("unchecked")
public class CollectionInspections extends Suite {

    private static final String[] COLLECTION_MUTATION_METHODS = {
            "add",
            "remove",
            "addAll",
            "removeAll",
            "removeIf",
            "retainAll",
            "clear"
    };

    private static final String[] MAP_MUTATION_METHODS = {
            "put",
            "remove",
            "putAll",
            "clear",
            "replaceAll",
            "putIfAbsent",
            "replace",
            "computeIfAbsent",
            "computeIfPresent",
            "compute",
            "merge"
    };

    private static final Field UNDERLYING_COLLECTION_FIELD;

    private static final Field UNDERLYING_MAP_FIELD;

    private static final Field UNDERLYING_SET_FROM_MAP_FIELD;

    static {
        try {
            UNDERLYING_MAP_FIELD = Unreflection.getDeclaredField(
                    SYNCHRONIZED_MAP,
                    "m"
            );
            UNDERLYING_MAP_FIELD.setAccessible(true);
            UNDERLYING_SET_FROM_MAP_FIELD = Unreflection.getDeclaredField(
                    SET_FROM_MAP,
                    "m"
            );
            UNDERLYING_SET_FROM_MAP_FIELD.setAccessible(true);
            UNDERLYING_COLLECTION_FIELD = Unreflection.getDeclaredField(
                    SYNCHRONIZED_COLLECTION,
                    "c"
            );
            UNDERLYING_COLLECTION_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * {@link ConcurrentHashMap} (unlike the generic {@link ConcurrentMap}
     * interface) guarantees that the lambdas passed into {@code compute()}-like
     * methods are performed atomically per key, and the thread safety of the
     * class may depend on that guarantee. If used in conjunction with a static
     * analysis rule that prohibits calling {@code compute()}-like methods on
     * {@link ConcurrentMap}-typed objects that are not {@link
     * ConcurrentHashMap} it could prevent some bugs: e. g. calling {@code
     * compute()} on a {@link ConcurrentSkipListMap} might be a race condition
     * and it’s easy to overlook that for somebody who is used to rely on the
     * strong semantics of {@code compute()} in {@link ConcurrentHashMap}.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#chm-type
     *
     * @return predicate
     */
    @Advice("Store ConcurrentHashMap in fields of type ConcurrentHashMap")
    public Predicate<Reference> field_type_for_ConcurrentHashMap() {
        return and(
                targetType(is(ConcurrentHashMap.class)),
                field(type(isNot(ConcurrentHashMap.class))),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * Use non-blocking {@link ConcurrentHashMap} instead of {@link Hashtable}
     *
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return predicate
     */
    @Advice("Use non-blocking ConcurrentHashMap instead of Hashtable")
    public Predicate<Reference> use_ConcurrentHashMap_instead_of_Hashtable() {
        return and(
                targetType(isExactly(Hashtable.class)),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * By explicitly specifying type of the fields, it would be easier to spot
     * problematic patterns like following:
     *
     * <pre>{@code
     * ConcurrentMap<String, Entity> entities = getEntities();
     * if (!entities.containsKey(key)) {
     *     entities.put(key, entity);
     * } else {
     *     ...
     * }
     * }</pre>
     * It should be pretty obvious that there might be a race condition because
     * an entity may be put into the map by a concurrent thread between the
     * calls to {@code containsKey()} and {@code put()} (see
     * https://github.com/code-review-checklists/java-concurrency#chm-race about
     * this type of race conditions). While if the type of the entities variable
     * was just {@code Map<String, Entity>} it would be less obvious and readers
     * might think this is only slightly suboptimal code and pass by.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#concurrent-map-type
     *
     * @return predicate
     */
    @Advice("Store ConcurrentSkipListMap in fields of type ConcurrentMap or ConcurrentSkipListMap")
    public Predicate<Reference> field_type_for_ConcurrentSkipListMap() {
        return and(
                targetType(is(ConcurrentSkipListMap.class)),
                field(type(isNot(ConcurrentMap.class))),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * Use non-blocking {@link ConcurrentHashMap} instead of {@code
     * Collections.synchronizedMap(HashMap)}
     *
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return predicate
     */
    @Advice("Use non-blocking ConcurrentHashMap instead of Collections.synchronizedMap(HashMap)")
    public Predicate<Reference> use_ConcurrentHashMap_instead_of_synchronized_HashMap() {
        return and(
                targetType(is(SYNCHRONIZED_MAP)),
                underlyingMap(is(HashMap.class)),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    private static Predicate<Reference> underlyingMap(Predicate<Class<?>> predicate) {
        return reference -> {
            if (reference.getTarget() == null) {
                return false;
            }

            Field field;
            if (SYNCHRONIZED_MAP.isAssignableFrom(reference.getTargetClass())) {
                field = UNDERLYING_MAP_FIELD;
            } else if (SET_FROM_MAP.isAssignableFrom(reference.getTargetClass())) {
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

    /**
     * Use non-blocking {@link ConcurrentHashMap#newKeySet()} instead of {@code
     * Collections.synchronizedSet(HashSet)}
     *
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return predicate
     */
    @Advice("Use non-blocking ConcurrentHashMap.newKeySet() instead of Collections.synchronizedSet(HashSet)")
    public Predicate<Reference> use_ConcurrentHashMap_newKeySet_instead_of_synchronized_HashSet() {
        return and(
                targetType(is(SYNCHRONIZED_SET)),
                underlyingCollection(is(HashSet.class)),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    private static Predicate<Reference> underlyingCollection(Predicate<Class<?>> predicate) {
        return reference -> {
            if (reference.getTarget() == null
                || !(SYNCHRONIZED_COLLECTION.isAssignableFrom(reference.getTargetClass()))) {
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

    /**
     * Use non-blocking {@link ConcurrentLinkedDeque} instead of {@link
     * LinkedBlockingDeque}
     *
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return predicate
     */
    @Advice("Use non-blocking ConcurrentLinkedDeque instead of LinkedBlockingDeque")
    public Predicate<Reference> use_ConcurrentLinkedDeque_instead_of_LinkedBlockingDeque() {
        return and(
                targetType(is(LinkedBlockingDeque.class)),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * Use non-blocking {@link ConcurrentLinkedQueue} instead of {@link
     * LinkedBlockingQueue}
     *
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return predicate
     */
    @Advice("Use non-blocking ConcurrentLinkedQueue instead of LinkedBlockingQueue")
    public Predicate<Reference> use_ConcurrentLinkedQueue_instead_of_LinkedBlockingQueue() {
        return and(
                targetType(is(LinkedBlockingQueue.class)),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * Use non-blocking {@link CopyOnWriteArrayList} instead of {@code
     * Collections.synchronizedList(ArrayList)}
     *
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return predicate
     */
    @Advice("Use non-blocking CopyOnWriteArrayList instead of Collections.synchronizedList(ArrayList)")
    public Predicate<Reference> use_CopyOnWriteArrayList_instead_of_synchronized_ArrayList() {
        return and(
                targetType(is(SYNCHRONIZED_LIST)),
                underlyingCollection(is(ArrayList.class)),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * Use non-blocking {@link CopyOnWriteArrayList} instead of {@link Vector}
     *
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return predicate predicate
     */
    @Advice("Use non-blocking CopyOnWriteArrayList instead of Vector")
    public Predicate<Reference> use_CopyOnWriteArrayList_instead_of_Vector() {
        return and(
                targetType(is(Vector.class)),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }


    /**
     * Use non-blocking {@link ConcurrentSkipListMap} instead of {@code
     * Collections.synchronizedMap(TreeMap)}
     *
     * Note: {@link ConcurrentSkipListMap} is not the state of the art
     * concurrent sorted dictionary implementation.
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return predicate
     */
    @Advice("Use non-blocking ConcurrentSkipListMap instead of Collections.synchronizedMap(TreeMap)")
    public Predicate<Reference> use_ConcurrentSkipListMap_instead_of_synchronized_TreeMap() {
        return and(
                targetType(is(SYNCHRONIZED_MAP)),
                underlyingMap(is(TreeMap.class)),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * Use non-blocking {@link ConcurrentSkipListSet} instead of {@code
     * Collections.synchronizedMap(TreeSet)}
     *
     * See: https://github.com/code-review-checklists/java-concurrency#non-blocking-collections
     *
     * @return predicate
     */
    @Advice("Use non-blocking ConcurrentSkipListSet instead of Collections.synchronizedMap(TreeSet)")
    public Predicate<Reference> use_ConcurrentSkipListSet_instead_of_syncronized_TreeSet() {
        return and(
                targetType(is(SYNCHRONIZED_SET)),
                underlyingCollection(is(TreeSet.class)),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * Consider using {@link ClassValue} instead of {@code Map<Class, ...>}
     *
     * Note, however, that unlike {@link ConcurrentHashMap} with its {@code
     * computeIfAbsent()} method {@link ClassValue} doesn’t guarantee that
     * per-class value is computed only once, i. e. {@code
     * ClassValue.computeValue()} might be executed by multiple concurrent
     * threads. So if the computation inside {@code computeValue()} is not
     * thread-safe, it should be synchronized separately. On the other hand,
     * {@link ClassValue} does guarantee that the same value is always returned
     * from {@code ClassValue.get()} (unless {@code remove()} is called).
     * <p>
     * See: https://github.com/code-review-checklists/java-concurrency#use-class-value
     *
     * @return predicate
     */
    @Suggestion("Consider using ClassValue instead of Map<Class, ...>")
    public Predicate<Reference> use_ClassValue_intead_of_Map_of_Class() {
        return and(
                field(
                        isStatic(),
                        type(is(Map.class)),
                        genericType(
                                0,
                                is(Class.class)
                        ),
                        genericType(
                                1,
                                clazz -> getScanner().matchesFilter(clazz)
                        )
                ),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * Use {@link EnumMap} intead of {@code Map<Enum, ...>}
     *
     * @return predicate
     */
    @Advice("Use EnumMap instead of Map<Enum, ...>")
    public Predicate<Reference> use_EnumMap_intead_of_Map_of_Enum() {
        return and(
                field(
                        type(is(Map.class)),
                        genericType(
                                0,
                                isEnum()
                        )
                ),
                targetType(
                        isNot(EnumMap.class),
                        isNot(THREAD_SAFE_COLLECTIONS),
                        isNot(UNMODIFIABLE_COLLECTIONS)
                ),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * Use {@link EnumSet} intead of {@code Set<Enum>}
     *
     * @return predicate
     */
    @Advice("Use EnumSet instead of Set<Enum>")
    public Predicate<Reference> use_EnumSet_instead_of_Set_of_Enum() {
        return and(
                field(
                        type(is(Set.class)),
                        genericType(
                                0,
                                isEnum()
                        )
                ),
                targetType(
                        isNot(EnumSet.class),
                        isNot(THREAD_SAFE_COLLECTIONS),
                        isNot(UNMODIFIABLE_COLLECTIONS)
                ),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * Exposing mutable collection allows the state of the shared object to be
     * mutated externally.
     *
     * @return predicate
     */
    @Warning(
            "Mutable collection exposed via non-private field or non-private getter")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> exposed_mutable_collection() {
        return and(
                ownerType(isNotPrivateClass()),
                targetType(
                        is(
                                Collection.class,
                                Map.class
                        ),
                        isNot(UNMODIFIABLE_COLLECTIONS)
                ),
                fieldIsExposedForReading(),
                field(type(isNot(UNMODIFIABLE_COLLECTIONS))),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                )
        );
    }

    /**
     * Using non-thread-safe mutable collections in a shared environment can
     * cause hard to track race conditions and spontaneous {@link
     * ConcurrentModificationException} exceptions
     *
     * @return predicate
     */
    @ProbableError("Non-thread-safe mutable collection is used")
    @Scopes({"static", "singleton", "session"})
    public Predicate<Reference> unsafe_mutable_collection() {
        return and(
                ownerType(isNotAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")),
                or(
                        targetType(
                                is(
                                        Map.class,
                                        Collection.class
                                ),
                                isNot(THREAD_SAFE_COLLECTIONS),
                                isNot(UNMODIFIABLE_COLLECTIONS),
                                isNot(SET_FROM_MAP)
                        ),
                        and(
                                targetType(is(SET_FROM_MAP)),
                                underlyingMap(isNot(THREAD_SAFE_COLLECTIONS)
                                                      .and(isNot(UNMODIFIABLE_COLLECTIONS))
                                                      .and(isNot(SET_FROM_MAP)))
                        )
                ),
                field(isNotAnnotatedWith("org.springframework.beans.factory.annotation.Value")),
                target(Objects::nonNull),
                referenceTypeIs(
                        ReferenceType.ACTUAL_VALUE,
                        ReferenceType.POSSIBLE_VALUE
                ),
                exposedOrModified()
        );
    }

    private Predicate<Reference> exposedOrModified() {
        return or(
                fieldIsExposedForReading(),
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

}
