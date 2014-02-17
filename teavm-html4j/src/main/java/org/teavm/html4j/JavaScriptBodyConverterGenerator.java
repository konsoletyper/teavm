/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.html4j;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyChecker;
import org.teavm.dependency.DependencyConsumer;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodGraph;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class JavaScriptBodyConverterGenerator implements Generator, DependencyPlugin {
    private static final MethodReference intValueMethod = new MethodReference("java.lang.Integer",
            new MethodDescriptor("intValue", ValueType.INTEGER));

    @Override
    public void methodAchieved(DependencyChecker checker, MethodReference method) {
        switch (method.getName()) {
            case "toJavaScript":
                achieveToJavaScript(checker, method);
                break;
        }
    }

    private void achieveToJavaScript(final DependencyChecker checker, MethodReference method) {
        MethodGraph graph = checker.attachMethodGraph(method);
        graph.getVariable(1).addConsumer(new DependencyConsumer() {
            @Override public void consume(String type) {
                if (type.equals("java.lang.Integer")) {
                    checker.attachMethodGraph(intValueMethod);
                }
            }
        });
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "toJavaScript":
                generateToJavaScript(context, writer);
                break;
        }
    }

    private void generateToJavaScript(GeneratorContext context, SourceWriter writer) throws IOException {
        ClassReaderSource classSource = context.getClassSource();
        String obj = context.getParameterName(1);
        writer.append("if").ws().append("(").append(obj).ws().append("===").ws().append("null)").ws().append("{")
                .softNewLine().indent();
        writer.append("return null;").softNewLine();
        writer.outdent().append("}").ws().append("else if").ws().append('(').append(obj)
                .append(".constructor.$meta.item)").ws().append("{").indent().softNewLine();
        writer.append("return ").append(obj).append(".data;").softNewLine();
        writer.outdent().append("}");
        if (classSource.get("java.lang.String") != null) {
            writer.ws().append("else if").ws().append("(").append(obj).append(".constructor").ws().append("===").ws()
                    .appendClass("java.lang.String").append(")").ws().append("{").indent().softNewLine();
            generateStringToJavaScript(context, writer);
            writer.outdent().append("}");
        }
        if (classSource.get("java.lang.Integer") != null) {
            writer.ws().append("else if").ws().append("(").append(obj).append(".constructor").ws().append("===").ws()
                    .appendClass("java.lang.Integer").append(")").ws().append("{").indent().softNewLine();
            writer.append("return ").appendMethodBody(intValueMethod).append("(").append(obj)
                    .append(");").softNewLine();
            writer.outdent().append("}");
        }
        writer.ws().append("else").ws().append("{").indent().softNewLine();
        writer.append("return ").append(obj).append(";").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateStringToJavaScript(GeneratorContext context, SourceWriter writer) throws IOException {
        FieldReference charsField = new FieldReference("java.lang.String", "characters");
        writer.append("var result = \"\";").softNewLine();
        writer.append("var data = ").append(context.getParameterName(1)).append('.')
                .appendField(charsField).append(".data;").softNewLine();
        writer.append("for (var i = 0; i < data.length; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("result += String.fromCharCode(data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
    }
}
