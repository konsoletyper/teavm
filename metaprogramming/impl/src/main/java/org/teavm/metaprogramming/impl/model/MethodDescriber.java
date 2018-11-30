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
package org.teavm.metaprogramming.impl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class MethodDescriber {
    private Diagnostics diagnostics;
    private ClassReaderSource classSource;
    private Map<MethodReference, Optional<MethodModel>> cache = new HashMap<>();
    private List<MethodModel> knownMethods = new ArrayList<>();

    public MethodDescriber(Diagnostics diagnostics, ClassReaderSource classSource) {
        this.diagnostics = diagnostics;
        this.classSource = classSource;
    }

    public MethodModel getMethod(MethodReference method) {
        return cache.computeIfAbsent(method, k -> {
            MethodModel model = describeMethod(k);
            if (model != null) {
                knownMethods.add(model);
            }
            return Optional.ofNullable(model);
        }).orElse(null);
    }

    public Iterable<MethodModel> getKnownMethods() {
        return knownMethods;
    }

    private MethodModel describeMethod(MethodReference methodRef) {
        ClassReader cls = classSource.get(methodRef.getClassName());
        if (cls == null) {
            return null;
        }
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        if (method == null) {
            return null;
        }
        if (method.getAnnotations().get(Meta.class.getName()) == null) {
            return null;
        }
        CallLocation location = new CallLocation(methodRef);

        MethodModel proxyMethod = findMetaMethod(method);
        if (proxyMethod == null) {
            diagnostics.error(location, "Corresponding meta method was not found");
            return null;
        }

        return proxyMethod;
    }

    private MethodModel findMetaMethod(MethodReader method) {
        ClassReader cls = classSource.get(method.getOwnerName());
        boolean isStatic = method.hasModifier(ElementModifier.STATIC);
        int expectedParameterCount = (isStatic ? 0 : 1) + method.parameterCount();
        for (MethodReader meta : cls.getMethods()) {
            if (meta == method
                    || !meta.hasModifier(ElementModifier.STATIC)
                    || !meta.getName().equals(method.getName())
                    || meta.getResultType() != ValueType.VOID
                    || meta.parameterCount() != expectedParameterCount) {
                continue;
            }

            int paramOffset = 0;
            if (!isStatic) {
                if (meta.parameterCount() == 0 || meta.parameterType(0).isObject(Value.class)) {
                    return null;
                }
                paramOffset++;
            }

            int classParamIndex = -1;
            for (int i = 0; i < method.parameterCount(); ++i) {
                ValueType proxyParam = meta.parameterType(i + paramOffset);
                if (proxyParam.isObject(ReflectClass.class)) {
                    if (classParamIndex == -1) {
                        classParamIndex = i;
                    } else {
                        return null;
                    }
                } else if (!proxyParam.isObject(Value.class)) {
                    return null;
                }
            }

            return new MethodModel(method.getReference(), meta.getReference(), classParamIndex, isStatic);
        }
        return null;
    }
}
