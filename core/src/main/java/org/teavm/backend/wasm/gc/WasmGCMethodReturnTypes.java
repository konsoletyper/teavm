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
package org.teavm.backend.wasm.gc;

import java.util.HashMap;
import java.util.Map;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmGCMethodReturnTypes {
    private DependencyInfo dependencyInfo;
    private ClassHierarchy hierarchy;
    private Map<MethodReference, ValueType> cache = new HashMap<>();

    public WasmGCMethodReturnTypes(DependencyInfo dependencyInfo, ClassHierarchy hierarchy) {
        this.dependencyInfo = dependencyInfo;
        this.hierarchy = hierarchy;
    }

    public ValueType returnTypeOf(MethodReference reference) {
        return cache.computeIfAbsent(reference, this::calculateReturnType);
    }

    private ValueType calculateReturnType(MethodReference reference) {
        if (!(reference.getReturnType() instanceof ValueType.Object)) {
            return reference.getReturnType();
        }
        var method = dependencyInfo.getMethod(reference);
        if (method == null) {
            return reference.getReturnType();
        }
        var types = method.getResult().getTypes();
        if (types.length == 0) {
            return reference.getReturnType();
        }
        var type = hierarchy.getClassSource().get(types[0]);
        if (type == null) {
            return reference.getReturnType();
        }
        for (var i = 1; i < types.length; ++i) {
            var otherType = hierarchy.getClassSource().get(types[i]);
            if (otherType == null) {
                return reference.getReturnType();
            }
            type = hierarchy.getClassSource().get(WasmGCUtil.findCommonSuperclass(hierarchy, type, otherType));
        }
        return ValueType.object(type.getName());
    }
}
