/*
 *  Copyright 2026 Alexey Andreev.
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
import org.teavm.model.GenericValueType;
import org.teavm.model.ValueType;

public class IntrospectParameterizedTypeImpl implements IntrospectParameterizedType {
    private final GenericValueType.Object type;
    private final IntrospectClassImpl<?> declaringClass;
    private final IntrospectMethodImpl declaringMethod;
    private final Introspection introspection;
    private IntrospectClass<?> rawType;
    private IntrospectType ownerType;
    private boolean ownerTypeResolved;
    private List<IntrospectTypeArgument> typeArguments;

    IntrospectParameterizedTypeImpl(GenericValueType.Object type, IntrospectClassImpl<?> declaringClass,
            IntrospectMethodImpl declaringMethod, Introspection introspection) {
        this.type = type;
        this.declaringClass = declaringClass;
        this.declaringMethod = declaringMethod;
        this.introspection = introspection;
    }

    @Override
    public IntrospectClass<?> rawType() {
        if (rawType == null) {
            rawType = introspection.getClass(ValueType.object(type.getFullClassName()));
        }
        return rawType;
    }

    @Override
    public IntrospectType ownerType() {
        if (!ownerTypeResolved) {
            ownerTypeResolved = true;
            var parent = type.getParent();
            if (parent != null) {
                ownerType = introspection.convertGenericType(parent, declaringClass, declaringMethod);
            }
        }
        return ownerType;
    }

    @Override
    public List<? extends IntrospectTypeArgument> typeArguments() {
        if (typeArguments == null) {
            var args = type.getArguments();
            var result = new ArrayList<IntrospectTypeArgument>(args.length);
            for (var arg : args) {
                result.add(new IntrospectTypeArgumentImpl(arg, declaringClass, declaringMethod, introspection));
            }
            typeArguments = Collections.unmodifiableList(result);
        }
        return typeArguments;
    }
}
