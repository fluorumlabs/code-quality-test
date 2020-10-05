package org.vaadin.qa.cqt.suites;

import org.vaadin.qa.cqt.Reference;
import org.vaadin.qa.cqt.Suite;
import org.vaadin.qa.cqt.annotations.Scopes;
import org.vaadin.qa.cqt.annotations.Warning;

import java.util.function.Predicate;

/**
 * Created by Artem Godin on 9/21/2020.
 */
@SuppressWarnings("unchecked")
public final class LambdaInspections extends Suite {

    @Warning("Narrow-scoped object captured in effectively static or singleton lambda")
    @Scopes(exclude = {"static", "singleton", "instance"})
    public Predicate<Reference> narrowScopeCapturedInStaticLambda() {
        return and(
                field(
                        declaringClass(isLambda()),
                        type(isNot(Classes.BOXED_PRIMITIVE_OR_STRING))
                ),
                targetType(isNot("org.springframework.beans.factory.config.Scope")),
                backreference(isNotInContext("static", "singleton", "instance"))
        );
    }

    @Warning("Narrow-scoped object captured in effectively session lambda")
    @Scopes("session")
    public Predicate<Reference> narrowScopeCapturedInSessionLambda() {
        return and(
                field(
                        declaringClass(isLambda()),
                        type(isNot(Classes.BOXED_PRIMITIVE_OR_STRING))
                ),
                targetType(isNot("org.springframework.beans.factory.config.Scope")),
                backreference(isNotInContext("static", "singleton", "session", "instance"))
        );
    }
}
