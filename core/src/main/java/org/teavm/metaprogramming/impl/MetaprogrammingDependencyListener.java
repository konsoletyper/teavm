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
package org.teavm.metaprogramming.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.metaprogramming.impl.model.MethodDescriber;
import org.teavm.metaprogramming.impl.model.MethodModel;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.StringChooseEmitter;
import org.teavm.model.emit.ValueEmitter;

public class MetaprogrammingDependencyListener extends AbstractDependencyListener {
    private MethodDescriber describer;
    private Set<MethodModel> installedProxies = new HashSet<>();
    private MetaprogrammingClassLoader proxyClassLoader;

    @Override
    public void started(DependencyAgent agent) {
        proxyClassLoader = new MetaprogrammingClassLoader(agent.getClassLoader());
        describer = new MethodDescriber(agent.getDiagnostics(), agent.getClassSource());
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency methodDep, CallLocation location) {
        MethodModel proxy = describer.getMethod(methodDep.getReference());
        if (proxy != null && installedProxies.add(proxy)) {
            new UsageGenerator(agent, proxy, methodDep, location, proxyClassLoader).installProxyEmitter();
        }
    }

    @Override
    public void completing(DependencyAgent agent) {
        for (MethodModel model : describer.getKnownMethods()) {
            ProgramEmitter pe = ProgramEmitter.create(model.getMethod().getDescriptor(), agent.getClassSource());

            ValueEmitter[] paramVars = new ValueEmitter[model.getMetaParameterCount()];
            for (int i = 0; i < paramVars.length; ++i) {
                paramVars[i] = pe.var(i, model.getMetaParameterType(i));
            }

            if (model.getUsages().size() == 1) {
                emitSingleUsage(model, pe, agent, paramVars);
            } else if (model.getUsages().isEmpty()) {
                if (model.getMethod().getReturnType() == ValueType.VOID) {
                    pe.exit();
                } else {
                    pe.constantNull(Object.class).returnValue();
                }
            } else {
                emitMultipleUsage(model, pe, agent, paramVars);
            }
        }
    }

    private void emitSingleUsage(MethodModel model, ProgramEmitter pe, DependencyAgent agent,
            ValueEmitter[] paramVars) {
        MethodReference usage = model.getUsages().values().stream().findFirst().get();
        ValueEmitter result = pe.invoke(usage, paramVars);
        if (usage.getReturnType() == ValueType.VOID) {
            pe.exit();
        } else {
            assert result != null : "Expected non-null result at " + model.getMethod();
            result.returnValue();
        }
        agent.submitMethod(model.getMethod(), pe.getProgram());
    }

    private void emitMultipleUsage(MethodModel model, ProgramEmitter pe, DependencyAgent agent,
            ValueEmitter[] paramVars) {
        MethodDependencyInfo methodDep = agent.getMethod(model.getMethod());
        ValueEmitter paramVar = paramVars[model.getMetaClassParameterIndex()];
        ValueEmitter tag = paramVar.invokeVirtual("getClass", Class.class).invokeVirtual("getName", String.class);

        StringChooseEmitter choice = pe.stringChoice(tag);
        for (Map.Entry<ValueType, MethodReference> usageEntry : model.getUsages().entrySet()) {
            ValueType type = usageEntry.getKey();
            String typeName = type instanceof ValueType.Object
                    ? ((ValueType.Object) type).getClassName()
                    : type.toString();
            choice.option(typeName, () -> {
                MethodReference implMethod = usageEntry.getValue();
                ValueEmitter[] castParamVars = new ValueEmitter[paramVars.length];
                for (int i = 0; i < castParamVars.length; ++i) {
                    castParamVars[i] = paramVars[i].cast(implMethod.parameterType(i));
                }
                ValueEmitter result = pe.invoke(implMethod, castParamVars);
                if (implMethod.getReturnType() == ValueType.VOID) {
                    pe.exit();
                } else {
                    assert result != null : "Expected non-null result at " + model.getMethod();
                    result.returnValue();
                }
            });
        }

        choice.otherwise(() -> {
            if (methodDep.getReference().getReturnType() == ValueType.VOID) {
                pe.exit();
            } else {
                pe.constantNull(Object.class).returnValue();
            }
        });

        agent.submitMethod(model.getMethod(), pe.getProgram());
    }
}
