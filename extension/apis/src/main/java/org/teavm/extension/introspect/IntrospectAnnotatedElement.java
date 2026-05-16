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

import java.lang.annotation.Annotation;
import java.util.List;

public interface IntrospectAnnotatedElement {
    <A extends Annotation> IntrospectAnnotation<A> annotation(IntrospectClass<A> type);

    <A extends Annotation> IntrospectAnnotation<A> annotation(Class<A> type);

    <A extends Annotation> List<? extends IntrospectAnnotation<A>> annotations(IntrospectClass<A> type);

    <A extends Annotation> List<? extends IntrospectAnnotation<A>> annotations(Class<A> type);

    List<? extends IntrospectAnnotation<?>> allAnnotations();
    
    default boolean hasAnnotation(IntrospectClass<? extends Annotation> type) {
        return annotation(type) != null;
    }
    
    default boolean hasAnnotation(Class<? extends Annotation> type) {
        return annotation(type) != null;
    }
}
