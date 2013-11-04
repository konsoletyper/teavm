package org.teavm.classlib.java.lang;

import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TObjectNativeGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        switch (methodRef.getDescriptor().getName()) {
            case "init":
                generateInit(context, writer);
                break;
            case "getClass":
                generateGetClass(context, writer);
                break;
            case "hashCode":
                generateHashCode(context, writer);
                break;
            case "equals":
                generateEquals(context, writer);
                break;
            case "clone":
                generateClone(context, writer);
                break;
        }
    }

    private void generateInit(GeneratorContext context, SourceWriter writer) {
        writer.append(context.getParameterName(0)).append(".$id = $rt_nextId();").newLine();
    }

    private void generateGetClass(GeneratorContext context, SourceWriter writer) {
        String thisArg = context.getParameterName(0);
        String classClass = "java.lang.Class";
        writer.append("var cls = ").append(thisArg).append(".$class.classObject;").newLine();
        writer.append("if (cls === undefined) {").newLine().indent();
        MethodReference createMethodRef = new MethodReference(classClass, new MethodDescriptor("createNew",
                ValueType.object(classClass)));
        writer.append("cls = ").appendClass(classClass).appendMethod(createMethodRef).append("();").newLine();
        writer.append("cls.$data = ").append(thisArg).append(".$class;").newLine().outdent().append("}").newLine();
        writer.append("return cls;").newLine();
    }

    private void generateHashCode(GeneratorContext context, SourceWriter writer) {
        writer.append("return ").append(context.getParameterName(0)).append(".$id;").newLine();
    }

    private void generateEquals(GeneratorContext context, SourceWriter writer) {
        writer.append("return ").append(context.getParameterName(0)).append(" == ")
                .append(context.getParameterName(1)).append(";").newLine();
    }

    private void generateClone(GeneratorContext context, SourceWriter writer) {
        writer.append("var copy = new ").append(context.getParameterName(0)).append("obj.$class();").newLine();
        writer.append("for (var field in obj) {").newLine().indent();
        writer.append("if (!obj.hasOwnProperty(field)) {").newLine().indent();
        writer.append("continue;").newLine().outdent().append("}").newLine();
        writer.append("copy[field] = obj[field];").newLine().outdent().append("}").newLine();
        writer.append("return copy;").newLine();
    }
}
