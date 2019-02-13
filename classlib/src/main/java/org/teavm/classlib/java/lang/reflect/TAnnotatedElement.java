/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.java.lang.reflect;

import java.util.ArrayList;
import java.util.List;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.annotation.TAnnotation;

public interface TAnnotatedElement {
    default boolean isAnnotationPresent(TClass<? extends TAnnotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    <T extends TAnnotation> T getAnnotation(TClass<T> annotationClass);

    TAnnotation[] getAnnotations();

    TAnnotation[] getDeclaredAnnotations();

    default <T extends TAnnotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        List<T> result = new ArrayList<>();
        Object classAsObject = annotationClass;
        for (TAnnotation annot : getAnnotations()) {
            if (annot.annotationType() == classAsObject) {
                result.add(annotationClass.cast((TObject) annot));
            }
        }
        @SuppressWarnings("unchecked")
        T[] array = (T[]) (Object) TArray.newInstance(annotationClass, result.size());
        return result.toArray(array);
    }

    default <T extends TAnnotation> T getDeclaredAnnotation(TClass<T> annotationClass) {
        Object classAsObject = annotationClass;
        for (TAnnotation annot : getDeclaredAnnotations()) {
            if (annot.annotationType() == classAsObject) {
                return annotationClass.cast((TObject) annot);
            }
        }
        return null;
    }

    default <T extends TAnnotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        List<T> result = new ArrayList<>();
        Object classAsObject = annotationClass;
        for (TAnnotation annot : getDeclaredAnnotations()) {
            if (annot.annotationType() == classAsObject) {
                result.add(annotationClass.cast((TObject) annot));
            }
        }
        @SuppressWarnings("unchecked")
        T[] array = (T[]) (Object) TArray.newInstance(annotationClass, result.size());
        return result.toArray(array);
    }
}
