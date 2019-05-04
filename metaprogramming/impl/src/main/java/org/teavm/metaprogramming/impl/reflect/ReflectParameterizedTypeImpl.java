/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.metaprogramming.impl.reflect;

import java.util.Map;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.reflect.ReflectParameterizedType;
import org.teavm.metaprogramming.reflect.ReflectTypeArgument;
import org.teavm.metaprogramming.reflect.ReflectVariance;
import org.teavm.model.GenericValueType;
import org.teavm.model.ValueType;

public class ReflectParameterizedTypeImpl implements ReflectParameterizedType {
    private ReflectContext context;
    private GenericValueType.Object innerType;
    private ReflectClassImpl<?> typeConstructor;
    private ReflectParameterizedTypeImpl ownerType;
    private boolean ownerTypeResolved;
    private ReflectTypeArgument[] typeArguments;
    private Map<String, ReflectTypeVariableImpl> variableMap;

    public ReflectParameterizedTypeImpl(ReflectContext context, GenericValueType.Object innerType,
            Map<String, ReflectTypeVariableImpl> variableMap) {
        this.context = context;
        this.innerType = innerType;
        this.variableMap = variableMap;
    }

    @Override
    public ReflectTypeArgument[] getTypeArguments() {
        if (typeArguments == null) {
            GenericValueType.Argument[] innerArguments = innerType.getArguments();
            if (innerArguments.length == 0) {
                typeArguments = ReflectContext.EMPTY_TYPE_ARGUMENTS;
            } else {
                typeArguments = new ReflectTypeArgument[innerArguments.length];
                for (int i = 0; i < innerArguments.length; ++i) {
                    GenericValueType.Argument innerArgument = innerArguments[i];
                    typeArguments[i] = new ReflectTypeArgumentImpl(context, mapVariance(innerArgument.getKind()),
                            innerArgument.getValue(), variableMap);
                }
            }
        }
        return typeArguments.length == 0 ? typeArguments : typeArguments.clone();
    }

    private static ReflectVariance mapVariance(GenericValueType.ArgumentKind kind) {
        switch (kind) {
            case ANY:
            case COVARIANT:
                return ReflectVariance.COVARIANT;
            case CONTRAVARIANT:
                return ReflectVariance.CONTRAVARIANT;
            case INVARIANT:
                return ReflectVariance.INVARIANT;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public ReflectParameterizedType getOwnerType() {
        if (!ownerTypeResolved) {
            ownerTypeResolved = true;
            if (innerType.getParent() != null) {
                String fullName = getFullClassName(innerType.getParent(), new StringBuilder()).toString();
                ReflectClassImpl<?> ownerClass = context.getClass(ValueType.object(fullName));
                ownerType = (ReflectParameterizedTypeImpl) context.getGenericType(innerType.getParent(),
                        ownerClass.getTypeVariableMap());
            }
        }
        return ownerType;
    }

    @Override
    public ReflectClass<?> getTypeConstructor() {
        if (typeConstructor == null) {
            String fullName = getFullClassName(innerType, new StringBuilder()).toString();
            typeConstructor = context.getClass(ValueType.object(fullName));
        }
        return typeConstructor;
    }

    private static StringBuilder getFullClassName(GenericValueType.Object type, StringBuilder sb) {
        if (type.getParent() != null) {
            getFullClassName(type.getParent(), sb).append("$");
        }
        return sb.append(type.getClassName());
    }
}
