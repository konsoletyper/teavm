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
import org.teavm.metaprogramming.impl.optimization.Optimizations;
import org.teavm.metaprogramming.impl.reflect.ReflectContext;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReader;
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

        MetaprogrammingImpl.classLoader = proxyClassLoader;
        MetaprogrammingImpl.classSource = agent.getClassSource();
        MetaprogrammingImpl.hierarchy = agent.getClassHierarchy();
        MetaprogrammingImpl.incrementalDependencies = agent.getIncrementalCache();
        MetaprogrammingImpl.agent = agent;
        MetaprogrammingImpl.reflectContext = new ReflectContext(agent.getClassHierarchy(), proxyClassLoader);
    }

    @Override
    public void complete() {
        MetaprogrammingImpl.classLoader = null;
        MetaprogrammingImpl.classSource = null;
        MetaprogrammingImpl.hierarchy = null;
        MetaprogrammingImpl.incrementalDependencies = null;
        MetaprogrammingImpl.agent = null;
        MetaprogrammingImpl.reflectContext = null;
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency methodDep) {
        MethodModel proxy = describer.getMethod(methodDep.getReference());
        if (proxy != null && installedProxies.add(proxy)) {
            agent.getIncrementalCache().setNoCache(methodDep.getReference());
            ClassReader cls = agent.getClassSource().get(methodDep.getMethod().getOwnerName());
            int index = 0;
            for (MethodReader method : cls.getMethods()) {
                if (method.getDescriptor().equals(methodDep.getMethod().getDescriptor())) {
                    break;
                }
                ++index;
            }
            new UsageGenerator(agent, proxy, methodDep, proxyClassLoader, index).installProxyEmitter();
        }
    }

    @Override
    public void completing(DependencyAgent agent) {
        for (MethodModel model : describer.getKnownMethods()) {
            ProgramEmitter pe = ProgramEmitter.create(model.getMethod().getDescriptor(), agent.getClassHierarchy());

            ValueEmitter[] paramVars = new ValueEmitter[model.getMetaParameterCount()];
            int offset = model.isStatic() ? 1 : 0;
            for (int i = 0; i < paramVars.length; ++i) {
                paramVars[i] = pe.var(i + offset, model.getMetaParameterType(i));
            }

            if (model.getUsages().size() == 1) {
                emitSingleUsage(model, pe, paramVars);
            } else if (model.getUsages().isEmpty()) {
                if (model.getMethod().getReturnType() == ValueType.VOID) {
                    pe.exit();
                } else {
                    pe.constantNull(Object.class).returnValue();
                }
            } else {
                emitMultipleUsage(model, pe, agent, paramVars);
            }

            agent.submitMethod(model.getMethod(), new Optimizations().apply(pe.getProgram(), model.getMethod()));
        }
    }

    private void emitSingleUsage(MethodModel model, ProgramEmitter pe, ValueEmitter[] paramVars) {
        MethodReference usage = model.getUsages().values().stream().findFirst().orElse(null);
        ValueEmitter result = pe.invoke(usage, paramVars);
        if (usage.getReturnType() == ValueType.VOID) {
            pe.exit();
        } else {
            assert result != null : "Expected non-null result at " + model.getMethod();
            result.returnValue();
        }
    }

    private void emitMultipleUsage(MethodModel model, ProgramEmitter pe, DependencyAgent agent,
            ValueEmitter[] paramVars) {
        MethodDependencyInfo methodDep = agent.getMethod(model.getMethod());
        ValueEmitter paramVar = paramVars[model.getMetaClassParameterIndex()];
        ValueEmitter tag = paramVar.invokeVirtual("getName", String.class);

        StringChooseEmitter choice = pe.stringChoice(tag);
        for (Map.Entry<ValueType, MethodReference> usageEntry : model.getUsages().entrySet()) {
            ValueType type = usageEntry.getKey();
            String typeName = type instanceof ValueType.Object
                    ? ((ValueType.Object) type).getClassName()
                    : type.toString().replace('/', '.');
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
    }
}
