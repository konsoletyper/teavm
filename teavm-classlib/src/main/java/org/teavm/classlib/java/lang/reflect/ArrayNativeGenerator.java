package org.teavm.classlib.java.lang.reflect;

import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyChecker;
import org.teavm.dependency.DependencyConsumer;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodGraph;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev
 */
public class ArrayNativeGenerator implements Generator, DependencyPlugin {
    @Override
    public void methodAchieved(DependencyChecker checker, MethodReference method) {
        if (method.getName().equals("getLength")) {
            achieveGetLength(checker, method);
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "getLength":
                generateGetLength(context, writer);
                break;
        }
    }

    private void generateGetLength(GeneratorContext context, SourceWriter writer) {
        String array = context.getParameterName(1);
        writer.append("if (" + array + " === null || " + array + " .$class.$meta.item === undefined) {")
                .newLine().indent();
        String clsName = "java.lang.IllegalArgumentException";
        MethodReference cons = new MethodReference(clsName, new MethodDescriptor("<init>", ValueType.VOID));
        writer.append("$rt_throw(").appendClass(clsName).append(".").appendMethod(cons).append("());").newLine();
        writer.outdent().append("}").newLine();
        writer.append("return " + array + ".data.length;").newLine();
    }

    private void achieveGetLength(final DependencyChecker checker, MethodReference methodRef) {
        final MethodGraph graph = checker.attachMethodGraph(methodRef);
        graph.getVariableNode(1).addConsumer(new DependencyConsumer() {
            @Override public void consume(String type) {
                if (!type.startsWith("[")) {
                    MethodReference cons = new MethodReference("java.lang.IllegalArgumentException",
                            new MethodDescriptor("<init>", ValueType.VOID));
                    checker.addEntryPoint(cons);
                }
            }
        });
    }
}
