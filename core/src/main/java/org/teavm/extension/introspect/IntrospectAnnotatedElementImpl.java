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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.teavm.model.AnnotationContainerReader;

public abstract class IntrospectAnnotatedElementImpl implements IntrospectAnnotatedElement {
    protected final Introspection introspection;
    private Map<IntrospectClass<?>, IntrospectAnnotation<?>> annotationsByType;
    private Map<? extends IntrospectClass<?>, ? extends List<? extends IntrospectAnnotation<?>>> annotationListsByType;
    private List<IntrospectAnnotation<?>> allAnnotations;

    IntrospectAnnotatedElementImpl(Introspection introspection) {
        this.introspection = introspection;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Annotation> IntrospectAnnotation<A> annotation(IntrospectClass<A> type) {
        if (annotationsByType == null) {
            annotationsByType = allAnnotations()
                    .stream()
                    .collect(Collectors.toMap(IntrospectAnnotation::type, a -> a));
        }
        return (IntrospectAnnotation<A>) annotationsByType.get(type);
    }

    @Override
    public <A extends Annotation> IntrospectAnnotation<A> annotation(Class<A> type) {
        return annotation(introspection.findClass(type));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Annotation> List<? extends IntrospectAnnotation<A>> annotations(IntrospectClass<A> type) {
        if (annotationListsByType == null) {
            var result = new HashMap<IntrospectClass<?>, List<IntrospectAnnotation<?>>>();
            for (var annotation : allAnnotations()) {
                result.computeIfAbsent(annotation.type(), k -> new ArrayList<>()).add(annotation);
            }
            var unmodifiableResult = new HashMap<IntrospectClass<?>, List<IntrospectAnnotation<?>>>();
            for (var entry : result.entrySet()) {
                unmodifiableResult.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
            }
            annotationListsByType = unmodifiableResult;
        }
        var list = (List<? extends IntrospectAnnotation<A>>) annotationListsByType.get(type);
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public <A extends Annotation> List<? extends IntrospectAnnotation<A>> annotations(Class<A> type) {
        return annotations(introspection.findClass(type));
    }

    @Override
    public List<? extends IntrospectAnnotation<?>> allAnnotations() {
        if (allAnnotations == null) {
            var underlyingAnnotations = annotationContainer();
            if (underlyingAnnotations == null) {
                allAnnotations = Collections.emptyList();
            } else {
                allAnnotations = StreamSupport.stream(annotationContainer().all().spliterator(), false)
                        .map(annotation -> new IntrospectAnnotationImpl<>(introspection, annotation))
                        .collect(Collectors.toUnmodifiableList());
            }
        }
        return allAnnotations;
    }

    protected abstract AnnotationContainerReader annotationContainer();
}
