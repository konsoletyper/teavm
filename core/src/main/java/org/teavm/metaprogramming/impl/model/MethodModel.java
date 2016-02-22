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

import java.util.HashMap;
import java.util.Map;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class MethodModel {
    private MethodReference method;
    private MethodReference metaMethod;
    private int classParameterIndex;
    private boolean isStatic;
    private Map<ValueType, MethodReference> usages = new HashMap<>();

    MethodModel(MethodReference method, MethodReference metaMethod, int classParameterIndex, boolean isStatic) {
        this.method = method;
        this.metaMethod = metaMethod;
        this.classParameterIndex = classParameterIndex;
        this.isStatic = isStatic;
    }

    public MethodReference getMethod() {
        return method;
    }

    public MethodReference getMetaMethod() {
        return metaMethod;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public int getClassParameterIndex() {
        return classParameterIndex;
    }

    public Map<ValueType, MethodReference> getUsages() {
        return usages;
    }

    public int getMetaParameterCount() {
        return (isStatic ? 0 : 1) + method.parameterCount();
    }

    public ValueType getMetaParameterType(int index) {
        if (!isStatic) {
            if (index == 0) {
                return ValueType.object(method.getClassName());
            } else {
                --index;
            }
        }
        return method.parameterType(index);
    }

    public int getMetaClassParameterIndex() {
        return classParameterIndex >= 0 ? mapParameterIndex(classParameterIndex) : -1;
    }

    public int mapParameterIndex(int index) {
        return isStatic ? index : index + 1;
    }
}
