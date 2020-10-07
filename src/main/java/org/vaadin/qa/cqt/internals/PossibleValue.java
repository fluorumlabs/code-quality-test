package org.vaadin.qa.cqt.internals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Possible value of object
 */
public class PossibleValue {

    private final List<String> methods;

    private final Class<?> owner;

    private final Class<?> type;

    /**
     * Instantiates a new possible value.
     *
     * @param type  the possible field value type
     * @param owner the owner object class
     */
    public PossibleValue(Class<?> type, Class<?> owner) {
        this.type    = type;
        this.owner   = owner;
        this.methods = new ArrayList<>();
    }

    /**
     * Add method that can write object of specific type to the field.
     *
     * @param method the method
     */
    public void addMethod(String method) {
        methods.add(method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, owner);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PossibleValue that = (PossibleValue) o;
        return type.equals(that.type) && owner.equals(that.owner);
    }

    /**
     * Get list of methods that can write object of specific type to the field.
     *
     * @return the methods
     */
    public List<String> getMethods() {
        return methods;
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
     * Get object type.
     *
     * @return the type
     */
    public Class<?> getType() {
        return type;
    }

}
