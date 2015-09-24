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
import java.io.StringReader;
import java.util.List;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ast.AstRoot;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.spi.Generator;
import org.teavm.javascript.spi.GeneratorContext;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class JSBodyGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        ClassReader cls = context.getClassSource().get(methodRef.getClassName());
        MethodReader method = cls.getMethod(methodRef.getDescriptor());
        AnnotationReader annot = method.getAnnotations().get(JSBodyImpl.class.getName());
        boolean isStatic = annot.getValue("isStatic").getBoolean();
        List<AnnotationValue> paramNames = annot.getValue("params").getList();
        String script = annot.getValue("script").getString();

        TeaVMErrorReporter errorReporter = new TeaVMErrorReporter(context.getDiagnostics(),
                new CallLocation(methodRef));
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecoverFromErrors(true);
        env.setLanguageVersion(Context.VERSION_1_8);
        env.setIdeMode(true);
        JSParser parser = new JSParser(env, errorReporter);
        parser.enterFunction();
        AstRoot rootNode = parser.parse(new StringReader(script), null, 0);
        parser.exitFunction();
        if (errorReporter.hasErrors()) {
            generateBloated(context, writer, methodRef, method, isStatic, paramNames, script);
            return;
        }

        AstWriter astWriter = new AstWriter(writer);
        int paramIndex = 1;
        if (!isStatic) {
            astWriter.declareAlias("this", context.getParameterName(paramIndex++));
        }
        for (int i = 0; i < paramNames.size(); ++i) {
            astWriter.declareAlias(paramNames.get(i).getString(), context.getParameterName(paramIndex++));
        }
        astWriter.hoist(rootNode);
        astWriter.print(rootNode);
        writer.softNewLine();
    }

    private void generateBloated(GeneratorContext context, SourceWriter writer, MethodReference methodRef,
            MethodReader method, boolean isStatic, List<AnnotationValue> paramNames, String script)
            throws IOException {
        int bodyParamCount = isStatic ? method.parameterCount() : method.parameterCount() - 1;

        writer.append("if (!").appendMethodBody(methodRef).append(".$native)").ws().append('{').indent().newLine();
        writer.appendMethodBody(methodRef).append(".$native").ws().append('=').ws().append("function(");
        int count = method.parameterCount();
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            writer.append('_').append(context.getParameterName(i + 1));
        }
        writer.append(')').ws().append('{').softNewLine().indent();

        writer.append("return (function(");
        for (int i = 0; i < bodyParamCount; ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            String name = paramNames.get(i).getString();
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
            writer.append('_').append(context.getParameterName(i + 1));
        }
        writer.append(");").softNewLine();
        writer.outdent().append("};").softNewLine();
        writer.appendMethodBody(methodRef).ws().append('=').ws().appendMethodBody(methodRef).append(".$native;")
                .softNewLine();
        writer.outdent().append("}").softNewLine();

        writer.append("return ").appendMethodBody(methodRef).append('(');
        for (int i = 0; i < count; ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            writer.append(context.getParameterName(i + 1));
        }
        writer.append(");").softNewLine();
    }
}
