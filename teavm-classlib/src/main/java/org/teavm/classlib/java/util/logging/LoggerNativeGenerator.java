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
package org.teavm.classlib.java.util.logging;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class LoggerNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        writer.append("if (console) {").indent().softNewLine();
        writer.append("console.").append(methodRef.getName()).append("($rt_ustr(").append(context.getParameterName(1))
                .append("));").softNewLine();
        writer.outdent().append("}").softNewLine();
    }
}
