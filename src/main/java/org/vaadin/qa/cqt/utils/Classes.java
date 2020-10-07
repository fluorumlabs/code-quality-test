package org.vaadin.qa.cqt.utils;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.*;

/**
 * Classes helpers.
 */
public final class Classes {

    /**
     * Boxed primitives or String.
     */
    public static final Class<?>[] BOXED_PRIMITIVE_OR_STRING = {
            Character.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            Boolean.class,
            String.class
    };

    /**
     * Class of Collections.emptyList().
     */
    public static final Class<?> EMPTY_LIST = Collections
            .emptyList()
            .getClass();

    /**
     * Class of Collections.emptyMap().
     */
    public static final Class<?> EMPTY_MAP = Collections.emptyMap().getClass();

    /**
     * Class of Collections.emptyNavigableMap().
     */
    public static final Class<?> EMPTY_NAVIGABLE_MAP = Collections
            .emptyNavigableMap()
            .getClass();

    /**
     * Class of Collections.emptyNavigableSet().
     */
    public static final Class<?> EMPTY_NAVIGABLE_SET = Collections
            .emptyNavigableSet()
            .getClass();

    /**
     * Class of Collections.emptySet().
     */
    public static final Class<?> EMPTY_SET = Collections.emptySet().getClass();

    /**
     * Class of Collections.emptySortedMap().
     */
    public static final Class<?> EMPTY_SORTED_MAP = Collections
            .emptySortedMap()
            .getClass();

    /**
     * Class of Collections.emptySortedSet().
     */
    public static final Class<?> EMPTY_SORTED_SET = Collections
            .emptySortedSet()
            .getClass();

    /**
     * Class of Collections.newSetFromMap().
     */
    public static final Class<?> SET_FROM_MAP = getInnerClass(Collections.class,
                                                              "SetFromMap"
    );

    /**
     * Class of Collections.singleton().
     */
    public static final Class<?> SINGLETON = Collections
            .singleton(DUMMY_ENUM.WHATEVER)
            .getClass();

    /**
     * Class of Collections.singletonList().
     */
    public static final Class<?> SINGLETON_LIST = Collections
            .singletonList(DUMMY_ENUM.WHATEVER)
            .getClass();

    /**
     * Class of Collections.singletonMap().
     */
    public static final Class<?> SINGLETON_MAP = Collections
            .singletonMap(DUMMY_ENUM.WHATEVER, DUMMY_ENUM.WHATEVER)
            .getClass();

    /**
     * Collections.SynchronizedCollection.class.
     */
    public static final Class<?> SYNCHRONIZED_COLLECTION = getInnerClass(Collections.class,
                                                                         "SynchronizedCollection"
    );

    /**
     * Collections.SynchronizedList.class.
     */
    public static final Class<?> SYNCHRONIZED_LIST
            = getInnerClass(Collections.class, "SynchronizedList");

    /**
     * Collections.SynchronizedMap.class.
     */
    public static final Class<?> SYNCHRONIZED_MAP
            = getInnerClass(Collections.class, "SynchronizedMap");

    /**
     * Collections.SynchronizedNavigableMap.class.
     */
    public static final Class<?> SYNCHRONIZED_NAVIGABLE_MAP = getInnerClass(Collections.class,
                                                                            "SynchronizedNavigableMap"
    );

    /**
     * Collections.SynchronizedNavigableSet.class.
     */
    public static final Class<?> SYNCHRONIZED_NAVIGABLE_SET = getInnerClass(Collections.class,
                                                                            "SynchronizedNavigableSet"
    );

    /**
     * Collections.SynchronizedSet.class.
     */
    public static final Class<?> SYNCHRONIZED_SET
            = getInnerClass(Collections.class, "SynchronizedSet");

    /**
     * Collections.SynchronizedSortedMap.class.
     */
    public static final Class<?> SYNCHRONIZED_SORTED_MAP = getInnerClass(Collections.class,
                                                                         "SynchronizedSortedMap"
    );

