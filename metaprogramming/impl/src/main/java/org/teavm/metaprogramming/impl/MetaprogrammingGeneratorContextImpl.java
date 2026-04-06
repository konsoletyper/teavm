/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.metaprogramming.impl;

import java.util.AbstractList;
import java.util.List;
import org.teavm.metaprogramming.MethodGeneratorContext;
import org.teavm.metaprogramming.Value;
import org.teavm.metaprogramming.impl.reflect.ReflectMethodImpl;
import org.teavm.metaprogramming.reflect.ReflectMethodDescriptor;
import org.teavm.model.ElementModifier;
import org.teavm.model.ValueType;

class MetaprogrammingGeneratorContextImpl implements MethodGeneratorContext {
    private ReflectMethodImpl method;
    private Value<?> callReceiverCache;
    private Value<?>[] parameterCache;

    void init(ReflectMethodImpl method, int index) {
        this.method = method;
        var varContext = new TopLevelVariableContext(MetaprogrammingImpl.createDiagnostics());
        MetaprogrammingImpl.generator = new CompositeMethodGenerator(varContext);
        MetaprogrammingImpl.varContext = varContext;
        MetaprogrammingImpl.returnType = method.method.getResultType();
        MetaprogrammingImpl.generator.location = null;
        MetaprogrammingImpl.proxySuffixGenerators.clear();
        MetaprogrammingImpl.suffix = "gen_" + index;
        MetaprogrammingImpl.templateMethod = method.method.getReference();

        for (int i = 0; i <= method.getParameterCount(); ++i) {
            MetaprogrammingImpl.generator.getProgram().createVariable();
        }
        parameterCache = new Value<?>[method.method.parameterCount()];
        for (int i = 0; i < method.getParameterCount(); ++i) {
            var type = method.method.parameterType(i);
            var param = new ValueImpl<>(UsageGenerator.getParameterVar(i, type),
                    MetaprogrammingImpl.varContext, type);
            parameterCache[i] = param;
        }
    }

    void cleanup() {
        method = null;
        callReceiverCache = null;
        parameterCache = null;
        MetaprogrammingImpl.varContext = null;
        MetaprogrammingImpl.generator = null;
    }

    @Override
    public Value<?> callReceiver() {
        if (method.method.hasModifier(ElementModifier.STATIC)) {
            return null;
        }
        var result = callReceiverCache;
        if (callReceiverCache == null) {
            var program = MetaprogrammingImpl.generator.getProgram();
            var variable = program.variableAt(0);
            result = new ValueImpl<>(variable, MetaprogrammingImpl.varContext,
                    ValueType.object(method.method.getOwnerName()));
            callReceiverCache = result;
        }
        return result;
    }

    @Override
    public List<? extends Value<?>> parameters() {
        return parameters;
    }

    @Override
    public ReflectMethodDescriptor method() {
        return method;
    }

    private List<? extends Value<?>> parameters = new AbstractList<>() {
        @Override
        public Value<?> get(int index) {
            return parameterCache[index];
        }

        @Override
        public int size() {
            return method.getParameterCount();
        }
    };
}
