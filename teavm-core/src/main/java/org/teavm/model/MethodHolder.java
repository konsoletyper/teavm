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

/**
 *
 * @author Alexey Andreev
 */
public class MethodHolder extends MemberHolder {
    private MethodDescriptor descriptor;
    private ClassHolder owner;
    private Program program;

    public MethodHolder(MethodDescriptor descriptor) {
        super(descriptor.getName());
        this.descriptor = descriptor;
    }

    public MethodHolder(String name, ValueType... signature) {
        this(new MethodDescriptor(name, signature));
    }

    public ValueType getResultType() {
        return descriptor.getResultType();
    }

    public int parameterCount() {
        return descriptor.parameterCount();
    }

    public ValueType[] getSignature() {
        return descriptor.getSignature();
    }

    public ValueType parameterType(int index) {
        return descriptor.parameterType(index);
    }

    public ValueType[] getParameterTypes() {
        return descriptor.getParameterTypes();
    }

    @Override
    public ClassHolder getOwner() {
        return owner;
    }

    void setOwner(ClassHolder owner) {
        this.owner = owner;
    }

    public MethodDescriptor getDescriptor() {
        return descriptor;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        if (this.program != null) {
            this.program.setMethod(null);
        }
        this.program = program;
        if (this.program != null) {
            this.program.setMethod(this);
        }
    }
}
