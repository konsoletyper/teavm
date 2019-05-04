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

import java.util.LinkedHashMap;
import java.util.Map;
import org.teavm.metaprogramming.reflect.ReflectParameterizedMember;
import org.teavm.metaprogramming.reflect.ReflectTypeVariable;
import org.teavm.model.GenericTypeParameter;
import org.teavm.model.GenericValueType;

public abstract class ReflectParameterizedMemberImpl implements ReflectParameterizedMember {
    private Map<String, ReflectTypeVariableImpl> typeVariableMap;
    private ReflectTypeVariableImpl[] typeParameters;

    protected abstract GenericTypeParameter[] getUnderlyingTypeParameters();

    abstract ReflectContext getReflectContext();

    @Override
    public ReflectTypeVariable[] getTypeParameters() {
        if (typeParameters == null) {
            typeParameters = getTypeVariableMap().values().toArray(new ReflectTypeVariableImpl[0]);
        }
        return typeParameters.length > 0 ? typeParameters.clone() : typeParameters;
    }

    abstract ReflectParameterizedMemberImpl getEnclosingMember();

    Map<String, ReflectTypeVariableImpl> getTypeVariableMap() {
        if (typeVariableMap == null) {
            GenericTypeParameter[] parameters = getUnderlyingTypeParameters();
            if (parameters.length == 0) {
                typeVariableMap = ReflectContext.EMPTY_TYPE_VARIABLES;
            } else {
                typeVariableMap = new LinkedHashMap<>();
                ReflectParameterizedMemberImpl enclosing = getEnclosingMember();
                if (enclosing != null) {
                    typeVariableMap.putAll(enclosing.getTypeVariableMap());
                }
                for (GenericTypeParameter parameter : parameters) {
                    GenericValueType.Reference[] interfaceBounds = parameter.getInterfaceBounds();
                    GenericValueType.Reference classBound = parameter.getClassBound();
                    GenericValueType.Reference[] bounds;
                    if (classBound != null) {
                        bounds = new GenericValueType.Reference[interfaceBounds.length + 1];
                        bounds[0] = classBound;
                        System.arraycopy(interfaceBounds, 0, bounds, 1, interfaceBounds.length);
                    } else {
                        bounds = interfaceBounds;
                    }
                    ReflectTypeVariableImpl variable = new ReflectTypeVariableImpl(getReflectContext(), this,
                            parameter.getName(), bounds);
                    typeVariableMap.put(parameter.getName(), variable);
                }
            }
        }
        return typeVariableMap;
    }
}
