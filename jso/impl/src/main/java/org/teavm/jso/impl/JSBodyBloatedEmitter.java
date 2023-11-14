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

import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;

class JSBodyBloatedEmitter implements JSBodyEmitter {
    private boolean isStatic;
    private MethodReference method;
    private String script;
    private String[] parameterNames;
    private JsBodyImportInfo[] imports;

    JSBodyBloatedEmitter(boolean isStatic, MethodReference method, String script, String[] parameterNames,
            JsBodyImportInfo[] imports) {
        this.isStatic = isStatic;
        this.method = method;
        this.script = script;
        this.parameterNames = parameterNames;
        this.imports = imports;
    }

    @Override
    public void emit(InjectorContext context) {
        emit(context.getWriter(), new EmissionStrategy() {
            @Override
            public void emitArgument(int argument) {
                context.writeExpr(context.getArgument(argument));
            }

            @Override
            public void emitModule(String name) {
                context.getWriter().append(context.importModule(name));
            }
        });
    }

    @Override
    public void emit(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        emit(writer, new EmissionStrategy() {
            @Override
            public void emitArgument(int argument) {
                writer.append(context.getParameterName(argument + 1));
            }

            @Override
            public void emitModule(String name) {
                writer.append(context.importModule(name));
            }
        });
    }

    private void emit(SourceWriter writer, EmissionStrategy strategy) {
        int bodyParamCount = isStatic ? method.parameterCount() : method.parameterCount() - 1;

        writer.append("if (!").appendMethodBody(method).append(".$native)").ws().append('{').indent().newLine();
        writer.appendMethodBody(method).append(".$native").ws().append('=').ws().append("function(");
        int count = method.parameterCount();

        var first = true;
        for (int i = 0; i < count; ++i) {
            if (!first) {
                writer.append(',').ws();
            }
            first = false;
            writer.append('_').append(i);
        }
        for (var i = 0; i < imports.length; ++i) {
            if (!first) {
                writer.append(',').ws();
            }
            first = false;
            writer.append("_i").append(i);
        }
        writer.append(')').ws().append('{').softNewLine().indent();

        writer.append("return (function(");

        first = true;
        for (int i = 0; i < bodyParamCount; ++i) {
            if (!first) {
                writer.append(',').ws();
            }
            first = false;
            String name = parameterNames[i];
            writer.append(name);
        }
        for (var importInfo : imports) {
            if (!first) {
                writer.append(',').ws();
            }
            first = false;
            writer.append(importInfo.alias);
        }

        writer.append(')').ws().append('{').softNewLine().indent();
        writer.append(script).softNewLine();
        writer.outdent().append("})");
        if (!isStatic) {
            writer.append(".call");
        }
        writer.append('(');

        first = true;
        for (int i = 0; i < count; ++i) {
            if (!first) {
                writer.append(',').ws();
            }
            first = false;
            writer.append('_').append(i);
        }
        for (var i = 0; i < imports.length; ++i) {
            if (!first) {
                writer.append(',').ws();
            }
            first = false;
            writer.append("_i").append(i);
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
        void emitArgument(int argument);

        void emitModule(String name);
    }
}
