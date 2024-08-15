/*
 *  Copyright 2024 Alexey Andreev.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.AccessLevel;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

abstract class JSAnnotationCache<T> {
    private ClassReaderSource classes;
    protected Diagnostics diagnostics;
    private Map<MethodReference, Value<T>> data = new HashMap<>();

    JSAnnotationCache(ClassReaderSource classes, Diagnostics diagnostics) {
        this.classes = classes;
        this.diagnostics = diagnostics;
    }

    T get(MethodReference methodReference, CallLocation location) {
        var result = getValue(methodReference, location);
        return result != null ? result.annotation : null;
    }

    private Value<T> getValue(MethodReference methodReference, CallLocation location) {
        var result = data.get(methodReference);
        if (result == null) {
            result = extract(methodReference, location);
            data.put(methodReference, result);
        }
        return result;
    }

    private Value<T> extract(MethodReference methodReference, CallLocation location) {
        var cls = classes.get(methodReference.getClassName());
        if (cls == null) {
            return new Value<>(null, methodReference);
        }
        var method = cls.getMethod(methodReference.getDescriptor());
        if (method == null || method.hasModifier(ElementModifier.STATIC)
                || method.getLevel() == AccessLevel.PRIVATE) {
            for (var candidateMethod : cls.getMethods()) {
                if (candidateMethod.getName().equals(methodReference.getName())
                        && !candidateMethod.hasModifier(ElementModifier.STATIC)
                        && !candidateMethod.hasModifier(ElementModifier.FINAL)
                        && candidateMethod.getLevel() != AccessLevel.PRIVATE
                        && Arrays.equals(candidateMethod.getParameterTypes(), methodReference.getParameterTypes())) {
                    method = candidateMethod;
                    break;
                }
            }
        }

        if (method != null) {
            methodReference = method.getReference();
            var annotation = take(method, location);
            if (annotation != null) {
                return new Value<>(annotation, methodReference);
            }
        }

        var candidates = new HashMap<MethodReference, T>();
        if (cls.getParent() != null) {
            var value = getValue(new MethodReference(cls.getParent(), methodReference.getDescriptor()), location);
            if (value.annotation != null) {
                candidates.put(value.source, value.annotation);
            }
        }
        for (var itf : cls.getInterfaces()) {
            var value = getValue(new MethodReference(itf, methodReference.getDescriptor()), location);
            if (value != null) {
                candidates.put(value.source, value.annotation);
            }
        }
        if (candidates.isEmpty()) {
            return new Value<>(null, methodReference);
        }
        if (candidates.size() == 1) {
            var entry = candidates.entrySet().iterator().next();
            return new Value<>(entry.getValue(), entry.getKey());
        }

        T annot = null;
        MethodReference lastMethod = null;
        for (var entry : candidates.entrySet()) {
            if (annot != null && !annot.equals(entry.getValue())) {
                diagnostics.error(location, "Method '{{m0}}' has inconsistent JS annotations from overridden "
                        + "methods '{{m1}}' and '{{m2}}', so it should be annotated explicitly",
                        methodReference, lastMethod, entry.getKey());
                return new Value<>(null, methodReference);
            }
            annot = entry.getValue();
            lastMethod = entry.getKey();
        }
        return new Value<>(annot, methodReference);
    }

    protected abstract T take(MethodReader method, CallLocation location);

    private static class Value<T> {
        final T annotation;
        final MethodReference source;

        Value(T annotation, MethodReference source) {
            this.annotation = annotation;
            this.source = source;
        }
    }
}
