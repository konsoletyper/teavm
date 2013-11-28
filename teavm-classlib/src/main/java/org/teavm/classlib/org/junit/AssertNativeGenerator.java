package org.teavm.classlib.org.junit;

import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class AssertNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        switch (methodRef.getDescriptor().getName()) {
            case "fail":
                generateFail(writer);
                break;
        }
    }

    private void generateFail(SourceWriter writer) {
        writer.append("throw new Error();").newLine();
    }
}
