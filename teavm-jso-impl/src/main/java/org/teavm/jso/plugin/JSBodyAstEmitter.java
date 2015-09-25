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
package org.teavm.jso.plugin;

import java.io.IOException;
import org.mozilla.javascript.ast.AstNode;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.spi.GeneratorContext;
import org.teavm.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
class JSBodyAstEmitter implements JSBodyEmitter {
    private boolean isStatic;
    private AstNode ast;
    private String[] parameterNames;

    public JSBodyAstEmitter(boolean isStatic, AstNode ast, String[] parameterNames) {
        this.isStatic = isStatic;
        this.ast = ast;
        this.parameterNames = parameterNames;
    }

    @Override
    public void emit(InjectorContext context) throws IOException {
        AstWriter astWriter = new AstWriter(null, context.getWriter());
        int paramIndex = 0;
        if (!isStatic) {
            int index = paramIndex++;
            astWriter.declareNameEmitter("this", () -> context.writeExpr(context.getArgument(index)));
        }
        for (int i = 0; i < parameterNames.length; ++i) {
            int index = paramIndex++;
            astWriter.declareNameEmitter(parameterNames[i], () -> context.writeExpr(context.getArgument(index)));
        }
        astWriter.hoist(ast);
        astWriter.print(ast);
    }

    @Override
    public void emit(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        AstWriter astWriter = new AstWriter(context.getDiagnostics(), writer);
        int paramIndex = 1;
        if (!isStatic) {
            int index = paramIndex++;
            astWriter.declareNameEmitter("this", () -> writer.append(context.getParameterName(index)));
        }
        for (int i = 0; i < parameterNames.length; ++i) {
            int index = paramIndex++;
            astWriter.declareNameEmitter(parameterNames[i], () -> writer.append(context.getParameterName(index)));
        }
        astWriter.hoist(ast);
        astWriter.print(ast);
        writer.softNewLine();
    }
}
