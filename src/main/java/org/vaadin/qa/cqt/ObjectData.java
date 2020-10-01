package org.vaadin.qa.cqt;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Artem Godin on 9/23/2020.
 */
public class ObjectData {
    @Nullable
    private final String scope;
    @Nullable
    private String inheritedScope;
    private final List<ObjectValue> objectValues;

    ObjectData(@Nullable String scope) {
        this.scope = scope;
        this.objectValues = new ArrayList<>();
    }

    public void addValue(ObjectValue objectValue) {
        objectValues.add(objectValue);
    }

    public boolean hasOwnScope() {
        return scope != null;
    }

    public String getScope() {
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

    public String getPrintableScope() {
        if (scope == null) {
            if (inheritedScope == null) {
                return "instance";
            } else {
                return "instance".equals(inheritedScope) ? "instance" : "effectively " + inheritedScope;
            }
        } else {
            return scope;
        }
    }

    public void setInheritedScope(@Nullable String scope) {
        this.inheritedScope = scope;
    }

    public String getInheritedScope() {
        return inheritedScope;
    }

    public List<ObjectValue> getValues() {
        return objectValues;
    }
}
