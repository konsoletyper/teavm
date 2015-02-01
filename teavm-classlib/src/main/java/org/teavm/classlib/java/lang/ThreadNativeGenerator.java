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
public class ThreadNativeGenerator  implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        if (methodRef.getName().equals("sleep")) {
            generateSleep(context, writer);
        } else if (methodRef.getName().equals("yield")) {
            generateYield(context, writer);
        }
    }

    private void generateSleep(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("setTimer(function() {").indent().softNewLine();
        writer.append(context.getCompleteContinuation()).append("();").softNewLine();
        writer.outdent().append(',').ws().append(context.getParameterName(1)).append(");").softNewLine();
    }

    private void generateYield(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("setTimer(function() {").indent().softNewLine();
        writer.append(context.getCompleteContinuation()).append("();").softNewLine();
        writer.outdent().append(',').ws().append("0);").softNewLine();
    }
}
