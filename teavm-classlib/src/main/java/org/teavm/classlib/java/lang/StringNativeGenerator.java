package org.teavm.classlib.java.lang;

import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
public class StringNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "wrap":
                generateWrap(context, writer);
                break;
        }
    }

    private void generateWrap(GeneratorContext context, SourceWriter writer) {
        writer.append("return ").append(context.getParameterName(1)).newLine();
    }
}
