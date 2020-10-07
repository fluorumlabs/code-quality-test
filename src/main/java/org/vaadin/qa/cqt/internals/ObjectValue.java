package org.vaadin.qa.cqt.internals;

import org.vaadin.qa.cqt.data.ReferenceType;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Object value holds reference to other object (field, collection item etc.)
 */
public class ObjectValue {

    @Nullable
    private final Field field;

    private final ReferenceType referenceType;

    private final Object value;

    /**
     * Instantiate a new object value not linked to field
     *
     * @param referenceType the reference type
     * @param value         the value
     */
    ObjectValue(ReferenceType referenceType, Object value) {
        this.referenceType = referenceType;
        this.field         = null;
        this.value         = value;
    }

    /**
     * Instantiate a new object value linked to field
     *
     * @param referenceType the reference type
     * @param field         the field
     * @param value         the value
     */
    ObjectValue(ReferenceType referenceType, Field field, Object value) {
        this.referenceType = referenceType;
        this.field         = field;
        this.value         = value;
    }

    /**
     * Get field.
     *
     * @return the field
     */
    @Nullable
    public Field getField() {
        return field;
    }

    /**
     * Get reference type.
     *
     * @return the reference type
     */
    public ReferenceType getReferenceType() {
        return referenceType;
    }

    /**
     * Get value.
     *
     * @return the value
     */
    public Object getValue() {
        return value;
    }

}
