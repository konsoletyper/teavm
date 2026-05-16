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
import org.teavm.model.GenericTypeParameter;

public class IntrospectTypeVariableImpl implements IntrospectTypeVariable {
    private final String name;
    private final GenericTypeParameter parameter;
    private final IntrospectClassImpl<?> declaringClass;
    private final IntrospectMethodImpl declaringMethod;
    private final Introspection introspection;
    private List<IntrospectType> superTypes;

    IntrospectTypeVariableImpl(GenericTypeParameter parameter, IntrospectClassImpl<?> declaringClass,
            IntrospectMethodImpl declaringMethod, Introspection introspection) {
        this.name = parameter.getName();
        this.parameter = parameter;
        this.declaringClass = declaringClass;
        this.declaringMethod = declaringMethod;
        this.introspection = introspection;
    }

    IntrospectTypeVariableImpl(String name) {
        this.name = name;
        this.parameter = null;
        this.declaringClass = null;
        this.declaringMethod = null;
        this.introspection = null;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<? extends IntrospectType> superTypes() {
        if (superTypes == null) {
            if (parameter == null) {
                superTypes = Collections.emptyList();
            } else {
                var bounds = parameter.extractAllBounds();
                var result = new ArrayList<IntrospectType>(bounds.size());
                for (var bound : bounds) {
                    result.add(introspection.convertGenericType(bound, declaringClass, declaringMethod));
                }
                superTypes = Collections.unmodifiableList(result);
            }
        }
        return superTypes;
    }

    @Override
    public IntrospectGenericDeclaration genericDeclaration() {
        return declaringMethod != null ? declaringMethod : declaringClass;
    }
}
