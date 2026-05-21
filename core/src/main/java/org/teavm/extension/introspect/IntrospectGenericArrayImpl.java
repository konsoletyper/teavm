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

import org.teavm.model.GenericValueType;

public class IntrospectGenericArrayImpl implements IntrospectGenericArray {
    private final GenericValueType.Array type;
    private final IntrospectClassImpl<?> declaringClass;
    private final IntrospectMethodImpl declaringMethod;
    private final Introspection introspection;
    private IntrospectType componentType;

    IntrospectGenericArrayImpl(GenericValueType.Array type, IntrospectClassImpl<?> declaringClass,
            IntrospectMethodImpl declaringMethod, Introspection introspection) {
        this.type = type;
        this.declaringClass = declaringClass;
        this.declaringMethod = declaringMethod;
        this.introspection = introspection;
    }

    @Override
    public String toString() {
        return componentType().toString() + "[]";
    }

    @Override
    public IntrospectType componentType() {
        if (componentType == null) {
            componentType = introspection.convertGenericType(type.getItemType(), declaringClass, declaringMethod);
        }
        return componentType;
    }
}
