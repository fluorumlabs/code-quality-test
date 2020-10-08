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

package com.github.fluorumlabs.cqt.data;

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
     * The Thread local value, but thread is in {@link Thread.State#WAITING}
     * state.
     */
    WAITING_THREAD_LOCAL("<thread local (waiting)>"),
    /**
     * The Thread local value, but thread is in {@link Thread.State#TERMINATED}
     * state.
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
