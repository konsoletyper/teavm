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
 * @author Alexey Andreev
 */
public class ConsoleOutputStreamGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        if (methodRef.getClassName().endsWith("_stderr")) {
            if (methodRef.getName().equals("write")) {
                writer.append("$rt_putStderr(").append(context.getParameterName(1)).append(");").softNewLine();
            }
        } else if (methodRef.getClassName().endsWith("_stdout")) {
            if (methodRef.getName().equals("write")) {
                writer.append("$rt_putStdout(").append(context.getParameterName(1)).append(");").softNewLine();
            }
        }
    }
}
