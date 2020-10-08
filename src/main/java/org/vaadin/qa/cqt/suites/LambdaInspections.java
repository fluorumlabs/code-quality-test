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

package org.vaadin.qa.cqt.suites;

import org.vaadin.qa.cqt.Suite;
import org.vaadin.qa.cqt.annotations.Scopes;
import org.vaadin.qa.cqt.annotations.Warning;
import org.vaadin.qa.cqt.data.Reference;
import org.vaadin.qa.cqt.utils.Classes;

import java.util.function.Predicate;

/**
 * Created by Artem Godin on 9/21/2020.
 */
@SuppressWarnings("unchecked")
public final class LambdaInspections extends Suite {

    /**
     * Capturing short-lived objects in a closure of session-scoped lambdas will
     * extend their lifetime and, potentially may result in memory leaks.
     *
     * @return predicate
     */
    @Warning("Narrow-scoped object captured in effectively session lambda")
    @Scopes("session")
    public Predicate<Reference> captured_narrow_scope_in_session_lambda() {
        return and(
                field(
                        declaringClass(isLambda()),
                        type(isNot(Classes.BOXED_PRIMITIVE_OR_STRING))
                ),
                targetType(isNot("org.springframework.beans.factory.config.Scope")),
                backreference(isNotInScope(
                        "static",
                        "singleton",
                        "session",
                        "instance"
                ))
        );
    }

    /**
     * Capturing short-lived objects in a closure of static- or singleton-scoped
     * lambdas will extend their lifetime and, potentially may result in memory
     * leaks.
     *
     * @return predicate
     */
    @Warning(
            "Narrow-scoped object captured in effectively static or singleton lambda")
    @Scopes(exclude = {"static", "singleton", "instance"})
    public Predicate<Reference> captured_narrow_scope_in_singleton_lambda() {
        return and(
                field(
                        declaringClass(isLambda()),
                        type(isNot(Classes.BOXED_PRIMITIVE_OR_STRING))
                ),
                targetType(isNot("org.springframework.beans.factory.config.Scope")),
                backreference(isNotInScope(
                        "static",
                        "singleton",
                        "instance"
                ))
        );
    }

}
