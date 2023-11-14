/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.platform.plugin;

import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.spi.VirtualMethodContributor;
import org.teavm.backend.javascript.spi.VirtualMethodContributorContext;
import org.teavm.backend.javascript.templating.JavaScriptTemplate;
import org.teavm.backend.javascript.templating.JavaScriptTemplateFactory;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.interop.AsyncCallback;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class AsyncMethodGenerator implements Generator, DependencyPlugin, VirtualMethodContributor {
    private static final MethodDescriptor completeMethod = new MethodDescriptor("complete", Object.class, void.class);
    private static final MethodDescriptor errorMethod = new MethodDescriptor("error", Throwable.class, void.class);
    private JavaScriptTemplate template;

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        if (template == null) {
            var templateFactory = new JavaScriptTemplateFactory(context.getClassLoader(), context.getClassSource());
            template = templateFactory.createFromResource("org/teavm/platform/plugin/Async.js");
        }
        MethodReference asyncRef = getAsyncReference(context.getClassSource(), methodRef);
        template.builder("asyncMethod")
                .withContext(context)
                .withFragment("callMethod", (w, p) -> {
                    w.appendMethodBody(asyncRef).append('(');
                    ClassReader cls = context.getClassSource().get(methodRef.getClassName());
                    MethodReader method = cls.getMethod(methodRef.getDescriptor());
                    int start = method.hasModifier(ElementModifier.STATIC) ? 1 : 0;
                    for (int i = start; i <= methodRef.parameterCount(); ++i) {
                        w.append(context.getParameterName(i));
                        w.append(',').ws();
                    }
                    w.append("callback);").softNewLine();
                })
                .build()
                .write(writer, 0);
    }

    private MethodReference getAsyncReference(ClassReaderSource classSource, MethodReference methodRef) {
        var method = classSource.resolve(methodRef);
        var callerAnnot = method.getAnnotations().get(AsyncCaller.class.getName());
        return MethodReference.parse(callerAnnot.getValue("value").getString());
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        MethodReference ref = method.getReference();
        MethodReference asyncRef = getAsyncReference(agent.getClassSource(), ref);
        MethodDependency asyncMethod = agent.linkMethod(asyncRef);
        method.addLocationListener(asyncMethod::addLocation);
        int paramCount = ref.parameterCount();
        for (int i = 0; i <= paramCount; ++i) {
            method.getVariable(i).connect(asyncMethod.getVariable(i));
        }
        asyncMethod.getVariable(paramCount + 1).propagate(agent.getType(AsyncCallbackWrapper.class.getName()));

        MethodDependency completeMethod = agent.linkMethod(
                new MethodReference(AsyncCallbackWrapper.class, "complete", Object.class, void.class));
        if (method.getResult() != null) {
            completeMethod.getVariable(1).connect(method.getResult(), type -> agent.getClassHierarchy()
                    .isSuperType(ref.getReturnType(), ValueType.object(type.getName()), false));
        }
        completeMethod.use();

        MethodDependency errorMethod = agent.linkMethod(new MethodReference(AsyncCallbackWrapper.class, "error",
                Throwable.class, void.class));
        errorMethod.getVariable(1).connect(method.getThrown());
        errorMethod.use();

        agent.linkMethod(new MethodReference(AsyncCallbackWrapper.class, "create",
                AsyncCallback.class, AsyncCallbackWrapper.class)).use();

        asyncMethod.use();
    }

    @Override
    public boolean isVirtual(VirtualMethodContributorContext context, MethodReference methodRef) {
        ClassReader cls = context.getClassSource().get(methodRef.getClassName());
        if (cls == null) {
            return false;
        }
        if (!cls.getInterfaces().contains(AsyncCallback.class.getName())) {
            return false;
        }
        return methodRef.getDescriptor().equals(completeMethod) || methodRef.getDescriptor().equals(errorMethod);
    }
}
