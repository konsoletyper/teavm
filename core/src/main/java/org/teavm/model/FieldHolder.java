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

public class FieldHolder extends MemberHolder implements FieldReader {
    private ValueType type;
    private Object initialValue;
    private ClassHolder owner;

    public FieldHolder(String name) {
        super(name);
    }

    @Override
    public ValueType getType() {
        return type;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    @Override
    public Object getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(Object initialValue) {
        this.initialValue = initialValue;
    }

    ClassHolder getOwner() {
        return owner;
    }

    void setOwner(ClassHolder owner) {
        this.owner = owner;
    }

    @Override
    public String getOwnerName() {
        return owner != null ? owner.getName() : null;
    }

    @Override
    public FieldReference getReference() {
        return new FieldReference(getOwnerName(), getName());
    }
}
