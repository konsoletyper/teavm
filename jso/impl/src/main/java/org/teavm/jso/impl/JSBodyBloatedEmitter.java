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
package org.teavm.jso.impl;

import java.io.IOException;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;

class JSBodyBloatedEmitter implements JSBodyEmitter {
    private boolean isStatic;
    private MethodReference method;
    private String script;
    private String[] parameterNames;

    public JSBodyBloatedEmitter(boolean isStatic, MethodReference method, String script, String[] parameterNames) {
        this.isStatic = isStatic;
        this.method = method;
        this.script = script;
        this.parameterNames = parameterNames;
    }

    @Override
    public void emit(InjectorContext context) throws IOException {
        emit(context.getWriter(), index -> context.writeExpr(context.getArgument(index)));
    }

    @Override
    public void emit(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        emit(writer, index -> writer.append(context.getParameterName(index + 1)));
    }

    private void emit(SourceWriter writer, EmissionStrategy strategy) throws IOException {
        int bodyParamCount = isStatic ? method.parameterCount() : method.parameterCount() - 1;

        writer.append("if (!").appendMethodBody(method).append(".$native)").ws().append('{').indent().newLine();
        writer.appendMethodBody(method).append(".$native").ws().append('=').ws().append("function(");
        int count = method.parameterCount();
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            writer.append('_').append(i);
        }
        writer.append(')').ws().append('{').softNewLine().indent();

        writer.append("return (function(");
        for (int i = 0; i < bodyParamCount; ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            String name = parameterNames[i];
            writer.append(name);
        }
        writer.append(')').ws().append('{').softNewLine().indent();
        writer.append(script).softNewLine();
        writer.outdent().append("})");
        if (!isStatic) {
            writer.append(".call");
        }
        writer.append('(');
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            writer.append('_').append(i);
        }
        writer.append(");").softNewLine();
        writer.outdent().append("};").softNewLine();
        writer.appendMethodBody(method).ws().append('=').ws().appendMethodBody(method).append(".$native;")
                .softNewLine();
        writer.outdent().append("}").softNewLine();

        writer.append("return ").appendMethodBody(method).append('(');
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            strategy.emitArgument(i);
        }
        writer.append(");").softNewLine();
    }

    interface EmissionStrategy {
        void emitArgument(int argument) throws IOException;
    }
}
