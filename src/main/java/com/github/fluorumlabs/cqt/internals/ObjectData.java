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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Object data associated with a particulat object instance.
 */
public class ObjectData {

    private final List<ObjectValue> objectValues;

    @Nullable
    private final String scope;

    @Nullable
    private String inheritedScope;

    /**
     * Instantiates a new object data.
     *
     * @param scope the scope
     */
    ObjectData(@Nullable String scope) {
        this.scope        = scope;
        this.objectValues = new ArrayList<>();
    }

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

}
