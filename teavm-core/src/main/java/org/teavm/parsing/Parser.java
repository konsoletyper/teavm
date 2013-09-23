package org.teavm.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.teavm.model.*;
import org.teavm.model.util.ListingBuilder;
import org.teavm.optimization.UnreachableBasicBlockEliminator;

/**
 *
 * @author Alexey Andreev
 */
public class Parser {
    public static MethodHolder parseMethod(MethodNode node) {
        ValueType[] signature = MethodDescriptor.parseSignature(node.desc);
        MethodHolder method = new MethodHolder(node.name, signature);
        parseModifiers(node.access, method);
        ProgramParser programParser = new ProgramParser();
        Program program = programParser.parser(node);
        new UnreachableBasicBlockEliminator().optimize(program);
        SSATransformer ssaProducer = new SSATransformer();
        ssaProducer.transformToSSA(program, method.getParameterTypes());
        method.setProgram(program);
        return method;
    }

    public static ClassHolder parseClass(ClassNode node) {
        ClassHolder cls = new ClassHolder(node.name.replace('/', '.'));
        parseModifiers(node.access, cls);
        if (node.superName != null) {
            cls.setParent(node.superName.replace('/', '.'));
        }
        if (node.interfaces != null) {
            for (Object obj : node.interfaces) {
                cls.getInterfaces().add(((String)obj).replace('/', '.'));
            }
        }
        for (Object obj : node.fields) {
            FieldNode fieldNode = (FieldNode)obj;
            cls.addField(parseField(fieldNode));
        }
        for (Object obj : node.methods) {
            MethodNode methodNode = (MethodNode)obj;
            cls.addMethod(parseMethod(methodNode));
        }
        return cls;
    }

    public static FieldHolder parseField(FieldNode node) {
        FieldHolder field = new FieldHolder(node.name);
        field.setType(ValueType.parse(node.desc));
        field.setInitialValue(node.value);
        parseModifiers(node.access, field);
        return field;
    }

    public static void parseModifiers(int access, ElementHolder member) {
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            member.setLevel(AccessLevel.PRIVATE);
        } else if ((access & Opcodes.ACC_PROTECTED) != 0) {
            member.setLevel(AccessLevel.PROTECTED);
        } else if ((access & Opcodes.ACC_PUBLIC) != 0) {
            member.setLevel(AccessLevel.PUBLIC);
        }

        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            member.getModifiers().add(ElementModifier.ABSTRACT);
        }
        if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            member.getModifiers().add(ElementModifier.ANNOTATION);
        }
        if ((access & Opcodes.ACC_BRIDGE) != 0) {
            member.getModifiers().add(ElementModifier.BRIDGE);
        }
        if ((access & Opcodes.ACC_DEPRECATED) != 0) {
            member.getModifiers().add(ElementModifier.DEPRECATED);
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            member.getModifiers().add(ElementModifier.ENUM);
        }
        if ((access & Opcodes.ACC_FINAL) != 0) {
            member.getModifiers().add(ElementModifier.FINAL);
        }
        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            member.getModifiers().add(ElementModifier.INTERFACE);
        }
        if ((access & Opcodes.ACC_NATIVE) != 0) {
            member.getModifiers().add(ElementModifier.NATIVE);
        }
        if ((access & Opcodes.ACC_STATIC) != 0) {
            member.getModifiers().add(ElementModifier.STATIC);
        }
        if ((access & Opcodes.ACC_STRICT) != 0) {
            member.getModifiers().add(ElementModifier.STRICT);
        }
        if ((access & Opcodes.ACC_SUPER) != 0) {
            member.getModifiers().add(ElementModifier.SUPER);
        }
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
            member.getModifiers().add(ElementModifier.SYNCHRONIZED);
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            member.getModifiers().add(ElementModifier.SYNTHETIC);
        }
        if ((access & Opcodes.ACC_TRANSIENT) != 0) {
            member.getModifiers().add(ElementModifier.TRANSIENT);
        }
        if ((access & Opcodes.ACC_VARARGS) != 0) {
            member.getModifiers().add(ElementModifier.VARARGS);
        }
        if ((access & Opcodes.ACC_VOLATILE) != 0) {
            member.getModifiers().add(ElementModifier.VOLATILE);
        }
    }

    public static void main(String[] args) throws IOException {
        Class<?>[] classesToParse = { Object.class, String.class, ArrayList.class,
                StringBuilder.class, HashMap.class };
        ClassLoader classLoader = Parser.class.getClassLoader();
        for (Class<?> cls : classesToParse) {
            try (InputStream input = classLoader.getResourceAsStream(cls.getName().replace('.', '/') + ".class")) {
                ClassReader reader = new ClassReader(input);
                ClassNode node = new ClassNode();
                reader.accept(node, 0);
                display(parseClass(node));
            }
        }
    }

    private static void display(ClassHolder cls) {
        System.out.print(cls.getLevel());
        for (ElementModifier modifier : cls.getModifiers()) {
            System.out.print(" " + modifier);
        }
        System.out.print(" class " + cls.getName());
        if (cls.getParent() != null) {
            System.out.print(" extends " + cls.getParent());
        }
        if (!cls.getInterfaces().isEmpty()) {
            System.out.print(" implements ");
            boolean first = true;
            for (String iface : cls.getInterfaces()) {
                if (!first) {
                    System.out.print(", ");
                } else {
                    first = false;
                }
                System.out.print(iface);
            }
        }
        System.out.println();
        for (FieldHolder field : cls.getFields()) {
            System.out.print("    " + field.getLevel());
            for (ElementModifier modifier : field.getModifiers()) {
                System.out.print(" " + modifier);
            }
            System.out.println(" " + field.getName() + " : " + field.getType());
        }
        ListingBuilder listingBuilder = new ListingBuilder();
        for (MethodHolder method : cls.getMethods()) {
            System.out.print("    " + method.getLevel());
            for (ElementModifier modifier : method.getModifiers()) {
                System.out.print(" " + modifier);
            }
            System.out.println(" " + method.getDescriptor());
            System.out.println(listingBuilder.buildListing(method.getProgram(), "        "));
        }
        System.out.println();
        System.out.println();
    }
}
