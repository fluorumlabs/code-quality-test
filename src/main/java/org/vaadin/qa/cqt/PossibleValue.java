package org.vaadin.qa.cqt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Artem Godin on 10/5/2020.
 */
public class PossibleValue {
    private final Class<?> type;
    private final Class<?> owner;
    private final List<String> methods;

    public PossibleValue(Class<?> type, Class<?> owner) {
        this.type = type;
        this.owner = owner;
        this.methods = new ArrayList<>();
    }

    public Class<?> getType() {
        return type;
    }

    public Class<?> getOwner() {
        return owner;
    }

    public void addMethod(String method) {
        methods.add(method);
    }

    public List<String> getMethods() {
        return methods;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PossibleValue that = (PossibleValue) o;
        return type.equals(that.type) &&
                owner.equals(that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, owner);
    }
}
