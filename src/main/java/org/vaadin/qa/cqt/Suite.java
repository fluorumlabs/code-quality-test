package org.vaadin.qa.cqt;

import org.vaadin.qa.cqt.annotations.Disabled;
import org.vaadin.qa.cqt.annotations.Report;
import org.vaadin.qa.cqt.annotations.Scopes;
import org.vaadin.qa.cqt.data.Inspection;
import org.vaadin.qa.cqt.data.Reference;
import org.vaadin.qa.cqt.data.ReferenceType;
import org.vaadin.qa.cqt.internals.CallFinder;
import org.vaadin.qa.cqt.internals.ExposedMembers;
import org.vaadin.qa.cqt.internals.ModificationFinder;
import org.vaadin.qa.cqt.internals.Scanner;
import org.vaadin.qa.cqt.predicates.AnnotatedElementPredicates;
import org.vaadin.qa.cqt.predicates.FieldPredicates;
import org.vaadin.qa.cqt.predicates.MemberPredicates;
import org.vaadin.qa.cqt.predicates.TypePredicates;
import org.vaadin.qa.cqt.utils.PredicateUtils;
import org.vaadin.qa.cqt.utils.Unreflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * All inspection suites must extend this class.
 */
public class Suite
        implements AnnotatedElementPredicates, FieldPredicates, TypePredicates, MemberPredicates {

    private Scanner scanner;

    /**
     * AND predicate.
     *
     * @param predicates the predicates
     * @return the predicate
     */
    public Predicate<Reference> and(Predicate<Reference>... predicates) {
        return PredicateUtils.and(predicates);
    }

    /**
     * Predicate testing if any of {@link Reference} owner object backreferences conform to rule.
     *
     * @param referencePredicate the reference predicate
     * @return the predicate
     */
    public Predicate<Reference> backreference(Predicate<Reference> referencePredicate) {
        return reference -> scanner.getBackreferences(reference.getOwner())
                .stream()
                .anyMatch(referencePredicate);
    }

    /**
     * Predicate testing if any of {@link Reference} owner object backreferences conform to rules.
     *
     * @param referencePredicates the reference predicates
     * @return the predicate
     */
    public Predicate<Reference> backreference(Predicate<Reference>... referencePredicates) {
        return reference -> scanner.getBackreferences(reference.getOwner())
                .stream()
                .anyMatch(and(referencePredicates));
    }

    /**
     * Predicate testing if {@link Field} methods are called from methods other than class initializer.
     *
     * @param methodNames the method names
     * @return the predicate
     */
    public Predicate<Field> calledByNonClassInit(String... methodNames) {
        return field -> new CallFinder(field,
                                       Arrays.asList(methodNames)
        ).calledByNot("<clinit>");
    }

    /**
     * Predicate testing if {@link Field} methods are called from methods other than class constructor.
     *
     * @param methodNames the method names
     * @return the predicate
     */
    public Predicate<Field> calledByNonConstructor(String... methodNames) {
        return field -> new CallFinder(field,
                                       Arrays.asList(methodNames)
        ).calledByNot("<init>");
    }

    /**
     * Predicate testing if {@link Reference} {@link Field} conforms to rules.
     *
     * @param rules the rules
     * @return the predicate
     */
    public Predicate<Reference> field(Predicate<Field>... rules) {
        return field(PredicateUtils.and(rules));
    }

    /**
     * Predicate testing if {@link Reference} {@link Field} conforms to rule.
     *
     * @param rule the rule
     * @return the predicate
     */
    public Predicate<Reference> field(Predicate<Field> rule) {
        return ref -> ref.getField() != null && rule.test(ref.getField());
    }

    /**
     * Predicate testing if {@link Reference} {@link Field} is exposed for reading.
     * <p>
     * This means that one of conditions is satisfied:
     * - Field can be read in other classes
     * - Field is public
     * - Field is protected
     * - There is a getter that returns field value and that getter can be called
     * in other classes or is public or protected
     *
     * @return the predicate
     */
    public Predicate<Reference> fieldIsExposedForReading() {
        return field(isReadInOtherClasses().or(isPublic())
                             .or(isProtected())).or(fieldIsExposedViaGetter().and(
                field(getter(isCalledFromOtherClasses().or(isPublic())
                                     .or(isProtected())))));
    }

    /**
     * Predicate testing if {@link Reference} {@link Field} is exposed for updating.
     * <p>
     * This means that one of conditions is satisfied:
     * - Field can be updated in other classes
     * - Field is public
     * - Field is protected
     * - There is a setter and that setter can be called
     * in other classes or is public or protected
     *
     * @return the predicate
     */
    public Predicate<Reference> fieldIsExposedForUpdating() {
        return field(isUpdatedInOtherClasses().or(isPublic()).or(isProtected()))
                .or(field(setter(isCalledFromOtherClasses().or(isPublic())
                                         .or(isProtected()))));
    }

    /**
     * Predicate testing if {@link Reference} there is a getter for {@link Field} that returns field value.
     *
     * @return the predicate
     */
    public Predicate<Reference> fieldIsExposedViaGetter() {
        return reference -> {
            if (reference.getField() == null || reference.getOwner() == null) {
                return false;
            }
            return field(getter(method -> {
                try {
                    method.setAccessible(true);
                    return method.invoke(reference.getOwner())
                            == reference.getTarget();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    return false;
                }
            })).test(reference);
        };
    }

    /**
     * Predicate testing if any of {@link Class} has a method.
     *
     * @param name the name
     * @param args the args
     * @return the predicate
     */
    public Predicate<Class<?>> hasMethod(String name, Class<?>... args) {
        return cz -> {
            try {
                Unreflection.getDeclaredMethod(cz, name, args);
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            }
        };
    }

    /**
     * Predicate testing if {@link Method} is can be called in other classes.
     *
     * @return the predicate
     */
    public Predicate<Method> isCalledFromOtherClasses() {
        return ExposedMembers::isMethodExposed;
    }

    /**
     * Predicate testing if {@link Reference} has field.
     *
     * @return the predicate
     */
    public Predicate<Reference> isField() {
        return ref -> ref.getField() != null;
    }

    /**
     * Predicate testing if {@link Reference} belongs to scopes.
     *
     * @param scopes the scopes
     * @return the predicate
     */
    public Predicate<Reference> isInScope(String... scopes) {
        return reference -> {
            String data = reference.getScope();
            for (String scope : scopes) {
                if (scope.equals(data)) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * Predicate testing if {@link Reference} has no field.
     *
     * @return the predicate
     */
    public Predicate<Reference> isNotField() {
        return ref -> ref.getField() == null;
    }

    /**
     * Predicate testing if {@link Reference} does not belong to scopes.
     *
     * @param scopes the scopes
     * @return the predicate
     */
    public Predicate<Reference> isNotInScope(String... scopes) {
        return reference -> {
            String data = reference.getScope();
            for (String scope : scopes) {
                if (scope.equals(data)) {
                    return false;
                }
            }
            return true;
        };
    }

    /**
     * Predicate testing if {@link Field} is can be read in other classes.
     *
     * @return the predicate
     */
    public Predicate<Field> isReadInOtherClasses() {
        return ExposedMembers::isFieldExposedForReading;
    }

    /**
     * Predicate testing if {@link Field} is can be updated in other classes.
     *
     * @return the predicate
     */
    public Predicate<Field> isUpdatedInOtherClasses() {
        return ExposedMembers::isFieldExposedForWriting;
    }

    /**
     * Predicate testing if {@link Field} is updated in methods other than class initializer.
     *
     * @return the predicate
     */
    public Predicate<Field> modifiedByNonClassInit() {
        return field -> new ModificationFinder(field).modifiedByNot("<clinit>");
    }

    /**
     * Predicate testing if {@link Field} is updated in methods other than class constructor.
     *
     * @return the predicate
     */
    public Predicate<Field> modifiedByNonConstructor() {
        return field -> new ModificationFinder(field).modifiedByNot("<init>");
    }

    /**
     * OR predicate.
     *
     * @param predicates the predicates
     * @return the predicate
     */
    public Predicate<Reference> or(Predicate<Reference>... predicates) {
        return PredicateUtils.or(predicates);
    }

    /**
     * Predicate testing if {@link Reference} owner object conforms to rules.
     *
     * @param rules the rules
     * @return the predicate
     */
    public Predicate<Reference> owner(Predicate<Object>... rules) {
        return owner(PredicateUtils.and(rules));
    }

    /**
     * Predicate testing if {@link Reference} owner object conforms to rule.
     *
     * @param rule the rule
     * @return the predicate
     */
    public Predicate<Reference> owner(Predicate<Object> rule) {
        return reference -> rule.test(reference.getOwner());
    }

    /**
     * Predicate testing if {@link Reference} owner class conforms to rules.
     *
     * @param rules the rules
     * @return the predicate
     */
    public Predicate<Reference> ownerType(Predicate<Class<?>>... rules) {
        return ownerType(PredicateUtils.and(rules));
    }

    /**
     * Predicate testing if {@link Reference} owner class conforms to rule.
     *
     * @param rule the rule
     * @return the predicate
     */
    public Predicate<Reference> ownerType(Predicate<Class<?>> rule) {
        return reference -> reference.getOwnerClass() != null && rule.test(
                reference.getOwnerClass());
    }

    /**
     * Predicate testing if {@link Reference} has specified {@link ReferenceType}.
     *
     * @param first the first
     * @return the predicate
     */
    public Predicate<Reference> referenceTypeIs(ReferenceType first) {
        return reference -> EnumSet.of(first)
                .contains(reference.getReferenceType());
    }

    /**
     * Predicate testing if {@link Reference} has one of specified {@link ReferenceType}.
     *
     * @param first          the first
     * @param referenceTypes the reference types
     * @return the predicate
     */
    public Predicate<Reference> referenceTypeIs(ReferenceType first,
                                                ReferenceType... referenceTypes) {
        return reference -> EnumSet.of(first, referenceTypes)
                .contains(reference.getReferenceType());
    }

    /**
     * Predicate testing if {@link Reference} has not specified {@link ReferenceType}.
     *
     * @param first the first
     * @return the predicate
     */
    public Predicate<Reference> referenceTypeIsNot(ReferenceType first) {
        return reference -> !EnumSet.of(first)
                .contains(reference.getReferenceType());
    }

    /**
     * Predicate testing if {@link Reference} has not one of specified {@link ReferenceType}.
     *
     * @param first          the first
     * @param referenceTypes the reference types
     * @return the predicate
     */
    public Predicate<Reference> referenceTypeIsNot(ReferenceType first,
                                                   ReferenceType... referenceTypes) {
        return reference -> !EnumSet.of(first, referenceTypes)
                .contains(reference.getReferenceType());
    }

    /**
     * Register inspection suite in scanner.
     *
     * @param scanner the scanner
     * @return the list
     */
    @SuppressWarnings("unchecked")
    public List<Inspection> register(Scanner scanner) {
        this.scanner = scanner;

        List<Inspection> inspections = new ArrayList<>();

        for (Method method : getClass().getMethods()) {
            if (method.getParameterCount() > 0
                    || !Predicate.class.isAssignableFrom(method.getReturnType())
                    || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getAnnotation(Disabled.class) != null) {
                continue;
            }
            Optional<Annotation> report = Arrays.stream(method.getDeclaredAnnotations())
                    .filter(annotation -> annotation.annotationType()
                            .getAnnotation(Report.class) != null)
                    .findFirst();
            report.ifPresent(annotation -> {
                Report reportAnnotation = annotation.annotationType()
                        .getAnnotation(Report.class);
                Scopes               scopes    = method.getAnnotation(Scopes.class);
                Predicate<Reference> preFilter = null;

                if (scopes != null && scopes.value().length > 0) {
                    Optional<Predicate<Reference>> filter = Stream.of(scopes.value()).<Predicate<Reference>>map(
                            scope -> (Reference reference) -> scope.equals(
                                    reference.getScope())).reduce(Predicate::or);
                    preFilter = filter.get();
                }

                if (scopes != null && scopes.exclude().length > 0) {
                    Optional<Predicate<Reference>> filter = Stream.of(scopes.exclude()).<Predicate<Reference>>map(
                            scope -> (Reference reference) -> !scope.equals(
                                    reference.getScope())).reduce(Predicate::and);
                    if (preFilter == null) {
                        preFilter = filter.get();
                    } else {
                        preFilter = preFilter.and(filter.get());
                    }
                }

                try {
                    Predicate<Reference> predicate = (Predicate<Reference>) method
                            .invoke(this);
                    if (preFilter != null) {
                        predicate = preFilter.and(predicate);
                    }
                    String message = (String) annotation.annotationType()
                            .getMethod("value")
                            .invoke(annotation);
                    inspections.add(new Inspection(reportAnnotation.level(),
                                                   predicate,
                                                   reportAnnotation.name(),
                                                   message,
                                                   getClass().getSimpleName()
                                                           + "."
                                                           + method.getName()
                    ));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalStateException("Cannot import test suite method",
                                                    e
                    );
                }
            });
        }
        return inspections;
    }

    /**
     * Predicate testing if {@link Reference} target object conforms to rules.
     *
     * @param rules the rules
     * @return the predicate
     */
    public Predicate<Reference> target(Predicate<Object>... rules) {
        return target(PredicateUtils.and(rules));
    }

    /**
     * Predicate testing if {@link Reference} target object conforms to rule.
     *
     * @param rule the rule
     * @return the predicate
     */
    public Predicate<Reference> target(Predicate<Object> rule) {
        return reference -> rule.test(reference.getTarget());
    }

    /**
     * Predicate testing if any of {@link Reference} target object backreferences conform to rule.
     *
     * @param referencePredicate the reference predicate
     * @return the predicate
     */
    public Predicate<Reference> targetBackreference(Predicate<Reference> referencePredicate) {
        return reference -> reference.getTarget() != null
                && scanner.getBackreferences(reference.getTarget())
                .stream()
                .anyMatch(referencePredicate);
    }

    /**
     * Predicate testing if any of {@link Reference} target object backreferences conform to rules.
     *
     * @param referencePredicates the reference predicates
     * @return the predicate
     */
    public Predicate<Reference> targetBackreference(Predicate<Reference>... referencePredicates) {
        return reference -> reference.getTarget() != null
                && scanner.getBackreferences(reference.getTarget())
                .stream()
                .anyMatch(and(referencePredicates));
    }

    /**
     * Predicate testing if {@link Reference} target class conforms to rules.
     *
     * @param rules the rules
     * @return the predicate
     */
    public Predicate<Reference> targetType(Predicate<Class<?>>... rules) {
        return targetType(PredicateUtils.and(rules));
    }

    /**
     * Predicate testing if {@link Reference} target class conforms to rule.
     *
     * @param rule the rule
     * @return the predicate
     */
    public Predicate<Reference> targetType(Predicate<Class<?>> rule) {
        return reference -> reference.getTargetClass() != null && rule.test(
                reference.getTargetClass());
    }

    /**
     * Gets scanner.
     *
     * @return the scanner
     */
    public Scanner getScanner() {
        return scanner;
    }

}
