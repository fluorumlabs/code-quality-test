package org.vaadin.qa.cqt;

import org.vaadin.qa.cqt.annotations.Disabled;
import org.vaadin.qa.cqt.annotations.Report;
import org.vaadin.qa.cqt.annotations.Scopes;
import org.vaadin.qa.cqt.predicates.AnnotatedElementPredicates;
import org.vaadin.qa.cqt.predicates.FieldPredicates;
import org.vaadin.qa.cqt.predicates.MemberPredicates;
import org.vaadin.qa.cqt.predicates.TypePredicates;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by Artem Godin on 9/24/2020.
 */
public class Suite implements AnnotatedElementPredicates, FieldPredicates, TypePredicates, MemberPredicates {
    private Scanner scanner;

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
                    .filter(annotation -> annotation.annotationType().getAnnotation(Report.class) != null)
                    .findFirst();
            report.ifPresent(annotation -> {
                Report reportAnnotation = annotation.annotationType().getAnnotation(Report.class);
                Scopes scopes = method.getAnnotation(Scopes.class);
                Predicate<Reference> preFilter = null;

                if (scopes != null && scopes.value().length > 0) {
                    Optional<Predicate<Reference>> filter = Stream.of(scopes.value())
                            .<Predicate<Reference>>map(scope -> (Reference reference) -> scope.equals(reference.getScope()))
                            .reduce(Predicate::or);
                    preFilter = filter.get();
                }

                if (scopes != null && scopes.exclude().length > 0) {
                    Optional<Predicate<Reference>> filter = Stream.of(scopes.exclude())
                            .<Predicate<Reference>>map(scope -> (Reference reference) -> !scope.equals(reference.getScope()))
                            .reduce(Predicate::and);
                    if (preFilter == null) {
                        preFilter = filter.get();
                    } else {
                        preFilter = preFilter.and(filter.get());
                    }
                }

                try {
                    Predicate<Reference> predicate = (Predicate<Reference>) method.invoke(this);
                    if (preFilter != null) {
                        predicate = preFilter.and(predicate);
                    }
                    String message = (String) annotation.annotationType().getMethod("value").invoke(annotation);
                    inspections.add(new Inspection(reportAnnotation.level(), predicate, reportAnnotation.name(), message, getClass().getSimpleName() + "." + method.getName()));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalStateException("Cannot import test suite method", e);
                }
            });
        }
        return inspections;
    }

    public Scanner getScanner() {
        return scanner;
    }

    // Helpers
    public Predicate<Reference> and(Predicate<Reference>... predicates) {
        return PredicateUtils.and(predicates);
    }

    public Predicate<Reference> or(Predicate<Reference>... predicates) {
        return PredicateUtils.or(predicates);
    }

    public Predicate<Reference> referenceTypeIs(ReferenceType first) {
        return reference -> EnumSet.of(first).contains(reference.getReferenceType());
    }

    public Predicate<Reference> referenceTypeIsNot(ReferenceType first) {
        return reference -> !EnumSet.of(first).contains(reference.getReferenceType());
    }

    public Predicate<Reference> referenceTypeIs(ReferenceType first, ReferenceType... referenceTypes) {
        return reference -> EnumSet.of(first, referenceTypes).contains(reference.getReferenceType());
    }

    public Predicate<Reference> referenceTypeIsNot(ReferenceType first, ReferenceType... referenceTypes) {
        return reference -> !EnumSet.of(first, referenceTypes).contains(reference.getReferenceType());
    }

    public Predicate<Reference> field(Predicate<Field>... rules) {
        return field(PredicateUtils.and(rules));
    }

    public Predicate<Reference> field(Predicate<Field> rule) {
        return ref -> ref.getField() != null && rule.test(ref.getField());
    }

    public Predicate<Reference> target(Predicate<Object>... rules) {
        return target(PredicateUtils.and(rules));
    }

    public Predicate<Reference> target(Predicate<Object> rule) {
        return reference -> rule.test(reference.getTarget());
    }

    public Predicate<Reference> owner(Predicate<Object>... rules) {
        return owner(PredicateUtils.and(rules));
    }

    public Predicate<Reference> owner(Predicate<Object> rule) {
        return reference -> rule.test(reference.getOwner());
    }

    public Predicate<Reference> targetType(Predicate<Class<?>>... rules) {
        return targetType(PredicateUtils.and(rules));
    }

    public Predicate<Reference> targetType(Predicate<Class<?>> rule) {
        return reference -> reference.getTargetClass() != null && rule.test(reference.getTargetClass());
    }

    public Predicate<Reference> ownerType(Predicate<Class<?>>... rules) {
        return ownerType(PredicateUtils.and(rules));
    }

    public Predicate<Reference> ownerType(Predicate<Class<?>> rule) {
        return reference -> reference.getOwnerClass() != null && rule.test(reference.getOwnerClass());
    }

    public Predicate<Reference> isField() {
        return ref -> ref.getField() != null;
    }

    public Predicate<Reference> isNotField() {
        return ref -> ref.getField() == null;
    }

    public Predicate<Reference> isInContext(String... scopes) {
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

    public Predicate<Reference> isNotInContext(String... scopes) {
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

    public Predicate<Field> isReadInOtherClasses() {
        return ExposedMembers::isFieldExposedForReading;
    }

    public Predicate<Field> isUpdatedInOtherClasses() {
        return ExposedMembers::isFieldExposedForWriting;
    }

    public Predicate<Method> isCalledFromOtherClasses() {
        return ExposedMembers::isMethodExposed;
    }

    public Predicate<Reference> fieldIsExposedForReading() {
        return field(
                isReadInOtherClasses().or(isPublic()).or(isProtected())
        ).or(
                fieldIsExposedViaGetter().and(
                        field(getter(
                                isCalledFromOtherClasses().or(isPublic()).or(isProtected())
                                )
                        )
                )
        );
    }

    public Predicate<Reference> fieldIsExposedForWriting() {
        return field(
                isUpdatedInOtherClasses().or(isPublic()).or(isProtected())
        ).or(
                field(setter(
                        isCalledFromOtherClasses().or(isPublic()).or(isProtected())
                        )
                )
        );
    }

    public Predicate<Reference> fieldIsExposedViaGetter() {
        return reference -> {
            if (reference.getField() == null || reference.getOwner() == null) {
                return false;
            }
            return field(getter(method -> {
                try {
                    method.setAccessible(true);
                    return method.invoke(reference.getOwner()) == reference.getTarget();
                } catch (IllegalAccessException | InvocationTargetException e) {
                    return false;
                }
            })).test(reference);
        };
    }

    public Predicate<Field> calledByNonClassInit(String... methodNames) {
        return field -> new CallFinder(field, Arrays.asList(methodNames)).calledByNot("<clinit>");
    }

    public Predicate<Field> calledByNonConstructor(String... methodNames) {
        return field -> new CallFinder(field, Arrays.asList(methodNames)).calledByNot("<init>");
    }

    public Predicate<Field> modifiedByNonClassInit() {
        return field -> new ModificationFinder(field).modifiedByNot("<clinit>");
    }

    public Predicate<Field> modifiedByNonConstructor() {
        return field -> new ModificationFinder(field).modifiedByNot("<init>");
    }

    public Predicate<Reference> backreference(Predicate<Reference> referencePredicate) {
        return reference -> scanner.getBackreferences(reference.getOwner()).stream().anyMatch(referencePredicate);
    }

    public Predicate<Reference> backreference(Predicate<Reference>... referencePredicates) {
        return reference -> scanner.getBackreferences(reference.getOwner()).stream().anyMatch(and(referencePredicates));
    }

    public Predicate<Reference> targetBackreference(Predicate<Reference> referencePredicate) {
        return reference -> reference.getTarget() != null && scanner.getBackreferences(reference.getTarget()).stream().anyMatch(referencePredicate);
    }

    public Predicate<Reference> targetBackreference(Predicate<Reference>... referencePredicates) {
        return reference -> reference.getTarget() != null && scanner.getBackreferences(reference.getTarget()).stream().anyMatch(and(referencePredicates));
    }

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
}
