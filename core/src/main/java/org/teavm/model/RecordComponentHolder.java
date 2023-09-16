/*
 *  Copyright 2023 ihromant.
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

public class RecordComponentHolder extends MemberHolder implements RecordComponentReader {
    private ValueType type;
    private GenericValueType genericType;
    private ClassHolder owner;
    private FieldReference reference;
    private MethodReference methodAccessorReference;

    public RecordComponentHolder(String name) {
        super(name);
    }

    @Override
    public ValueType getType() {
        return type;
    }

    @Override
    public GenericValueType getGenericType() {
        return genericType;
    }

    public void setGenericType(GenericValueType genericType) {
        this.genericType = genericType;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    ClassHolder getOwner() {
        return owner;
    }

    void setOwner(ClassHolder owner) {
        this.owner = owner;
        reference = null;
    }

    @Override
    public String getOwnerName() {
        return owner != null ? owner.getName() : null;
    }

    @Override
    public FieldReference getReference() {
        if (reference == null && owner != null) {
            reference = new FieldReference(getOwnerName(), getName());
        }
        return reference;
    }

    @Override
    public MethodReference getMethodAccessorReference() {
        if (methodAccessorReference == null && owner != null) {
            methodAccessorReference = new MethodReference(getOwnerName(), getName(), getType());
        }
        return null;
    }

    public void updateReferences(ReferenceCache cache) {
        FieldReference reference = getReference();
        if (reference != null) {
            this.reference = cache.getCached(reference);
        }
        MethodReference methodAccessorReference = getMethodAccessorReference();
        if (reference != null) {
            this.methodAccessorReference = cache.getCached(methodAccessorReference);
        }
    }
}
