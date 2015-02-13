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
import org.teavm.dependency.MethodDependency;
import org.teavm.javascript.spi.Generator;
import org.teavm.javascript.spi.GeneratorContext;
import org.teavm.model.*;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class AsyncMethodGenerator implements Generator, DependencyPlugin {
    private static final MethodReference completeMethod = new MethodReference(AsyncCallback.class, "complete",
            Object.class, void.class);
    private static final MethodReference errorMethod = new MethodReference(AsyncCallback.class, "error",
            Throwable.class, void.class);

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        MethodReference asyncRef = getAsyncReference(methodRef);
        writer.append("var callback").ws().append("=").ws().append("function()").ws().append("{};").softNewLine();
        writer.append("callback.").appendMethod(completeMethod).ws().append("=").ws().append("function($this,").ws()
                .append("val)").ws().append("{").indent().softNewLine();
        writer.append("return $return($rt_asyncResult(val));").softNewLine();
        writer.outdent().append("};").softNewLine();
        writer.append("callback.").appendMethod(errorMethod).ws().append("=").ws().append("function($this,").ws()
                .append("e)").ws().append("{").indent().softNewLine();
        writer.append("return $return($rt_asyncError(e));").softNewLine();
        writer.outdent().append("};").softNewLine();
        writer.append("try").ws().append("{").indent().softNewLine();
        writer.append("return ").appendMethodBody(asyncRef).append('(');
        ClassReader cls = context.getClassSource().get(methodRef.getClassName());
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        int start = method.hasModifier(ElementModifier.STATIC) ? 1 : 0;
        for (int i = start; i <= methodRef.parameterCount(); ++i) {
            writer.append(context.getParameterName(i));
            writer.append(',').ws();
        }
        writer.append("callback);").softNewLine();
        writer.outdent().append("}").ws().append("catch($e)").ws().append("{").indent().softNewLine();
        writer.append("return $return($rt_asyncError($e));").softNewLine();
        writer.outdent().append("}").softNewLine();
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
    public void methodAchieved(DependencyAgent checker, MethodDependency method, CallLocation location) {
        MethodReference asyncRef = getAsyncReference(method.getReference());
        MethodDependency asyncMethod = checker.linkMethod(asyncRef, location);
        int paramCount = method.getReference().parameterCount();
        for (int i = 0; i <= paramCount; ++i) {
            method.getVariable(i).connect(asyncMethod.getVariable(i));
        }
        asyncMethod.use();
    }
}
