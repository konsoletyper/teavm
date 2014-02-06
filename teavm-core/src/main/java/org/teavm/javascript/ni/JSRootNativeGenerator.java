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
public class JSRootNativeGenerator implements Generator {
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
        }
    }

    private void generateWrapString(GeneratorContext context, SourceWriter writer) throws IOException {
        FieldReference charsField = new FieldReference("java.lang.String", "characters");
        writer.append("var result = \"\";").softNewLine();
        writer.append("var data = ").append(context.getParameterName(1))
                .appendField(charsField).append(".data;").softNewLine();
        writer.append("for (var i = 0; i < data.length; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("result += String.fromCharCode(data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
    }
}
