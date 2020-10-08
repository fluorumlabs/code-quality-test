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

package com.github.fluorumlabs.cqt.internals;

import com.github.fluorumlabs.cqt.data.ReferenceType;

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
