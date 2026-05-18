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

import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;

public class IntrospectFieldImpl extends IntrospectAnnotatedElementImpl implements IntrospectField {
    private IntrospectClassImpl<?> declaringClass;
    public final FieldReader field;
    private IntrospectClassImpl<?> type;
    private IntrospectType genericType;

    IntrospectFieldImpl(IntrospectClassImpl<?> declaringClass, FieldReader field) {
        super(declaringClass.introspection());
        this.declaringClass = declaringClass;
        this.field = field;
    }

    @Override
    public IntrospectClass<?> declaringClass() {
        return declaringClass;
    }

    @Override
    public String name() {
        return field.getName();
    }

    @Override
    public int modifiers() {
        return Introspection.getModifiers(field);
    }

    @Override
    public boolean isEnumConstant() {
        return field.readModifiers().contains(ElementModifier.ENUM);
    }

    @Override
    public IntrospectClass<?> type() {
        if (type == null) {
            type = introspection.getClass(field.getType());
        }
        return type;
    }

    @Override
    public IntrospectType genericType() {
        if (genericType == null) {
            var gvt = field.getGenericType();
            genericType = gvt != null ? introspection.convertGenericType(gvt, declaringClass, null) : type();
        }
        return genericType;
    }

    public FieldReader getBackingField() {
        return field;
    }

    @Override
    protected AnnotationContainerReader annotationContainer() {
        return field.getAnnotations();
    }
}
