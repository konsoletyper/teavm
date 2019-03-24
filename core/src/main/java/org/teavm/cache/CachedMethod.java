/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.cache;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.GenericValueType;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ProgramReader;
import org.teavm.model.ValueType;

class CachedMethod extends CachedMember implements MethodReader {
    MethodReference reference;
    GenericTypeParameter[] typeParameters;
    GenericValueType genericReturnType;
    GenericValueType[] genericParameterTypes;
    CachedAnnotations[] parameterAnnotations;
    AnnotationValue annotationDefault;
    WeakReference<ProgramReader> program;
    Supplier<ProgramReader> programSupplier;

    @Override
    public GenericTypeParameter[] getTypeParameters() {
        return typeParameters != null ? typeParameters.clone() : new GenericTypeParameter[0];
    }

    @Override
    public ValueType getResultType() {
        return reference.getReturnType();
    }

    @Override
    public GenericValueType getGenericResultType() {
        return genericReturnType;
    }

    @Override
    public int parameterCount() {
        return reference.parameterCount();
    }

    @Override
    public ValueType[] getSignature() {
        return reference.getSignature();
    }

    @Override
    public ValueType parameterType(int index) {
        return reference.parameterType(index);
    }

    @Override
    public int genericParameterCount() {
        return genericParameterTypes != null ? genericParameterTypes.length : 0;
    }

    @Override
    public GenericValueType genericParameterType(int index) {
        return genericParameterTypes != null ? genericParameterTypes[index] : null;
    }

    @Override
    public ValueType[] getParameterTypes() {
        return reference.getParameterTypes();
    }

    @Override
    public AnnotationContainerReader parameterAnnotation(int index) {
        return parameterAnnotations[index];
    }

    @Override
    public AnnotationContainerReader[] getParameterAnnotations() {
        return parameterAnnotations.clone();
    }

    @Override
    public MethodDescriptor getDescriptor() {
        return reference.getDescriptor();
    }

    @Override
    public MethodReference getReference() {
        return reference;
    }

    @Override
    public ProgramReader getProgram() {
        if (programSupplier == null) {
            return null;
        }
        ProgramReader program = this.program != null ? this.program.get() : null;
        if (program == null) {
            program = programSupplier.get();
            this.program = new WeakReference<>(program);
        }
        return program;
    }

    @Override
    public AnnotationValue getAnnotationDefault() {
        return annotationDefault;
    }
}
