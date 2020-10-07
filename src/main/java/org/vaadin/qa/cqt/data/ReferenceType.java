package org.vaadin.qa.cqt.data;

import java.util.Optional;

/**
 * Reference type of owner-target relationship
 *
 * @see Reference
 */
public enum ReferenceType {
    /**
     * Actual field value.
     */
    ACTUAL_VALUE(""),
    /**
     * Possible field value detected from bytecode analysis.
     */
    POSSIBLE_VALUE(""),
    /**
     * The Thread local value.
     */
    THREAD_LOCAL("<thread local>"),
    /**
     * The Thread local value, but thread is in {@link Thread.State#WAITING} state.
     */
    WAITING_THREAD_LOCAL("<thread local (waiting)>"),
    /**
     * The Thread local value, but thread is in {@link Thread.State#TERMINATED} state.
     */
    TERMINATED_THREAD_LOCAL("<thread local (terminated)>"),
    /**
     * {@link Optional} value.
     */
    OPTIONAL_VALUE("<value>"),
    /**
     * {@link java.lang.ref.Reference} value.
     */
    REFERENCE_VALUE("<value>"),
    /**
     * {@link java.util.concurrent.atomic.AtomicReference} value
     */
    ATOMIC_REFERENCE_VALUE("<value>"),
    /**
     * Array item.
     */
    ARRAY_ITEM("[]"),
    /**
     * {@link java.util.Collection} item.
     */
    COLLECTION_ITEM("<item>"),
    /**
     * {@link java.util.Map} key.
     */
    MAP_KEY("<map key>"),
    /**
     * {@link java.util.Map} value.
     */
    MAP_VALUE("<map value>");

    private final String title;

    ReferenceType(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return title;
    }
}
