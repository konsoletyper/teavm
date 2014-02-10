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
package org.teavm.classlib.java.lang;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev
 */
public class CharacterNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "toLowerCase":
                if (methodRef.getDescriptor().parameterType(0) == ValueType.CHARACTER) {
                    generateToLowerCase(context, writer);
                } else {
                    generateToLowerCaseInt(context, writer);
                }
                break;
        }
    }

    private void generateToLowerCase(GeneratorContext context, SourceWriter writer) throws IOException{
        writer.append("return String.fromCharCode(").append(context.getParameterName(1))
                .append(").toLowerCase().charCodeAt(0)|0;").softNewLine();
    }

    private void generateToLowerCaseInt(GeneratorContext context, SourceWriter writer) throws IOException{
        writer.append("return String.fromCharCode(").append(context.getParameterName(1))
                .append(").toLowerCase().charCodeAt(0);").softNewLine();
    }
}
