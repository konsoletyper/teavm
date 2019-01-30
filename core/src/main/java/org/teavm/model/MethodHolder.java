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

import java.util.function.Function;

public class MethodHolder extends MemberHolder implements MethodReader {
    private MethodDescriptor descriptor;
    private ClassHolder owner;
    private Program program;
    private Function<MethodHolder, Program> programSupplier;
    private AnnotationValue annotationDefault;
    private AnnotationContainer[] parameterAnnotations;
    private MethodReference reference;

    public MethodHolder(MethodDescriptor descriptor) {
        super(descriptor.getName());
        this.descriptor = descriptor;
        parameterAnnotations = new AnnotationContainer[descriptor.parameterCount()];
        for (int i = 0; i < parameterAnnotations.length; ++i) {
            parameterAnnotations[i] = new AnnotationContainer();
        }
    }

    public MethodHolder(String name, ValueType... signature) {
        this(new MethodDescriptor(name, signature));
    }

    @Override
    public ValueType getResultType() {
        return descriptor.getResultType();
    }

    @Override
    public int parameterCount() {
        return descriptor.parameterCount();
    }

    @Override
    public ValueType[] getSignature() {
        return descriptor.getSignature();
    }

    @Override
    public ValueType parameterType(int index) {
        return descriptor.parameterType(index);
    }

    @Override
    public ValueType[] getParameterTypes() {
        return descriptor.getParameterTypes();
    }

    @Override
    public AnnotationContainer parameterAnnotation(int index) {
        return parameterAnnotations[index];
    }

    @Override
    public AnnotationContainer[] getParameterAnnotations() {
        return parameterAnnotations.clone();
    }

    @Override
    public String getOwnerName() {
        return owner != null ? owner.getName() : null;
    }

    ClassHolder getOwner() {
        return owner;
    }

    void setOwner(ClassHolder owner) {
        reference = null;
        this.owner = owner;
    }

    @Override
    public MethodDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public MethodReference getReference() {
        if (owner == null) {
            return null;
        }
        if (reference == null) {
            reference = new MethodReference(owner.getName(), descriptor);
        }
        return reference;
    }

    public void updateReference(ReferenceCache cache) {
        MethodReference reference = getReference();
        if (reference != null) {
            this.reference = cache.getCached(reference);
        }
    }

    @Override
    public Program getProgram() {
        if (program == null && programSupplier != null) {
            program = programSupplier.apply(this);
            if (program != null) {
                program.setMethod(this);
            }
            programSupplier = null;
        }
        return program;
    }

    public void setProgram(Program program) {
        if (this.program != null) {
            this.program.setMethod(null);
        }
        this.program = program;
        this.programSupplier = null;
        if (this.program != null) {
            this.program.setMethod(this);
        }
    }

    public void setProgramSupplier(Function<MethodHolder, Program> programSupplier) {
        if (this.program != null) {
            this.program.setMethod(null);
        }
        this.program = null;
        this.programSupplier = programSupplier;
    }

    @Override
    public AnnotationValue getAnnotationDefault() {
        return annotationDefault;
    }

    public void setAnnotationDefault(AnnotationValue annotationDefault) {
        this.annotationDefault = annotationDefault;
    }
}
