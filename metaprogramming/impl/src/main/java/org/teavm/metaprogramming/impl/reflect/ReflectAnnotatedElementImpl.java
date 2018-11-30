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
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.teavm.metaprogramming.reflect.ReflectAnnotatedElement;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.AnnotationReader;

public class ReflectAnnotatedElementImpl implements ReflectAnnotatedElement {
    private ReflectContext context;
    private AnnotationContainerReader annotationContainer;
    private Map<Class<?>, Annotation> annotations = new HashMap<>();

    public ReflectAnnotatedElementImpl(ReflectContext context, AnnotationContainerReader annotationContainer) {
        this.context = context;
        this.annotationContainer = annotationContainer;
    }

    @Override
    public <S extends Annotation> S getAnnotation(Class<S> type) {
        @SuppressWarnings("unchecked")
        S result = (S) annotations.computeIfAbsent(type, t -> {
            if (annotationContainer == null) {
                return null;
            }
            AnnotationReader annot = annotationContainer.get(t.getName());
            if (annot == null) {
                return null;
            }

            AnnotationProxy handler = new AnnotationProxy(context.getClassLoader(), context.getHierarchy(),
                    annot, t);
            return (Annotation) Proxy.newProxyInstance(context.getClassLoader(), new Class<?>[] { t }, handler);
        });
        return result;
    }
}
