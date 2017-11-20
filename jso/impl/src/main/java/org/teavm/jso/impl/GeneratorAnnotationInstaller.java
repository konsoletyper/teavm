/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.jso.impl;

import java.util.function.Function;
import org.teavm.backend.javascript.ProviderContext;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

class GeneratorAnnotationInstaller<T> implements Function<ProviderContext, T> {
    private T generator;
    private String annotationName;

    GeneratorAnnotationInstaller(T generator, String annotationName) {
        this.generator = generator;
        this.annotationName = annotationName;
    }

    @Override
    public T apply(ProviderContext providerContext) {
        ClassReaderSource classSource = providerContext.getClassSource();
        MethodReference methodRef = providerContext.getMethod();
        ClassReader cls = classSource.get(methodRef.getClassName());
        if (cls == null) {
            return null;
        }

        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        if (method == null) {
            return null;
        }

        return method.getAnnotations().get(annotationName) != null ? generator : null;
    }
}
