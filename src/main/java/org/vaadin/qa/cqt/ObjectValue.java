package org.vaadin.qa.cqt;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Created by Artem Godin on 9/23/2020.
 */
public class ObjectValue {
    private final ReferenceType referenceType;
    @Nullable
    private final Field field;
    private final Object value;

    ObjectValue(ReferenceType referenceType, Object value) {
        this.referenceType = referenceType;
        this.field = null;
        this.value = value;
    }

    ObjectValue(ReferenceType referenceType, Field field, Object value) {
        this.referenceType = referenceType;
        this.field = field;
        this.value = value;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    @Nullable
    public Field getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }
}
