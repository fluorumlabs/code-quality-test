package org.vaadin.qa.cqt;

/**
 * Created by Artem Godin on 9/28/2020.
 */
public enum ReferenceType {
    ACTUAL_VALUE(""),
    THREAD_LOCAL("<thread local>"),
    WAITING_THREAD_LOCAL("<thread local (waiting)>"),
    TERMINATED_THREAD_LOCAL("<thread local (terminated)>"),
    OPTIONAL_VALUE("<value>"),
    REFERENCE_VALUE("<value>"),
    ATOMIC_REFERENCE_VALUE("<value>"),
    ARRAY_ITEM("[]"),
    COLLECTION_ITEM("<item>"),
    MAP_KEY("<map key>"),
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
