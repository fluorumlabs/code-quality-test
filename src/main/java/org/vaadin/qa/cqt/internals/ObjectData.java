package org.vaadin.qa.cqt.internals;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Object data associated with a particulat object instance.
 */
public class ObjectData {

    /**
     * Add value associated with this object (see {@link ObjectValue}.
     *
     * @param objectValue the related object value
     */
    public void addValue(ObjectValue objectValue) {
        objectValues.add(objectValue);
    }

    /**
     * Test if owner object has own (not inherited) scope defined.
     *
     * @return {@code true} if owner has own scope.
     */
    public boolean hasOwnScope() {
        return scope != null;
    }

    /**
     * Set inherited scope.
     *
     * @param scope the scope
     */
    public void setInheritedScope(@Nullable String scope) {
        this.inheritedScope = scope;
    }

    /**
     * Get effective scope (own or inherited).
     *
     * @return the scope
     */
    public String getEffectiveScope() {
        if (scope == null) {
            if (inheritedScope == null) {
                return "instance";
            } else {
                return inheritedScope;
            }
        } else {
            return scope;
        }
    }

    /**
     * Get printable scope (own or inherited).
     *
     * @return the printable scope
     */
    public String getPrintableScope() {
        if (scope == null) {
            if (inheritedScope == null) {
                return "instance";
            } else {
                return "instance".equals(inheritedScope)
                       ? "instance"
                       : "effectively " + inheritedScope;
            }
        } else {
            return scope;
        }
    }

    /**
     * Get associated values.
     *
     * @return the values
     */
    public List<ObjectValue> getValues() {
        return objectValues;
    }

    /**
     * Instantiates a new object data.
     *
     * @param scope the scope
     */
    ObjectData(@Nullable String scope) {
        this.scope        = scope;
        this.objectValues = new ArrayList<>();
    }

    private final List<ObjectValue> objectValues;

    @Nullable
    private final String scope;

    @Nullable
    private String inheritedScope;

}
