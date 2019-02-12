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

import java.io.IOException;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.spi.VirtualMethodContributor;
import org.teavm.backend.javascript.spi.VirtualMethodContributorContext;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.platform.async.AsyncCallback;

public class AsyncMethodGenerator implements Generator, DependencyPlugin, VirtualMethodContributor {
    private static final MethodDescriptor completeMethod = new MethodDescriptor("complete", Object.class, void.class);
    private static final MethodDescriptor errorMethod = new MethodDescriptor("error", Throwable.class, void.class);

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        MethodReference asyncRef = getAsyncReference(methodRef);
        writer.append("var thread").ws().append('=').ws().append("$rt_nativeThread();").softNewLine();
        writer.append("var javaThread").ws().append('=').ws().append("$rt_getThread();").softNewLine();
        writer.append("if").ws().append("(thread.isResuming())").ws().append("{").indent().softNewLine();
        writer.append("thread.status").ws().append("=").ws().append("0;").softNewLine();
        writer.append("var result").ws().append("=").ws().append("thread.attribute;").softNewLine();
        writer.append("if").ws().append("(result instanceof Error)").ws().append("{").indent().softNewLine();
        writer.append("throw result;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
        writer.outdent().append("}").softNewLine();

        writer.append("var callback").ws().append("=").ws().append("function()").ws().append("{};").softNewLine();
        writer.append("callback.").appendMethod(completeMethod).ws().append("=").ws()
                .append("function(val)").ws().append("{").indent().softNewLine();
        writer.append("thread.attribute").ws().append('=').ws().append("val;").softNewLine();
        writer.append("$rt_setThread(javaThread);").softNewLine();
        writer.append("thread.resume();").softNewLine();
        writer.outdent().append("};").softNewLine();
        writer.append("callback.").appendMethod(errorMethod).ws().append("=").ws()
                .append("function(e)").ws().append("{").indent().softNewLine();
        writer.append("thread.attribute").ws().append('=').ws().append("$rt_exception(e);").softNewLine();
        writer.append("$rt_setThread(javaThread);").softNewLine();
        writer.append("thread.resume();").softNewLine();
        writer.outdent().append("};").softNewLine();
        writer.append("callback").ws().append("=").ws().appendMethodBody(AsyncCallbackWrapper.class, "create",
                AsyncCallback.class, AsyncCallbackWrapper.class).append("(callback);").softNewLine();
        writer.append("return thread.suspend(function()").ws().append("{").indent().softNewLine();
        writer.append("try").ws().append("{").indent().softNewLine();
        writer.appendMethodBody(asyncRef).append('(');
        ClassReader cls = context.getClassSource().get(methodRef.getClassName());
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        int start = method.hasModifier(ElementModifier.STATIC) ? 1 : 0;
        for (int i = start; i <= methodRef.parameterCount(); ++i) {
            writer.append(context.getParameterName(i));
            writer.append(',').ws();
        }
        writer.append("callback);").softNewLine();
        writer.outdent().append("}").ws().append("catch($e)").ws().append("{").indent().softNewLine();
        writer.append("callback.").appendMethod(errorMethod).append("($rt_exception($e));")
                .softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.outdent().append("});").softNewLine();
    }

    private MethodReference getAsyncReference(MethodReference methodRef) {
        ValueType[] signature = new ValueType[methodRef.parameterCount() + 2];
        for (int i = 0; i < methodRef.parameterCount(); ++i) {
            signature[i] = methodRef.getDescriptor().parameterType(i);
        }
        signature[methodRef.parameterCount()] = ValueType.parse(AsyncCallback.class);
        signature[methodRef.parameterCount() + 1] = ValueType.VOID;
        return new MethodReference(methodRef.getClassName(), methodRef.getName(), signature);
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        MethodReference ref = method.getReference();
        MethodReference asyncRef = getAsyncReference(ref);
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
