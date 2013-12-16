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
public class FloatNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "isNaN":
                generateIsNaN(context, writer);
                break;
            case "isInfinite":
                generateIsInfinite(context, writer);
                break;
        }
    }

    private void generateIsNaN(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return (isNaN(").append(context.getParameterName(1)).append(")").ws().append("?")
            .ws().append("1").ws().append(":").ws().append("0").ws().append(");").softNewLine();
    }

    private void generateIsInfinite(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return (isFinite(").append(context.getParameterName(1)).append(")").ws().append("?")
                .ws().append("0").ws().append(":").ws().append("1").append(");").softNewLine();
    }
}
