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
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.DependencyType;
import org.teavm.dependency.DependencyTypeFilter;
import org.teavm.dependency.MethodDependency;
import org.teavm.javascript.spi.Generator;
import org.teavm.javascript.spi.GeneratorContext;
import org.teavm.model.*;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev
 */
public class AsyncMethodGenerator implements Generator, DependencyPlugin {
    private static final MethodReference completeMethod = new MethodReference(AsyncCallback.class, "complete",
            Object.class, void.class);
    private static final MethodReference errorMethod = new MethodReference(AsyncCallback.class, "error",
            Throwable.class, void.class);

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        MethodReference asyncRef = getAsyncReference(methodRef);
        writer.append("var thread").ws().append('=').ws().append("$rt_nativeThread();").softNewLine();
        writer.append("if").ws().append("(thread.isResuming())").ws().append("{").indent().softNewLine();
        writer.append("thread.status").ws().append("=").ws().append("0;").softNewLine();
        writer.append("var result").ws().append("=").ws().append("thread.attribute;").softNewLine();
        writer.append("if").ws().append("(result instanceof Error)").ws().append("{").indent().softNewLine();
        writer.append("throw result;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
        writer.outdent().append("}").softNewLine();

        writer.append("var callback").ws().append("=").ws().append("function()").ws().append("{};").softNewLine();
        writer.append("callback.").appendMethod(completeMethod.getDescriptor()).ws().append("=").ws()
                .append("function(val)").ws().append("{").indent().softNewLine();
        writer.append("thread.attribute").ws().append('=').ws().append("val;").softNewLine();
        writer.append("thread.resume();").softNewLine();
        writer.outdent().append("};").softNewLine();
        writer.append("callback.").appendMethod(errorMethod.getDescriptor()).ws().append("=").ws()
                .append("function(e)").ws().append("{").indent().softNewLine();
        writer.append("thread.attribute").ws().append('=').ws().append("$rt_exception(e);").softNewLine();
        writer.append("thread.resume();").softNewLine();
        writer.outdent().append("};").softNewLine();
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
        writer.append("callback.").appendMethod(errorMethod.getDescriptor()).append("($rt_exception($e));")
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
    public void methodAchieved(final DependencyAgent checker, final MethodDependency method, CallLocation location) {
        MethodReference asyncRef = getAsyncReference(method.getReference());
        MethodDependency asyncMethod = checker.linkMethod(asyncRef, location);
        int paramCount = method.getReference().parameterCount();
        for (int i = 0; i <= paramCount; ++i) {
            method.getVariable(i).connect(asyncMethod.getVariable(i));
        }
        asyncMethod.getVariable(paramCount + 1).propagate(checker.getType(FakeAsyncCallback.class.getName()));

        if (method.getResult() != null) {
            MethodDependency completeMethod = checker.linkMethod(
                    new MethodReference(FakeAsyncCallback.class, "complete", Object.class, void.class), null);
            completeMethod.getVariable(1).connect(method.getResult(), new DependencyTypeFilter() {
                @Override
                public boolean match(DependencyType type) {
                    return isSubtype(checker.getClassSource(), type.getName(), method.getReference().getReturnType());
                }
            });
        }

        MethodDependency errorMethod = checker.linkMethod(new MethodReference(FakeAsyncCallback.class, "error",
                Throwable.class, void.class), null);
        errorMethod.getVariable(1).connect(method.getThrown());

        asyncMethod.use();
    }

    private boolean isSubtype(ClassReaderSource classSource, String className, ValueType returnType) {
        if (returnType instanceof ValueType.Primitive) {
            return false;
        } else if (returnType instanceof ValueType.Array) {
            return className.startsWith("[");
        } else {
            return isSubclass(classSource, className, ((ValueType.Object)returnType).getClassName());
        }
    }

    private boolean isSubclass(ClassReaderSource classSource, String className, String baseClass) {
        if (className.equals(baseClass)) {
            return true;
        }
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return false;
        }
        if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
            if (isSubclass(classSource, cls.getParent(), baseClass)) {
                return true;
            }
        }
        for (String iface : cls.getInterfaces()) {
            if (isSubclass(classSource, iface, baseClass)) {
                return true;
            }
        }
        return false;
    }
}
