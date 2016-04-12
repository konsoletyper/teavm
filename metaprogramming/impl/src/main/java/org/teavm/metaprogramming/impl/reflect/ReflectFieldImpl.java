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
package org.teavm.metaprogramming.impl.reflect;

import java.lang.annotation.Annotation;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.reflect.ReflectField;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;

public class ReflectFieldImpl implements ReflectField {
    private ReflectContext context;
    private ReflectClassImpl<?> declaringClass;
    public final FieldReader field;
    private ReflectClassImpl<?> type;
    private ReflectAnnotatedElementImpl annotations;

    public ReflectFieldImpl(ReflectClassImpl<?> declaringClass, FieldReader field) {
        context = declaringClass.getReflectContext();
        this.declaringClass = declaringClass;
        this.field = field;
    }

    @Override
    public ReflectClass<?> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public int getModifiers() {
        return ReflectContext.getModifiers(field);
    }

    @Override
    public boolean isEnumConstant() {
        return field.readModifiers().contains(ElementModifier.ENUM);
    }

    @Override
    public ReflectClass<?> getType() {
        if (type == null) {
            type = context.getClass(field.getType());
        }
        return type;
    }

    @Override
    public Object get(Object target) {
        throw new IllegalStateException("Don't call this method from compile domain");
    }

    @Override
    public void set(Object target, Object value) {
        throw new IllegalStateException("Don't call this method from compile domain");
    }

    public FieldReader getBackingField() {
        return field;
    }

    @Override
    public <S extends Annotation> S getAnnotation(Class<S> type) {
        if (annotations == null) {
            annotations = new ReflectAnnotatedElementImpl(context, field.getAnnotations());
        }
        return annotations.getAnnotation(type);
    }
}
