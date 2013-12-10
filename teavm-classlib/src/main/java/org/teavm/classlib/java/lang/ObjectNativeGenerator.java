package org.teavm.classlib.java.lang;

import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyChecker;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodGraph;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ObjectNativeGenerator implements Generator, DependencyPlugin {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        switch (methodRef.getDescriptor().getName()) {
            case "<init>":
                generateInit(context, writer);
                break;
            case "getClass":
                generateGetClass(context, writer);
                break;
            case "hashCode":
                generateHashCode(context, writer);
                break;
            case "clone":
                generateClone(context, writer);
                break;
            case "wrap":
                generateWrap(context, writer);
                break;
        }
    }

    @Override
    public void methodAchieved(DependencyChecker checker, MethodReference method) {
        switch (method.getDescriptor().getName()) {
            case "clone":
                achieveClone(checker, method);
                break;
            case "getClass":
                achieveGetClass(checker);
                break;
            case "wrap":
                achieveWrap(checker, method);
                break;
        }
    }

    private void generateInit(GeneratorContext context, SourceWriter writer) {
        writer.append(context.getParameterName(0)).append(".$id = $rt_nextId();").softNewLine();
    }

    private void generateGetClass(GeneratorContext context, SourceWriter writer) {
        String thisArg = context.getParameterName(0);
        writer.append("return $rt_cls(").append(thisArg).append(".$class);").softNewLine();
    }

    private void achieveGetClass(DependencyChecker checker) {
        String classClass = "java.lang.Class";
        MethodReference method = new MethodReference(classClass, new MethodDescriptor("createNew",
                ValueType.object(classClass)));
        checker.addEntryPoint(method);
    }

    private void generateHashCode(GeneratorContext context, SourceWriter writer) {
        writer.append("return ").append(context.getParameterName(0)).append(".$id;").softNewLine();
    }

    private void generateClone(GeneratorContext context, SourceWriter writer) {
        writer.append("var copy = new ").append(context.getParameterName(0)).append(".$class();").softNewLine();
        writer.append("for (var field in obj) {").softNewLine().indent();
        writer.append("if (!obj.hasOwnProperty(field)) {").softNewLine().indent();
        writer.append("continue;").softNewLine().outdent().append("}").softNewLine();
        writer.append("copy[field] = obj[field];").softNewLine().outdent().append("}").softNewLine();
        writer.append("return copy;").softNewLine();
    }

    private void achieveClone(DependencyChecker checker, MethodReference method) {
        MethodGraph graph = checker.attachMethodGraph(method);
        graph.getVariableNode(0).connect(graph.getResultNode());
    }

    private void generateWrap(GeneratorContext context, SourceWriter writer) {
        writer.append("return ").append(context.getParameterName(1)).append(";").softNewLine();
    }

    private void achieveWrap(DependencyChecker checker, MethodReference method) {
        MethodGraph graph = checker.attachMethodGraph(method);
        graph.getVariableNode(1).connect(graph.getResultNode());
    }
}
