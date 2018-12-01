/*
 *  Copyright 2017 Alexey Andreev.
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
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.model.MethodReference;

public class LongNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "compare":
                writer.append("return Long_compare(").append(context.getParameterName(1)).append(", ")
                        .append(context.getParameterName(2)).append(");").softNewLine();
                context.useLongLibrary();
                break;
            case "divideUnsigned":
                writer.append("return Long_udiv(").append(context.getParameterName(1)).append(", ")
                        .append(context.getParameterName(2)).append(");").softNewLine();
                context.useLongLibrary();
                break;
            case "remainderUnsigned":
                writer.append("return Long_urem(").append(context.getParameterName(1)).append(", ")
                        .append(context.getParameterName(2)).append(");").softNewLine();
                context.useLongLibrary();
                break;
        }
    }
}
