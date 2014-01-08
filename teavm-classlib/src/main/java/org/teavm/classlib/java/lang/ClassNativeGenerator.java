/*
 *  Copyright 2013 Alexey Andreev.
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

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClassNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef)
            throws IOException {
        switch (methodRef.getName()) {
            case "isInstance":
                generateIsInstance(context, writer);
                break;
            case "isAssignable":
                generateIsAssignableFrom(context, writer);
                break;
            case "getComponentType0":
                generateGetComponentType(context, writer);
                break;
        }
    }

    private void generateIsInstance(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return $rt_isInstance(").append(context.getParameterName(1)).append(", ")
                .append(context.getParameterName(0)).append(".$data);").softNewLine();
    }

    private void generateIsAssignableFrom(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return $rt_isAssignable(").append(context.getParameterName(1)).append(".$data, ")
                .append(context.getParameterName(0)).append(".$data;").softNewLine();
    }

    private void generateGetComponentType(GeneratorContext context, SourceWriter writer) throws IOException {
        String thisArg = context.getParameterName(0);
        writer.append("var item = " + thisArg + ".$data.$meta.item;").softNewLine();
        writer.append("return item != null ? $rt_cls(item) : null;").softNewLine();
    }
}
