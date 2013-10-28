package org.teavm.javascript;

import java.util.Set;
import org.teavm.codegen.NamingStrategy;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ast.RenderableMethod;
import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class Decompiler {
    private SourceWriter writer;
    private MethodDecompiler methodDecompiler;
    private NamingStrategy naming;
    private Renderer renderer;

    public Decompiler(ClassHolderSource classSource, NamingStrategy naming, SourceWriter writer) {
        this.methodDecompiler = new MethodDecompiler(classSource);
        this.renderer = new Renderer(writer, classSource);
        this.writer = writer;
        this.naming = naming;
    }

    public void decompile(ClassHolder cls) {
        writer.appendClass(cls.getName()).append(" = function() {\n").indent().newLine();
        for (FieldHolder field : cls.getFields()) {
            if (field.getModifiers().contains(ElementModifier.STATIC)) {
                continue;
            }
            Object value = field.getInitialValue();
            if (value == null) {
                value = getDefaultValue(field.getType());
            }
            writer.append("this.").appendField(cls.getName(), field.getName()).append(" = ")
                    .append(renderer.constantToString(value)).append(";").newLine();
        }
        writer.append("this.$class = ").appendClass(cls.getName()).append(";").newLine();
        writer.outdent().append("}").newLine();

        for (FieldHolder field : cls.getFields()) {
            if (!field.getModifiers().contains(ElementModifier.STATIC)) {
                continue;
            }
            Object value = field.getInitialValue();
            if (value == null) {
                value = getDefaultValue(field.getType());
            }
            writer.appendClass(cls.getName()).append('.')
                    .appendField(cls.getName(), field.getName()).append(" = ")
                    .append(renderer.constantToString(value)).append(";").newLine();
        }

        writer.appendClass(cls.getName()).append(".prototype = new ")
                .append(cls.getParent() != null ? naming.getNameFor(cls.getParent()) :
                "Object").append("();").newLine();
        writer.appendClass(cls.getName()).append(".$meta = { ");
        writer.append("supertypes : [");
        boolean first = true;
        if (cls.getParent() != null) {
            writer.appendClass(cls.getParent());
            first = false;
        }
        for (String iface : cls.getInterfaces()) {
            if (!first) {
                writer.append(", ");
            }
            first = false;
            writer.appendClass(iface);
        }
        writer.append("]");
        writer.append(" };").newLine();
        for (MethodHolder method : cls.getMethods()) {
            Set<ElementModifier> modifiers = method.getModifiers();
            if (modifiers.contains(ElementModifier.ABSTRACT)) {
                continue;
            }
            if (modifiers.contains(ElementModifier.NATIVE)) {
                AnnotationHolder annotHolder = method.getAnnotations().get(GeneratedBy.class.getName());
                if (annotHolder == null) {
                    throw new DecompilationException("Method " + cls.getName() + "." + method.getDescriptor() +
                            " is native, but no " + GeneratedBy.class.getName() + " annotation found");
                }
                ValueType annotValue = annotHolder.getValues().get("value").getJavaClass();
                String generatorClassName = ((ValueType.Object)annotValue).getClassName();
                generateNativeMethod(generatorClassName, method);
            } else {
                RenderableMethod renderableMethod = methodDecompiler.decompile(method);
                Optimizer optimizer = new Optimizer();
                optimizer.optimize(renderableMethod);
                renderer.render(renderableMethod);
            }
        }
    }

    private void generateNativeMethod(String generatorClassName, MethodHolder method) {
        Generator generator;
        try {
            Class<?> generatorClass = Class.forName(generatorClassName);
            generator = (Generator)generatorClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new DecompilationException("Error instantiating generator " + generatorClassName +
                    " for native method " + method.getOwner().getName() + "." + method.getDescriptor());
        }
        MethodReference ref = new MethodReference(method.getOwner().getName(), method.getDescriptor());
        generator.generate(new Context(), writer, ref);
    }

    private static Object getDefaultValue(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitive = (ValueType.Primitive)type;
            switch (primitive.getKind()) {
                case BOOLEAN:
                    return false;
                case BYTE:
                    return (byte)0;
                case SHORT:
                    return (short)0;
                case INTEGER:
                    return 0;
                case CHARACTER:
                    return '\0';
                case LONG:
                    return 0L;
                case FLOAT:
                    return 0F;
                case DOUBLE:
                    return 0.0;
            }
        }
        return null;
    }

    private class Context implements GeneratorContext {
        @Override
        public String getParameterName(int index) {
            return renderer.variableName(index);
        }

        @Override
        public NamingStrategy getNaming() {
            return naming;
        }
    }
}
