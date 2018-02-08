/*
 *  Copyright 2018 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.backend.c.generate;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.backend.c.analyze.Characteristics;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.Structure;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTable;
import org.teavm.model.classes.VirtualTableEntry;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

public class ClassGenerator {
    private GenerationContext context;
    private Decompiler decompiler;
    private TagRegistry tagRegistry;
    private CodeWriter writer;
    private CodeGenerator codeGenerator;
    private ObjectIntMap<String> classLayoutOffsets = new ObjectIntHashMap<>();

    public ClassGenerator(GenerationContext context, TagRegistry tagRegistry, Decompiler decompiler,
            CodeWriter writer) {
        this.context = context;
        this.tagRegistry = tagRegistry;
        this.decompiler = decompiler;
        this.writer = writer;
        codeGenerator = new CodeGenerator(context, writer);
    }

    public void generateForwardDeclarations(ClassHolder cls) {
        generateForwardClassStructure(cls);

        for (MethodHolder method : cls.getMethods()) {
            if (method.hasModifier(ElementModifier.ABSTRACT)) {
                continue;
            }
            if (method.hasModifier(ElementModifier.NATIVE)
                    && method.getAnnotations().get(DelegateTo.class.getName()) == null) {
                continue;
            }

            codeGenerator.generateMethodSignature(method.getReference(), method.hasModifier(ElementModifier.STATIC),
                    false);
            writer.println(";");
        }

        if (needsInitializer(cls)) {
            writer.print("static void ").print(context.getNames().forClassInitializer(cls.getName())).println("();");
        }
    }

    private void generateForwardClassStructure(ClassHolder cls) {
        if (!needsData(cls) || isSystemClass(cls)) {
            return;
        }

        writer.print("struct ").print(context.getNames().forClass(cls.getName())).println(";");
    }

    public void generateStructures(ClassHolder cls) {
        generateClassStructure(cls);
        generateStaticFields(cls);
    }

    public void generateVirtualTableStructures(ClassHolder cls) {
        generateVirtualTableStructure(cls);
    }

    private void generateClassStructure(ClassHolder cls) {
        if (!needsData(cls)) {
            return;
        }

        String name = context.getNames().forClass(cls.getName());

        writer.print("typedef struct ").print(name).println(" {").indent();

        if (cls.getParent() == null || !cls.getParent().equals(Structure.class.getName())) {
            String parentName = cls.getParent();
            if (parentName == null) {
                parentName = RuntimeObject.class.getName();
            }
            writer.print("struct ").print(context.getNames().forClass(parentName)).println(" parent;");
        }

        for (FieldHolder field : cls.getFields()) {
            if (field.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            String fieldName = context.getNames().forMemberField(field.getReference());
            writer.printStrictType(field.getType()).print(" ").print(fieldName).println(";");
        }

        writer.outdent().print("} ").print(name).println(";");
    }

    private void generateStaticFields(ClassHolder cls) {
        if (!needsData(cls)) {
            return;
        }

        for (FieldHolder field : cls.getFields()) {
            if (!field.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            String fieldName = context.getNames().forStaticField(field.getReference());
            writer.print("static ").printStrictType(field.getType()).print(" ").print(fieldName).println(";");
        }
    }

    public void generateStaticGCRoots(Collection<String> classNames) {
        List<FieldReference[]> data = new ArrayList<>();
        int total = 0;

        for (String className : classNames) {
            ClassReader cls = context.getClassSource().get(className);
            if (!needsData(cls)) {
                continue;
            }

            FieldReference[] fields = new FieldReference[cls.getFields().size()];
            int index = 0;

            for (FieldReader field : cls.getFields()) {
                if (!field.hasModifier(ElementModifier.STATIC) || !isReferenceType(field.getType())) {
                    continue;
                }

                fields[index++] = field.getReference();
            }

            if (index == 0) {
                continue;
            }

            fields = Arrays.copyOf(fields, index);
            total += fields.length;
            data.add(fields);
        }

        writer.println("static void** gc_staticRoots[" + (total + 1) + "] = {").indent();
        writer.print("(void**) (intptr_t) " + total);

        for (FieldReference[] fields : data) {
            writer.print(",").println();

            boolean first = true;
            for (FieldReference field : fields) {
                if (!first) {
                    writer.print(", ");
                }
                first = false;
                String name = context.getNames().forStaticField(field);
                writer.print("(void**) &").print(name);
            }
        }

        writer.println().outdent().println("};");
    }

    private boolean isReferenceType(ValueType type) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            return !context.getCharacteristics().isStructure(className)
                    && !className.equals(Address.class.getName());
        } else {
            return type instanceof ValueType.Array;
        }
    }

    public void generateVirtualTableForwardDeclaration(ValueType type) {
        if (!needsVirtualTable(context.getCharacteristics(), type)) {
            return;
        }

        String className;
        if (type instanceof ValueType.Object) {
            className = ((ValueType.Object) type).getClassName();
        } else if (type instanceof ValueType.Array) {
            className = "java.lang.Object";
        } else {
            className = null;
        }
        String structName = className != null ? context.getNames().forClassClass(className) : "JavaClass";
        String name = context.getNames().forClassInstance(type);

        writer.print("static ").print(structName).print(" ").print(name).println(";");
    }

    private void generateVirtualTableStructure(ClassHolder cls) {
        if (!needsVirtualTable(context.getCharacteristics(), ValueType.object(cls.getName()))) {
            return;
        }

        String name = context.getNames().forClassClass(cls.getName());

        writer.print("typedef struct ").print(name).println(" {").indent();
        writer.println("JavaClass parent;");

        VirtualTable virtualTable = context.getVirtualTableProvider().lookup(cls.getName());
        CodeGenerator codeGenerator = new CodeGenerator(context, writer);
        for (VirtualTableEntry entry : virtualTable.getEntries().values()) {
            String methodName = context.getNames().forVirtualMethod(
                    new MethodReference(cls.getName(), entry.getMethod()));
            writer.printType(entry.getMethod().getResultType())
                    .print(" (*").print(methodName).print(")(");
            codeGenerator.generateMethodParameters(entry.getMethod(), false, false);
            writer.println(");");
        }

        writer.outdent().print("} ").print(name).println(";");
    }

    public void generateLayoutArray(List<String> classNames) {
        List<FieldReference[]> data = new ArrayList<>();
        int totalSize = 0;

        for (String className : classNames) {
            ClassReader cls = context.getClassSource().get(className);
            if (!needsData(cls) || !needsVirtualTable(context.getCharacteristics(), ValueType.object(className))) {
                continue;
            }

            FieldReference[] fields = new FieldReference[cls.getFields().size()];
            int index = 0;
            for (FieldReader field : cls.getFields()) {
                if (field.hasModifier(ElementModifier.STATIC) || !isReferenceType(field.getType())) {
                    continue;
                }
                fields[index++] = field.getReference();
            }

            if (index == 0) {
                continue;
            }

            fields = Arrays.copyOf(fields, index);

            classLayoutOffsets.put(className, totalSize);
            totalSize += fields.length + 1;
            data.add(fields);
        }

        writer.print("static int16_t classLayouts[" + totalSize + "] = {").indent();
        for (int i = 0; i < data.size(); ++i) {
            if (i > 0) {
                writer.print(",");
            }
            FieldReference[] fields = data.get(i);
            writer.println().print("INT16_C(" + fields.length + ")");

            for (FieldReference field : fields) {
                String className = context.getNames().forClass(field.getClassName());
                String fieldName = context.getNames().forMemberField(field);
                writer.print(", (int16_t) offsetof(" + className + ", " + fieldName + ")");
            }
        }
        writer.println().outdent().println("};");
    }

    public void generateVirtualTable(ValueType type, Set<? extends ValueType> allTypes) {
        if (!needsVirtualTable(context.getCharacteristics(), type)) {
            return;
        }

        generateIsSupertypeForwardDeclaration(type);

        String className = null;
        if (type instanceof ValueType.Object) {
            className = ((ValueType.Object) type).getClassName();
        } else if (type instanceof ValueType.Array) {
            className = "java.lang.Object";
        }
        String structName = className != null
                ? context.getNames().forClassClass(className)
                : "JavaClass";
        String name = context.getNames().forClassInstance(type);

        writer.print("static ").print(structName).print(" ").print(name).println(" = {").indent();

        if (className != null) {
            writer.println(".parent = {").indent();
            generateRuntimeClassInitializer(type, allTypes);
            writer.outdent().println("},");

            VirtualTable virtualTable = context.getVirtualTableProvider().lookup(className);
            if (virtualTable != null) {
                List<VirtualTableEntry> entries = new ArrayList<>(virtualTable.getEntries().values());
                for (int i = 0; i < entries.size(); ++i) {
                    VirtualTableEntry entry = entries.get(i);
                    String methodName = context.getNames().forVirtualMethod(
                            new MethodReference(className, entry.getMethod()));
                    String implName = entry.getImplementor() != null
                            ? "&" + context.getNames().forMethod(entry.getImplementor())
                            : "NULL";
                    writer.print(".").print(methodName).print(" = ").print(implName);
                    if (i < entries.size() - 1) {
                        writer.print(",");
                    }
                    writer.println();
                }
            }
        } else {
            generateRuntimeClassInitializer(type, allTypes);
        }

        writer.outdent().println("};");
    }

    private void generateRuntimeClassInitializer(ValueType type, Set<? extends ValueType> allTypes) {
        String sizeExpr;
        int tag;
        String parent;
        String itemTypeExpr;
        int flags = 0;
        String layout = "NULL";

        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = context.getClassSource().get(className);

            if (className.equals(Object.class.getName())) {
                className = RuntimeObject.class.getName();
            }

            if (needsData(cls)) {
                String structName = context.getNames().forClass(className);
                sizeExpr = "(int32_t) (intptr_t) ALIGN(sizeof(" + structName + "), sizeof(void*))";
            } else {
                sizeExpr = "0";
            }
            tag = tagRegistry.getRanges(className).get(0).lower;

            parent = cls != null && cls.getParent() != null && allTypes.contains(ValueType.object(cls.getParent()))
                    ? "&" + context.getNames().forClassInstance(ValueType.object(cls.getParent()))
                    : "NULL";
            itemTypeExpr = "NULL";
            int layoutOffset = classLayoutOffsets.getOrDefault(className, -1);
            layout = layoutOffset >= 0 ? "classLayouts + " + layoutOffset : "NULL";
        } else if (type instanceof ValueType.Array) {
            parent = "&" + context.getNames().forClassInstance(ValueType.object("java.lang.Object"));
            tag = tagRegistry.getRanges("java.lang.Object").get(0).lower;
            ValueType itemType = ((ValueType.Array) type).getItemType();
            sizeExpr = "sizeof(" + CodeWriter.strictTypeAsString(itemType) + ")";
            itemTypeExpr = "&" + context.getNames().forClassInstance(itemType);
        } else if (type == ValueType.VOID) {
            parent = "NULL";
            tag = 0;
            sizeExpr = "0";
            itemTypeExpr = "NULL";
        } else {
            parent = "NULL";
            tag = 0;
            sizeExpr = "sizeof(" + CodeWriter.strictTypeAsString(type) + ")";
            flags |= RuntimeClass.PRIMITIVE;
            itemTypeExpr = "NULL";
        }

        int nameRef = context.getStringPool().getStringIndex(nameOfType(type));
        String superTypeFunction = context.getNames().forSupertypeFunction(type);

        ValueType arrayType = ValueType.arrayOf(type);
        String arrayTypeExpr;
        if (allTypes.contains(arrayType)) {
            arrayTypeExpr = "&" + context.getNames().forClassInstance(arrayType);
        } else {
            arrayTypeExpr = "NULL";
        }

        writer.println(".parent = {},");
        writer.print(".").print(classFieldName("size")).print(" = ").print(sizeExpr).println(",");
        writer.print(".").print(classFieldName("flags")).println(" = " + flags + ",");
        writer.print(".").print(classFieldName("tag")).print(" = ").print(String.valueOf(tag)).println(",");
        writer.print(".").print(classFieldName("canary")).println(" = 0,");
        writer.print(".").print(classFieldName("name")).println(" = stringPool + " + nameRef + ",");
        writer.print(".").print(classFieldName("arrayType")).println(" = " + arrayTypeExpr + ",");
        writer.print(".").print(classFieldName("itemType")).println(" = " + itemTypeExpr + ",");
        writer.print(".").print(classFieldName("isSupertypeOf")).println(" = &" + superTypeFunction + ",");
        writer.print(".").print(classFieldName("parent")).println(" = " + parent + ",");
        writer.print(".").print(classFieldName("enumValues")).println(" = NULL,");
        writer.print(".").print(classFieldName("layout")).println(" = " + layout);
    }

    private String classFieldName(String field) {
        return context.getNames().forMemberField(new FieldReference(RuntimeClass.class.getName(), field));
    }

    private boolean needsData(ClassReader cls) {
        if (cls.hasModifier(ElementModifier.INTERFACE)) {
            return false;
        }
        return !cls.getName().equals(Structure.class.getName())
                && !cls.getName().equals(Address.class.getName());
    }

    private boolean isSystemClass(ClassHolder cls) {
        switch (cls.getName()) {
            case "java.lang.Object":
            case "java.lang.Class":
            case "java.lang.String":
                return true;
            default:
                return false;
        }
    }

    public static boolean needsVirtualTable(Characteristics characteristics, ValueType type) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            if (className.equals(Address.class.getName())) {
                return false;
            }
            return !characteristics.isStructure(className);
        } else if (type instanceof ValueType.Array) {
            return needsVirtualTable(characteristics, ((ValueType.Array) type).getItemType());
        } else {
            return true;
        }
    }

    public void generateClass(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.hasModifier(ElementModifier.NATIVE)) {
                tryDelegateToMethod(cls, method);
                continue;
            }

            if (method.hasModifier(ElementModifier.ABSTRACT)) {
                continue;
            }

            RegularMethodNode methodNode = decompiler.decompileRegular(method);
            codeGenerator.generateMethod(methodNode);
        }

        if (needsInitializer(cls)) {
            writer.print("static void ").print(context.getNames().forClassInitializer(cls.getName()))
                    .println("() {").indent();

            String classInstanceName = context.getNames().forClassInstance(ValueType.object(cls.getName()));
            String clinitName = context.getNames().forMethod(
                    new MethodReference(cls.getName(), "<clinit>", ValueType.VOID));
            writer.print("JavaClass* cls = (JavaClass*) &").print(classInstanceName).println(";");
            writer.println("if (!(cls->flags & INT32_C(" + RuntimeClass.INITIALIZED + "))) {").indent();
            writer.println("cls->flags |= INT32_C(" + RuntimeClass.INITIALIZED + ");");
            writer.print(clinitName).println("();");
            writer.outdent().println("}");

            writer.outdent().println("}");
        }
    }

    private boolean needsInitializer(ClassHolder cls) {
        return !context.getCharacteristics().isStaticInit(cls.getName())
                && !context.getCharacteristics().isStructure(cls.getName())
                && cls.getMethod(new MethodDescriptor("<clinit>", ValueType.VOID)) != null;
    }

    private void tryDelegateToMethod(ClassHolder cls, MethodHolder method) {
        AnnotationHolder delegateToAnnot = method.getAnnotations().get(DelegateTo.class.getName());
        if (delegateToAnnot == null) {
            return;
        }

        String methodName = delegateToAnnot.getValue("value").getString();
        for (MethodHolder candidate : cls.getMethods()) {
            if (candidate.getName().equals(methodName)) {
                delegateToMethod(method, candidate);
                break;
            }
        }
    }

    private void delegateToMethod(MethodHolder callingMethod, MethodHolder delegateMethod) {
        codeGenerator.generateMethodSignature(callingMethod.getReference(),
                callingMethod.hasModifier(ElementModifier.STATIC), true);
        writer.println(" {").indent();

        if (callingMethod.getResultType() != ValueType.VOID) {
            writer.print("return ");
        }

        writer.print(context.getNames().forMethod(delegateMethod.getReference())).print("(");

        boolean isStatic = callingMethod.hasModifier(ElementModifier.STATIC);
        int start = 0;
        if (!isStatic) {
            writer.print("_this_");
        } else {
            if (callingMethod.parameterCount() > 0) {
                writer.print("local_1");
            }
            start++;
        }

        for (int i = start; i < callingMethod.parameterCount(); ++i) {
            writer.print(", ").print("local_").print(String.valueOf(i + 1));
        }

        writer.println(");");

        writer.outdent().println("}");
    }

    public static String nameOfType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return "boolean";
                case BYTE:
                    return "byte";
                case SHORT:
                    return "short";
                case CHARACTER:
                    return "char";
                case INTEGER:
                    return "int";
                case LONG:
                    return "long";
                case FLOAT:
                    return "float";
                case DOUBLE:
                    return "double";
                default:
                    throw new AssertionError();
            }
        } else if (type instanceof ValueType.Array) {
            return nameOfType(((ValueType.Array) type).getItemType()) + "[]";
        } else if (type == ValueType.VOID) {
            return "void";
        } else if (type instanceof ValueType.Object) {
            return ((ValueType.Object) type).getClassName();
        } else {
            throw new AssertionError();
        }
    }

    private void generateIsSupertypeForwardDeclaration(ValueType type) {
        String name = context.getNames().forSupertypeFunction(type);
        writer.println("static int32_t " + name + "(JavaClass*);");
    }

    public void generateIsSupertypeFunction(ValueType type) {
        String name = context.getNames().forSupertypeFunction(type);
        writer.println("static int32_t " + name + "(JavaClass* cls) {").indent();

        if (type instanceof ValueType.Object) {
            generateIsSuperclassFunction(((ValueType.Object) type).getClassName());
        } else if (type instanceof ValueType.Primitive) {
            generateIsSuperPrimitiveFunction((ValueType.Primitive) type);
        } else if (type == ValueType.VOID) {
            generateIsSuperclassFunction("java.lang.Void");
        } else if (type instanceof ValueType.Array) {
            generateIsSuperArrayFunction(((ValueType.Array) type).getItemType());
        }

        writer.outdent().println("}");
    }

    private void generateIsSuperclassFunction(String className) {
        List<TagRegistry.Range> ranges = tagRegistry.getRanges(className);
        if (ranges.isEmpty()) {
            writer.println("return INT32_C(0);");
            return;
        }

        String tagName = context.getNames().forMemberField(new FieldReference(
                RuntimeClass.class.getName(), "tag"));
        writer.println("int32_t tag = cls->" + tagName + ";");

        int lower = ranges.get(0).lower;
        int upper = ranges.get(ranges.size() - 1).upper;
        writer.println("if (tag < " + lower + " || tag > " + upper + ") return INT32_C(0);");

        for (int i = 1; i < ranges.size(); ++i) {
            lower = ranges.get(i - 1).upper;
            upper = ranges.get(i).lower;
            writer.println("if (tag < " + lower + " || tag > " + upper + ") return INT32_C(0);");
        }

        writer.println("return INT32_C(1);");
    }

    private void generateIsSuperArrayFunction(ValueType itemType) {
        String itemTypeName = context.getNames().forMemberField(new FieldReference(
                RuntimeClass.class.getName(), "itemType"));
        writer.println("JavaClass* itemType = cls->" + itemTypeName + ";");
        writer.println("if (itemType == NULL) return INT32_C(0);");

        if (itemType instanceof ValueType.Primitive) {
            writer.println("return itemType == &" + context.getNames().forClassInstance(itemType) + ";");
        } else {
            writer.println("return " + context.getNames().forSupertypeFunction(itemType) + "(itemType);");
        }
    }

    private void generateIsSuperPrimitiveFunction(ValueType.Primitive primitive) {
        switch (primitive.getKind()) {
            case BOOLEAN:
                generateIsSuperclassFunction("java.lang.Boolean");
                break;
            case BYTE:
                generateIsSuperclassFunction("java.lang.Byte");
                break;
            case SHORT:
                generateIsSuperclassFunction("java.lang.Short");
                break;
            case CHARACTER:
                generateIsSuperclassFunction("java.lang.Character");
                break;
            case INTEGER:
                generateIsSuperclassFunction("java.lang.Integer");
                break;
            case LONG:
                generateIsSuperclassFunction("java.lang.Long");
                break;
            case FLOAT:
                generateIsSuperclassFunction("java.lang.Float");
                break;
            case DOUBLE:
                generateIsSuperclassFunction("java.lang.Double");
                break;
        }
    }
}
