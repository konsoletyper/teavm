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
import org.teavm.backend.javascript.codegen.NamingStrategy;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReader;
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
        if (javacall != null && javacall.getBoolean()) {
            GeneratorJsCallback callbackGen = new GeneratorJsCallback(context, context.getClassSource(),
                    writer.getNaming());
            body = callbackGen.parse(body);
        }
        writer.append("var result = (function(");
        for (int i = 0; i < args.size(); ++i) {
            if (i > 0) {
                writer.append(",").ws();
            }
            writer.append(args.get(i).getString());
        }
        writer.append(")").ws().append("{").indent().softNewLine();
        writer.append(body).softNewLine();
        writer.outdent().append("}).call(").append(!method.hasModifier(ElementModifier.STATIC)
                ? context.getParameterName(0) : "null");
        for (int i = 0; i < args.size(); ++i) {
            writer.append(",").ws();
            wrapParameter(writer, context.getParameterName(i + 1));
        }
        writer.append(");").softNewLine();
        writer.append("return ");
        unwrapValue(context, writer, method.getResultType(), "result");
        writer.append(";").softNewLine();
    }

    private void wrapParameter(SourceWriter writer, String param) throws IOException {
        writer.appendMethodBody(JavaScriptConvGenerator.toJsMethod);
        writer.append("(").append(param).append(")");
    }

    private void unwrapValue(GeneratorContext context, SourceWriter writer, ValueType type, String param)
            throws IOException {
        writer.appendMethodBody(JavaScriptConvGenerator.fromJsMethod);
        writer.append("(").append(param).append(",").ws().append(context.typeToClassString(type))
                .append(")");
    }

    private static class GeneratorJsCallback extends JsCallback {
        private GeneratorContext context;
        private ClassReaderSource classSource;
        private NamingStrategy naming;
        public GeneratorJsCallback(GeneratorContext context, ClassReaderSource classSource, NamingStrategy naming) {
            this.context = context;
            this.classSource = classSource;
            this.naming = naming;
        }
        @Override protected CharSequence callMethod(String ident, String fqn, String method, String params) {
            MethodDescriptor desc = MethodDescriptor.parse(method + params + "V");
            MethodReader reader = findMethod(fqn, desc);
            StringBuilder sb = new StringBuilder();
            sb.append("(function(");
            if (ident != null) {
                sb.append("$this");
            }
            for (int i = 0; i < reader.parameterCount(); ++i) {
                if (ident != null || i > 0) {
                    sb.append(", ");
                }
                sb.append("p").append(i);
            }
            sb.append(") { return ").append(naming.getFullNameFor(JavaScriptConvGenerator.toJsMethod)).append("(");
            if (ident == null) {
                sb.append(naming.getFullNameFor(reader.getReference()));
            } else {
                sb.append("$this.").append(naming.getNameFor(reader.getDescriptor()));
            }
            sb.append("(");
            for (int i = 0; i < reader.parameterCount(); ++i) {
                if (i > 0) {
                    sb.append(", ");
                }
                ValueType paramType = simplifyParamType(reader.parameterType(i));
                sb.append(naming.getFullNameFor(JavaScriptConvGenerator.fromJsMethod)).append("(p").append(i)
                        .append(", ")
                        .append(context.typeToClassString(paramType)).append(")");
            }
            sb.append(")); })(");
            if (ident != null) {
                sb.append(ident);
            }
            return sb.toString();
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
        private MethodReader findMethod(String clsName, MethodDescriptor desc) {
            while (clsName != null) {
                ClassReader cls = classSource.get(clsName);
                for (MethodReader method : cls.getMethods()) {
                    if (method.getName().equals(desc.getName()) && sameParams(method.getDescriptor(), desc)) {
                        return method;
                    }
                }
                clsName = cls.getParent();
            }
            return null;
        }
        private boolean sameParams(MethodDescriptor a, MethodDescriptor b) {
            if (a.parameterCount() != b.parameterCount()) {
                return false;
            }
            for (int i = 0; i < a.parameterCount(); ++i) {
                if (!a.parameterType(i).equals(b.parameterType(i))) {
                    return false;
                }
            }
            return true;
        }
    }
}
