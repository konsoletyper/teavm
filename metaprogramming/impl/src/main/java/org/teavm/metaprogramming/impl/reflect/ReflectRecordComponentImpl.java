/*
 *  Copyright 2023 ihromant.
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
import org.teavm.metaprogramming.reflect.ReflectRecordComponent;
import org.teavm.model.RecordComponentReader;

public class ReflectRecordComponentImpl implements ReflectRecordComponent {
    private ReflectContext context;
    private ReflectClassImpl<?> declaringClass;
    public final RecordComponentReader recordComponent;
    private ReflectClassImpl<?> type;
    private ReflectAnnotatedElementImpl annotations;

    public ReflectRecordComponentImpl(ReflectClassImpl<?> declaringClass, RecordComponentReader recordComponent) {
        context = declaringClass.getReflectContext();
        this.declaringClass = declaringClass;
        this.recordComponent = recordComponent;
    }

    @Override
    public ReflectClass<?> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getName() {
        return recordComponent.getName();
    }

    @Override
    public int getModifiers() {
        return ReflectContext.getModifiers(recordComponent);
    }

    @Override
    public ReflectClass<?> getType() {
        if (type == null) {
            type = context.getClass(recordComponent.getType());
        }
        return type;
    }

    @Override
    public Object get(Object target) {
        throw new IllegalStateException("Don't call this method from compile domain");
    }

    public RecordComponentReader getBackingRecordComponent() {
        return recordComponent;
    }

    @Override
    public <S extends Annotation> S getAnnotation(Class<S> type) {
        if (annotations == null) {
            annotations = new ReflectAnnotatedElementImpl(context, recordComponent.getAnnotations());
        }
        return annotations.getAnnotation(type);
    }
}
