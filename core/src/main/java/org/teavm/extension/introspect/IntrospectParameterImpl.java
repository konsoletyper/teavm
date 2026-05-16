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

import org.teavm.model.AnnotationContainerReader;

public class IntrospectParameterImpl extends IntrospectAnnotatedElementImpl implements IntrospectParameter {
    private final IntrospectMethodImpl declaringMethod;
    private final int index;
    private IntrospectClass<?> type;
    private IntrospectType genericType;

    IntrospectParameterImpl(IntrospectMethodImpl declaringMethod, int index) {
        super(declaringMethod.introspection);
        this.declaringMethod = declaringMethod;
        this.index = index;
    }

    @Override
    public IntrospectClass<?> type() {
        if (type == null) {
            type = introspection.getClass(declaringMethod.method.parameterType(index));
        }
        return type;
    }

    @Override
    public IntrospectType genericType() {
        if (genericType == null) {
            var gvt = declaringMethod.method.genericParameterType(index);
            genericType = gvt != null
                    ? introspection.convertGenericType(gvt, declaringMethod.declaringClass, declaringMethod)
                    : type();
        }
        return genericType;
    }

    @Override
    protected AnnotationContainerReader annotationContainer() {
        return declaringMethod.method.parameterAnnotation(index);
    }
}
