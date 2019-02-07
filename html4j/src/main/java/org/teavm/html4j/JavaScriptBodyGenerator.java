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
import java.util.List;
import net.java.html.js.JavaScriptBody;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class JavaScriptBodyGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        MethodReader method = context.getClassSource().get(methodRef.getClassName())
                .getMethod(methodRef.getDescriptor());
        AnnotationReader annot = method.getAnnotations().get(JavaScriptBody.class.getName());
        String body = annot.getValue("body").getString();
        List<AnnotationValue> args = annot.getValue("args").getList();
        AnnotationValue javacall = annot.getValue("javacall");
        writer.append("var result = (function(");
        for (int i = 0; i < args.size(); ++i) {
            if (i > 0) {
                writer.append(",").ws();
            }
            writer.append(args.get(i).getString());
        }
        writer.append(")").ws().append("{").indent().softNewLine();
        if (javacall != null && javacall.getBoolean()) {
            GeneratorJsCallback callbackGen = new GeneratorJsCallback(writer, context, context.getClassSource(),
                    context.getDiagnostics(), methodRef);
            callbackGen.parse(body);
        } else {
            writer.append(body);
        }
        writer.softNewLine();
        writer.outdent().append("}).call(").append(!method.hasModifier(ElementModifier.STATIC)
                ? context.getParameterName(0) : "null");
        for (int i = 0; i < args.size(); ++i) {
            writer.append(",").ws();
            wrapParameter(writer, context.getParameterName(i + 1));
        }
        writer.append(");").softNewLine();
        writer.append("return ");
        unwrapValue(context, writer, method.getResultType());
        writer.append(";").softNewLine();
    }

    private void wrapParameter(SourceWriter writer, String param) throws IOException {
        writer.appendMethodBody(JavaScriptConvGenerator.toJsMethod);
        writer.append("(").append(param).append(")");
    }

    private void unwrapValue(GeneratorContext context, SourceWriter writer, ValueType type) throws IOException {
        writer.appendMethodBody(JavaScriptConvGenerator.fromJsMethod);
        writer.append("(").append("result").append(",").ws();
        context.typeToClassString(writer, type);
        writer.append(")");
    }

    private static class GeneratorJsCallback extends JsCallback {
        private SourceWriter writer;
        private GeneratorContext context;
        private ClassReaderSource classSource;
        private Diagnostics diagnostics;
        private MethodReference methodReference;

        GeneratorJsCallback(SourceWriter writer, GeneratorContext context, ClassReaderSource classSource,
                Diagnostics diagnostics, MethodReference methodReference) {
            this.writer = writer;
            this.context = context;
            this.classSource = classSource;
            this.diagnostics = diagnostics;
            this.methodReference = methodReference;
        }

        @Override
        protected void callMethod(String ident, String fqn, String method, String params) {
            try {
                MethodDescriptor desc = MethodDescriptor.parse(method + params + "V");
                MethodReader reader = JavaScriptBodyDependency.findMethod(classSource, fqn, desc);
                if (reader == null) {
                    return;
                }
                desc = reader.getDescriptor();

                writer.append("(function(");
                if (ident != null) {
                    writer.append("$this");
                }
                for (int i = 0; i < desc.parameterCount(); ++i) {
                    if (ident != null || i > 0) {
                        writer.append(",").ws();
                    }
                    writer.append("p").append(i);
                }
                writer.append(")").ws().append("{").ws().append("return ")
                        .appendMethodBody(JavaScriptConvGenerator.toJsMethod).append("(");
                if (ident == null) {
                    writer.appendMethodBody(reader.getReference());
                } else {
                    writer.append("$this.").appendMethod(desc);
                }
                writer.append("(");
                for (int i = 0; i < desc.parameterCount(); ++i) {
                    if (i > 0) {
                        writer.append(",").ws();
                    }
                    ValueType paramType = simplifyParamType(desc.parameterType(i));
                    writer.appendMethodBody(JavaScriptConvGenerator.fromJsMethod).append("(p").append(i)
                            .append(",").ws();
                    context.typeToClassString(writer, paramType);
                    writer.append(")");
                }
                writer.append("));").ws().append("})(");
                if (ident != null) {
                    writer.append(ident);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void append(String text) {
            try {
                writer.append(text);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void reportDiagnostic(String text) {
            diagnostics.error(new CallLocation(methodReference), text);
        }

        private ValueType simplifyParamType(ValueType type) {
            if (type instanceof ValueType.Object) {
                return ValueType.object("java.lang.Object");
            } else if (type instanceof ValueType.Array) {
                ValueType.Array array = (ValueType.Array) type;
                return ValueType.arrayOf(simplifyParamType(array.getItemType()));
            } else {
                return type;
            }
        }
    }
}
