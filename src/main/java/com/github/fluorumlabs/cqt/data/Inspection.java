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

import com.github.fluorumlabs.cqt.annotations.Level;

import java.util.function.Predicate;

/**
 * Inspection definition
 */
public class Inspection {

    private final String category;

    private final String id;

    private final Level level;

    private final String message;

    private final Predicate<Reference> predicate;

    /**
     * Instantiate a new Inspection.
     *
     * @param level     the severity level (see {@link Level})
     * @param predicate the predicate
     * @param category  the category
     * @param message   the message
     * @param id        the id
     */
    public Inspection(Level level,
                      Predicate<Reference> predicate,
                      String category,
                      String message,
                      String id) {
        this.level     = level;
        this.predicate = predicate;
        this.message   = message;
        this.category  = category;
        this.id        = id;
    }

    /**
     * Get category.
     *
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Get id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Get severity level.
     *
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Get message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get predicate.
     *
     * @return the predicate
     */
    public Predicate<Reference> getPredicate() {
        return predicate;
    }

}
