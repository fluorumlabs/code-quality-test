package org.vaadin.qa.cqt.predicates;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

/**
 * Created by Artem Godin on 9/24/2020.
 */
public interface MemberPredicates {
    default <T extends Member> Predicate<T> isPublic() {
        return member -> Modifier.isPublic(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNotPublic() {
        return member -> !Modifier.isPublic(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isProtected() {
        return member -> Modifier.isProtected(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNotProtected() {
        return member -> !Modifier.isProtected(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isPrivate() {
        return member -> Modifier.isPrivate(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNotPrivate() {
        return member -> !Modifier.isPrivate(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isStatic() {
        return member -> Modifier.isStatic(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNotStatic() {
        return member -> !Modifier.isStatic(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isFinal() {
        return member -> Modifier.isFinal(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNotFinal() {
        return member -> !Modifier.isFinal(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isTransient() {
        return member -> Modifier.isTransient(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNotTransient() {
        return member -> !Modifier.isTransient(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isVolatile() {
        return member -> Modifier.isVolatile(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNotVolatile() {
        return member -> !Modifier.isVolatile(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isAbstract() {
        return member -> Modifier.isAbstract(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNotAbstract() {
        return member -> !Modifier.isAbstract(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isSynchronized() {
        return member -> Modifier.isSynchronized(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNotSynchronized() {
        return member -> !Modifier.isSynchronized(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNative() {
        return member -> Modifier.isNative(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNotNative() {
        return member -> !Modifier.isNative(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isStrict() {
        return member -> Modifier.isStrict(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isNotStrict() {
        return member -> !Modifier.isStrict(member.getModifiers());
    }

    default <T extends Member> Predicate<T> isSynthetic() {
        return Member::isSynthetic;
    }

    default <T extends Member> Predicate<T> isNotSynthetic() {
        return member -> !member.isSynthetic();
    }
}
