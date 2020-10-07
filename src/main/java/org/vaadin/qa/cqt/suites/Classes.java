package org.vaadin.qa.cqt.suites;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.*;

/**
 * Created by Artem Godin on 9/25/2020.
 */
public final class Classes {
    private Classes() {}

    public static final Class<?> EMPTY_LIST = Collections.emptyList().getClass();
    public static final Class<?> EMPTY_SET = Collections.emptySet().getClass();
    public static final Class<?> EMPTY_SORTED_SET = Collections.emptySortedSet().getClass();
    public static final Class<?> EMPTY_NAVIGABLE_SET = Collections.emptyNavigableSet().getClass();
    public static final Class<?> EMPTY_MAP = Collections.emptyMap().getClass();
    public static final Class<?> EMPTY_SORTED_MAP = Collections.emptySortedMap().getClass();
    public static final Class<?> EMPTY_NAVIGABLE_MAP = Collections.emptyNavigableMap().getClass();

    public static final Class<?> UNMODIFIABLE_COLLECTION = getInnerClass(Collections.class, "UnmodifiableCollection");
    public static final Class<?> UNMODIFIABLE_LIST = getInnerClass(Collections.class, "UnmodifiableList");
    public static final Class<?> UNMODIFIABLE_SET = getInnerClass(Collections.class, "UnmodifiableSet");
    public static final Class<?> UNMODIFIABLE_SORTED_SET = getInnerClass(Collections.class, "UnmodifiableSortedSet");
    public static final Class<?> UNMODIFIABLE_NAVIGABLE_SET = getInnerClass(Collections.class, "UnmodifiableNavigableSet");
    public static final Class<?> UNMODIFIABLE_MAP = getInnerClass(Collections.class, "UnmodifiableMap");
    public static final Class<?> UNMODIFIABLE_SORTED_MAP = getInnerClass(Collections.class, "UnmodifiableSortedMap");
    public static final Class<?> UNMODIFIABLE_NAVIGABLE_MAP = getInnerClass(Collections.class, "UnmodifiableNavigableMap");

    public static final Class<?> SYNCHRONIZED_COLLECTION = getInnerClass(Collections.class, "SynchronizedCollection");
    public static final Class<?> SYNCHRONIZED_LIST = getInnerClass(Collections.class, "SynchronizedList");
    public static final Class<?> SYNCHRONIZED_SET = getInnerClass(Collections.class, "SynchronizedSet");
    public static final Class<?> SYNCHRONIZED_SORTED_SET = getInnerClass(Collections.class, "SynchronizedSortedSet");
    public static final Class<?> SYNCHRONIZED_NAVIGABLE_SET = getInnerClass(Collections.class, "SynchronizedNavigableSet");
    public static final Class<?> SYNCHRONIZED_MAP = getInnerClass(Collections.class, "SynchronizedMap");
    public static final Class<?> SYNCHRONIZED_SORTED_MAP = getInnerClass(Collections.class, "SynchronizedSortedMap");
    public static final Class<?> SYNCHRONIZED_NAVIGABLE_MAP = getInnerClass(Collections.class, "SynchronizedNavigableMap");
    public static final Class<?> SET_FROM_MAP = getInnerClass(Collections.class, "SetFromMap");

    public static final Class<?> SINGLETON = Collections.singleton(DUMMY_ENUM.WHATEVER).getClass();
    public static final Class<?> SINGLETON_LIST = Collections.singletonList(DUMMY_ENUM.WHATEVER).getClass();
    public static final Class<?> SINGLETON_MAP = Collections.singletonMap(DUMMY_ENUM.WHATEVER, DUMMY_ENUM.WHATEVER).getClass();

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


    private enum DUMMY_ENUM {WHATEVER}

    public static Class<?> getInnerClass(Class<?> outer, String name) {
        for (Class<?> declaredClass : outer.getDeclaredClasses()) {
            if (name.equals(declaredClass.getSimpleName())) {
                return declaredClass;
            }
        }
        throw new IllegalArgumentException("Cannot find inner class "+name+" in "+outer.getName());
    }
}
