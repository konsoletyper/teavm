package org.teavm.classlib.java.lang;

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
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
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

    private void generateIsInstance(GeneratorContext context, SourceWriter writer) {
        writer.append("return $rt_isInstance(").append(context.getParameterName(1)).append(", ")
                .append(context.getParameterName(0)).append(".$data);").newLine();
    }

    private void generateIsAssignableFrom(GeneratorContext context, SourceWriter writer) {
        writer.append("return $rt_isAssignable(").append(context.getParameterName(1)).append(".$data, ")
                .append(context.getParameterName(0)).append(".$data;").newLine();
    }

    private void generateGetComponentType(GeneratorContext context, SourceWriter writer) {
        String thisArg = context.getParameterName(0);
        writer.append("var item = " + thisArg + ".$data.$meta.item;").newLine();
        writer.append("return item != null ? $rt_cls(item) : null;").newLine();
    }
}
