/*
 *  Copyright 2012 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.model;

import java.util.*;

/**
 *
 * @author Alexey Andreev
 */
public class ClassHolder extends ElementHolder {
    private String parent = Object.class.getName();
    private Set<String> interfaces = new HashSet<>();
    private Map<MethodDescriptor, MethodHolder> methods = new HashMap<>();
    private Map<String, FieldHolder> fields = new HashMap<>();

    public ClassHolder(String name) {
        super(name);
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Set<String> getInterfaces() {
        return interfaces;
    }

    public MethodHolder getMethod(MethodDescriptor method) {
        return methods.get(method);
    }

    public Collection<MethodHolder> getMethods() {
        return methods.values();
    }

    public void addMethod(MethodHolder method) {
        if (method.getOwner() != null) {
            throw new IllegalArgumentException("Method " + method.getDescriptor() +
                    " is already in another class (" + method.getOwner().getName() + ")");
        }
        method.setOwner(this);
        MethodHolder oldMethod = methods.put(method.getDescriptor(), method);
        if (oldMethod != null) {
            oldMethod.setOwner(null);
        }
    }

    public void removeMethod(MethodHolder method) {
        if (method.getOwner() != null) {
            throw new IllegalArgumentException("Method " + method.getDescriptor() +
                    " is not a member of " + getName());
        }
        if (method.getOwner() != this) {
            throw new IllegalArgumentException("Method " + method.getOwner().getName() +
                    "." + method.getDescriptor() + " is not a member of " + getName());
        }
        methods.remove(method.getDescriptor());
        method.setOwner(null);
    }

    public FieldHolder getField(String name) {
        return fields.get(name);
    }

    public Collection<FieldHolder> getFields() {
        return fields.values();
    }

    public void addField(FieldHolder field) {
        if (field.getOwner() != null) {
            throw new IllegalArgumentException("Field " + field.getName() + " is already " +
                    "in another class (" + field.getOwner().getName() + ")");
        }
        field.setOwner(this);
        FieldHolder oldField = fields.put(field.getName(), field);
        if (oldField != null) {
            oldField.setOwner(null);
        }
    }

    public void removeField(FieldHolder field) {
        if (field.getOwner() != null) {
            throw new IllegalArgumentException("Field " + field.getName() +
                    " is not a member of " + getName());
        }
        if (field.getOwner() != this) {
            throw new IllegalArgumentException("Field " + field.getOwner().getName() + "." +
                    field.getName() + " is not a member of " + getName());
        }
        fields.remove(field.getName());
    }
}