    /**
     * Collections.SynchronizedSortedSet.class.
     */
    public static final Class<?> SYNCHRONIZED_SORTED_SET = getInnerClass(Collections.class,
                                                                         "SynchronizedSortedSet"
    );

    /**
     * Thread safe collections.
     */
    public static final Class<?>[] THREAD_SAFE_COLLECTIONS = {
            Properties.class,
            Hashtable.class,
            Vector.class,
            ConcurrentHashMap.class,
            ConcurrentLinkedQueue.class,
            ConcurrentLinkedDeque.class,
            ConcurrentSkipListMap.class,
            ConcurrentSkipListSet.class,
            ConcurrentMap.class,
            ConcurrentNavigableMap.class,
            LinkedBlockingQueue.class,
            LinkedBlockingDeque.class,
            CopyOnWriteArraySet.class,
            CopyOnWriteArrayList.class,
            SYNCHRONIZED_COLLECTION,
            SYNCHRONIZED_MAP,
            };

    /**
     * Collections.UnmodifiableCollection.class
     */
    public static final Class<?> UNMODIFIABLE_COLLECTION = getInnerClass(Collections.class,
                                                                         "UnmodifiableCollection"
    );

    /**
     * Collections.UnmodifiableList.class.
     */
    public static final Class<?> UNMODIFIABLE_LIST
            = getInnerClass(Collections.class, "UnmodifiableList");

    /**
     * Collections.UnmodifiableMap.class.
     */
    public static final Class<?> UNMODIFIABLE_MAP
            = getInnerClass(Collections.class, "UnmodifiableMap");

    /**
     * Collections.UnmodifiableNavigableMap.class.
     */
    public static final Class<?> UNMODIFIABLE_NAVIGABLE_MAP = getInnerClass(Collections.class,
                                                                            "UnmodifiableNavigableMap"
    );

    /**
     * Collections.UnmodifiableNavigableSet.class.
     */
    public static final Class<?> UNMODIFIABLE_NAVIGABLE_SET = getInnerClass(Collections.class,
                                                                            "UnmodifiableNavigableSet"
    );

    /**
     * Collections.UnmodifiableSet.class.
     */
    public static final Class<?> UNMODIFIABLE_SET
            = getInnerClass(Collections.class, "UnmodifiableSet");

    /**
     * Unmodifiable collections.
     */
    public static final Class<?>[] UNMODIFIABLE_COLLECTIONS = {
            Iterable.class,
            EMPTY_LIST,
            EMPTY_SET,
            EMPTY_SORTED_SET,
            EMPTY_NAVIGABLE_SET,
            EMPTY_MAP,
            EMPTY_SORTED_MAP,
            EMPTY_NAVIGABLE_MAP,
            UNMODIFIABLE_COLLECTION,
            UNMODIFIABLE_SET,
            UNMODIFIABLE_MAP,
            SINGLETON,
            SINGLETON_LIST,
            SINGLETON_MAP
    };

    /**
     * Collections.UnmodifiableSortedMap.class.
     */
    public static final Class<?> UNMODIFIABLE_SORTED_MAP = getInnerClass(Collections.class,
                                                                         "UnmodifiableSortedMap"
    );

    /**
     * Collections.UnmodifiableSortedSet.class.
     */
    public static final Class<?> UNMODIFIABLE_SORTED_SET = getInnerClass(Collections.class,
                                                                         "UnmodifiableSortedSet"
    );

    private static Class<?> getInnerClass(Class<?> outer, String name) {
        for (Class<?> declaredClass : outer.getDeclaredClasses()) {
            if (name.equals(declaredClass.getSimpleName())) {
                return declaredClass;
            }
        }
        throw new IllegalArgumentException("Cannot find inner class "
                                                   + name
                                                   + " in "
                                                   + outer.getName());
    }


    private Classes() {
    }

    private enum DUMMY_ENUM {
        /**
         * Whatever dummy enum.
         */
        WHATEVER
    }

}
