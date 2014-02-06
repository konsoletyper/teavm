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
package org.teavm.javascript.ni;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class JSNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef)
            throws IOException {
        switch (methodRef.getName()) {
            case "getTypeName":
                writer.append("return typeof ").append(context.getParameterName(1)).append(";").softNewLine();
                break;
            case "getGlobal":
                writer.append("return window;").softNewLine();
                break;
            case "wrap":
                if (methodRef.getParameterTypes()[0].isObject("java.lang.String")) {
                    generateWrapString(context, writer);
                } else {
                    writer.append("return ").append(context.getParameterName(1)).append(";").softNewLine();
                }
                break;
            case "get":
                writer.append("return ").append(context.getParameterName(1)).append("[")
                        .append(context.getParameterName(2)).append("];").softNewLine();
                break;
            case "set":
                writer.append(context.getParameterName(1)).append("[").append(context.getParameterName(2))
                        .append("] = ").append(context.getParameterName(3)).softNewLine();
                break;
            case "invoke":
                generateInvoke(context, writer, methodRef.parameterCount() - 2);
                break;
            case "isUndefined":
                writer.append("return ").append(context.getParameterName(1)).append(" === undefined;");
                break;
            default:
                if (methodRef.getName().startsWith("unwrap")) {
                    if (methodRef.getDescriptor().getResultType().isObject("java.lang.String")) {
                        writer.append("return $rt_str(").append(context.getParameterName(1)).append(");")
                                .softNewLine();
                    } else {
                        writer.append("return ").append(context.getParameterName(1)).append(";").softNewLine();
                    }
                }
                break;
        }
    }

    private void generateWrapString(GeneratorContext context, SourceWriter writer) throws IOException {
        FieldReference charsField = new FieldReference("java.lang.String", "characters");
        writer.append("var result = \"\";").softNewLine();
        writer.append("var data = ").append(context.getParameterName(1)).append('.')
                .appendField(charsField).append(".data;").softNewLine();
        writer.append("for (var i = 0; i < data.length; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("result += String.fromCharCode(data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
    }

    private void generateInvoke(GeneratorContext context, SourceWriter writer, int argNum) throws IOException {
        writer.append("return ").append(context.getParameterName(1)).append("[")
                .append(context.getParameterName(2)).append("](");
        for (int i = 0; i < argNum; ++i) {
            if (i > 0) {
                writer.append(",").ws();
            }
            writer.append(context.getParameterName(i + 3));
        }
        writer.append(");").softNewLine();
    }
}
