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
