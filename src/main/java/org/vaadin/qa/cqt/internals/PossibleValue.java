package org.vaadin.qa.cqt.internals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Possible value of object
 */
public class PossibleValue {
    private final Class<?> type;
    private final Class<?> owner;
    private final List<String> methods;

    /**
     * Instantiates a new possible value.
     *
     * @param type  the possible field value type
     * @param owner the owner object class
     */
    public PossibleValue(Class<?> type, Class<?> owner) {
        this.type = type;
        this.owner = owner;
        this.methods = new ArrayList<>();
    }

    /**
     * Get object type.
     *
     * @return the type
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Get object owner.
     *
     * @return the owner
     */
    public Class<?> getOwner() {
        return owner;
    }

    /**
     * Add method that can write object of specific type to the field.
     *
     * @param method the method
     */
    public void addMethod(String method) {
        methods.add(method);
    }

    /**
     * Get list of methods that can write object of specific type to the field.
     *
     * @return the methods
     */
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PossibleValue that = (PossibleValue) o;
        return type.equals(that.type) &&
                owner.equals(that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, owner);
    }
}
