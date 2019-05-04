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
import org.teavm.metaprogramming.reflect.ReflectType;
import org.teavm.metaprogramming.reflect.ReflectTypeArgument;
import org.teavm.metaprogramming.reflect.ReflectVariance;
import org.teavm.model.GenericValueType;

public class ReflectTypeArgumentImpl implements ReflectTypeArgument {
    private ReflectContext context;
    private ReflectVariance variance;
    private GenericValueType innerConstraint;
    private ReflectType constraint;
    private Map<String, ReflectTypeVariableImpl> variableMap;

    public ReflectTypeArgumentImpl(ReflectContext context, ReflectVariance variance,
            GenericValueType innerConstraint, Map<String, ReflectTypeVariableImpl> variableMap) {
        this.context = context;
        this.variance = variance;
        this.innerConstraint = innerConstraint;
        this.variableMap = variableMap;
    }

    @Override
    public ReflectVariance getVariance() {
        return variance;
    }

    @Override
    public ReflectType getConstraint() {
        if (constraint == null) {
            constraint = context.getGenericType(innerConstraint, variableMap);
        }
        return constraint;
    }
}
