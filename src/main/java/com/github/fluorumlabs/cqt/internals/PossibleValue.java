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
        return Objects.hash(
                type,
                owner
        );
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
