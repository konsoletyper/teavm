/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.extension.introspect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;

public class IntrospectMethodImpl extends IntrospectAnnotatedElementImpl implements IntrospectMethod {
    IntrospectClassImpl<?> declaringClass;
    public final MethodReader method;
    private IntrospectClassImpl<?> returnType;
    private List<? extends IntrospectParameter> parameters;
    private List<? extends IntrospectTypeVariable> typeParameters;
    private List<? extends IntrospectClass<? extends Throwable>> exceptionTypes;

    IntrospectMethodImpl(IntrospectClassImpl<?> declaringClass, MethodReader method) {
        super(declaringClass.introspection);
        this.declaringClass = declaringClass;
        this.method = method;
    }

    @Override
    public IntrospectClass<?> declaringClass() {
        return declaringClass;
    }

    @Override
    public String name() {
        return method.getName();
    }

    @Override
    public int modifiers() {
        return Introspection.getModifiers(method);
    }

    @Override
    public boolean isConstructor() {
        return method.getName().equals("<init>");
    }

    @Override
    public IntrospectClass<?> returnType() {
        if (returnType == null) {
            returnType = introspection.getClass(method.getResultType());
        }
        return returnType;
    }

    @Override
    public List<? extends IntrospectParameter> parameters() {
        if (parameters == null) {
            var params = new ArrayList<IntrospectParameterImpl>(method.parameterCount());
            for (var i = 0; i < method.parameterCount(); ++i) {
                params.add(new IntrospectParameterImpl(this, i));
            }
            parameters = Collections.unmodifiableList(params);
        }
        return parameters;
    }

    @Override
    public List<? extends IntrospectTypeVariable> typeParameters() {
        if (typeParameters == null) {
            GenericTypeParameter[] params = method.getTypeParameters();
            if (params == null || params.length == 0) {
                typeParameters = Collections.emptyList();
            } else {
                var result = new ArrayList<IntrospectTypeVariableImpl>(params.length);
                for (var param : params) {
                    result.add(new IntrospectTypeVariableImpl(param, declaringClass, this, introspection));
                }
                typeParameters = Collections.unmodifiableList(result);
            }
        }
        return typeParameters;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends IntrospectClass<? extends Throwable>> exceptionTypes() {
        if (exceptionTypes == null) {
            var thrownTypes = method.getThrownTypes();
            if (thrownTypes == null || thrownTypes.isEmpty()) {
                exceptionTypes = Collections.emptyList();
            } else {
                var result = new ArrayList<IntrospectClass<? extends Throwable>>(thrownTypes.size());
                for (var name : thrownTypes) {
                    result.add((IntrospectClass<? extends Throwable>) introspection.getClass(
                            ValueType.object(name)));
                }
                exceptionTypes = Collections.unmodifiableList(result);
            }
        }
        return exceptionTypes;
    }

    IntrospectTypeVariableImpl typeParameter(String name) {
        for (var tp : typeParameters()) {
            if (tp.name().equals(name)) {
                return (IntrospectTypeVariableImpl) tp;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType()).append(' ').append(name()).append('(');
        sb.append(parameters().stream().map(p -> p.type().toString()).collect(Collectors.joining(", ")));
        sb.append(')');
        return sb.toString();
    }

    @Override
    protected AnnotationContainerReader annotationContainer() {
        return method.getAnnotations();
    }
}
