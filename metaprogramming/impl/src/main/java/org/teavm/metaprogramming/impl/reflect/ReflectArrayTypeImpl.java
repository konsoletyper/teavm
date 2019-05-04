/*
 *  Copyright 2019 konsoletyper.
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

import java.util.Map;
import org.teavm.metaprogramming.reflect.ReflectArrayType;
import org.teavm.metaprogramming.reflect.ReflectType;
import org.teavm.model.GenericValueType;

public class ReflectArrayTypeImpl implements ReflectArrayType {
    private ReflectContext context;
    private GenericValueType.Array innerType;
    private ReflectType componentType;
    private Map<String, ReflectTypeVariableImpl> variableMap;

    public ReflectArrayTypeImpl(ReflectContext context, GenericValueType.Array innerType,
            Map<String, ReflectTypeVariableImpl> variableMap) {
        this.context = context;
        this.innerType = innerType;
        this.variableMap = variableMap;
    }

    @Override
    public ReflectType getComponentType() {
        if (componentType == null) {
            componentType = context.getGenericType(innerType, variableMap);
        }
        return componentType;
    }
}
