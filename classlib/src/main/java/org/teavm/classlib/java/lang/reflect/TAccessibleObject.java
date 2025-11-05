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
package org.teavm.classlib.java.lang.reflect;

import java.util.HashMap;
import java.util.Map;
import org.teavm.classlib.java.lang.TClass;
import org.teavm.classlib.java.lang.TSecurityException;
import org.teavm.classlib.java.lang.annotation.TAnnotation;

public class TAccessibleObject implements TAnnotatedElement {
    private Map<TClass<?>, TAnnotation> annotationMap;

    protected TAccessibleObject() {
    }

    public static void setAccessible(TAccessibleObject[] array, boolean flag) throws TSecurityException {
        // Do nothing
    }

    public void setAccessible(boolean flag) throws TSecurityException {
        // Do nothing
    }

    public boolean isAccessible() {
        return true;
    }

    @Override
    public <T extends TAnnotation> T getDeclaredAnnotation(TClass<T> annotationClass) {
        return getAnnotation(annotationClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TAnnotation> T getAnnotation(TClass<T> annotationClass) {
        if (annotationMap == null) {
            annotationMap = new HashMap<>();
            for (var annotation : getDeclaredAnnotations()) {
                annotationMap.put((TClass<?>) (Object) annotation.annotationType(), annotation);
            }
        }
        return (T) annotationMap.get(annotationClass);
    }

    @Override
    public TAnnotation[] getAnnotations() {
        return getDeclaredAnnotations();
    }

    @Override
    public TAnnotation[] getDeclaredAnnotations() {
        throw new UnsupportedOperationException();
    }
}
