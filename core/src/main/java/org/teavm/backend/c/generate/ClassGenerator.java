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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.backend.c.generators.Generator;
import org.teavm.backend.c.generators.GeneratorContext;
import org.teavm.backend.lowlevel.generate.ClassGeneratorUtil;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.Structure;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
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
import org.teavm.model.lowlevel.Characteristics;
import org.teavm.model.lowlevel.ShadowStackTransformer;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

public class ClassGenerator {
    private GenerationContext context;
    private ClassReaderSource unprocessedClassSource;
    private Decompiler decompiler;
    private TagRegistry tagRegistry;
    private CodeGenerator codeGenerator;
    private ObjectIntMap<String> classLayoutOffsets = new ObjectIntHashMap<>();
    private List<FieldReference[]> staticGcRoots = new ArrayList<>();
    private List<FieldReference[]> layouts = new ArrayList<>();
    private int currentLayoutIndex;
    private Set<ValueType> types = new LinkedHashSet<>();
    private Set<String> includes = new LinkedHashSet<>();
    private CodeWriter includesWriter;
    private CodeWriter forwardDeclarationsWriter;
    private CodeWriter structuresWriter;
    private CodeWriter vtableStructuresWriter;
    private CodeWriter stringPoolWriter;
    private CodeWriter layoutWriter;
    private CodeWriter vtableForwardWriter;
    private CodeWriter vtableWriter;
    private CodeWriter isSupertypeWriter;
    private CodeWriter staticGcRootsWriter;
    private CodeWriter callSiteWriter;
    private CodeWriter preCodeWriter;
    private CodeWriter codeWriter;
    private CodeWriter staticFieldInitWriter;

    public ClassGenerator(GenerationContext context, ClassReaderSource unprocessedClassSource,
            TagRegistry tagRegistry, Decompiler decompiler, CodeWriter writer) {
        this.context = context;
        this.unprocessedClassSource = unprocessedClassSource;
        this.tagRegistry = tagRegistry;
        this.decompiler = decompiler;

        includesWriter = writer.fragment();
        forwardDeclarationsWriter = writer.fragment();
        structuresWriter = writer.fragment();
        vtableStructuresWriter = writer.fragment();
        stringPoolWriter = writer.fragment();
        layoutWriter = writer.fragment();
        vtableForwardWriter = writer.fragment();
        vtableWriter = writer.fragment();
        isSupertypeWriter = writer.fragment();
        staticGcRootsWriter = writer.fragment();
        callSiteWriter = writer.fragment();
        preCodeWriter = writer.fragment();
        codeWriter = writer.fragment();

        writer.println("static void initStaticFields() {").indent();
        staticFieldInitWriter = writer.fragment();
        writer.outdent().println("}");

        codeGenerator = new CodeGenerator(context, codeWriter, includes);
    }

    public CodeWriter getPreCodeWriter() {
        return preCodeWriter;
    }

    public CodeWriter getStructuresWriter() {
        return structuresWriter;
    }

    public void generateClass(ClassHolder cls) {
        generateClassStructure(cls);
        generateClassMethods(cls);
        generateInitializer(cls);
    }

    public void generateRemainingData(List<String> classNames, ShadowStackTransformer shadowStackTransformer) {
        generateCallSites(shadowStackTransformer);

        collectTypes(classNames);
        for (ValueType type : types) {
            generateVirtualTable(type);
        }
        generateStaticGCRoots();
        generateLayoutArray();

        new StringPoolGenerator(stringPoolWriter).generate(context.getStringPool().getStrings());

        for (String include : includes) {
            includesWriter.println("#include " + include);
        }
    }

    public Set<ValueType> getTypes() {
        return types;
    }

    private void collectTypes(List<String> classNames) {
        for (String className : classNames) {
            types.add(ValueType.object(className));
        }

        types.add(ValueType.object("java.lang.Class"));
        for (ValueType type : context.getNames().getTypes()) {
            if (type instanceof ValueType.Array) {
                types.add(ValueType.object("java.lang.Object"));
            }
            while (true) {
                if (!types.add(type)) {
                    break;
                }
                if (!(type instanceof ValueType.Array)) {
                    break;
                }
                type = ((ValueType.Array) type).getItemType();
            }
        }
    }

    private void generateCallSites(ShadowStackTransformer shadowStackTransformer) {
        new CallSiteGenerator(context, callSiteWriter).generate(shadowStackTransformer.getCallSites());
    }

    private void generateClassMethods(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods()) {
            if (method.hasModifier(ElementModifier.ABSTRACT)) {
                continue;
            }

            if (method.hasModifier(ElementModifier.NATIVE)) {
                if (!tryDelegateToMethod(cls, method)) {
                    tryUsingGenerator(method);
                }
                continue;
            }

            generateMethodForwardDeclaration(method);
            RegularMethodNode methodNode = decompiler.decompileRegular(method);
            codeGenerator.generateMethod(methodNode);
        }
    }

    private void generateMethodForwardDeclaration(MethodHolder method) {
        codeGenerator.generateMethodSignature(forwardDeclarationsWriter, method.getReference(),
                method.hasModifier(ElementModifier.STATIC), false);
        forwardDeclarationsWriter.println(";");

    }

    private void generateInitializer(ClassHolder cls) {
        if (!needsInitializer(cls)) {
            return;
        }

        forwardDeclarationsWriter.print("static void ")
                .print(context.getNames().forClassInitializer(cls.getName())).println("();");

        codeWriter.print("static void ").print(context.getNames().forClassInitializer(cls.getName()))
                .println("() {").indent();

        String classInstanceName = context.getNames().forClassInstance(ValueType.object(cls.getName()));
        String clinitName = context.getNames().forMethod(
                new MethodReference(cls.getName(), "<clinit>", ValueType.VOID));
        codeWriter.print("JavaClass* cls = (JavaClass*) &").print(classInstanceName).println(";");
        codeWriter.println("if (!(cls->flags & INT32_C(" + RuntimeClass.INITIALIZED + "))) {").indent();
        codeWriter.println("cls->flags |= INT32_C(" + RuntimeClass.INITIALIZED + ");");
        codeWriter.print(clinitName).println("();");
        codeWriter.outdent().println("}");

        codeWriter.outdent().println("}");
    }

    private void generateClassStructure(ClassHolder cls) {
        if (!needsData(cls)) {
            return;
        }
        generateForwardClassStructure(cls);

        String name = context.getNames().forClass(cls.getName());

        CodeWriter structWriter = structuresWriter.fragment();
        CodeWriter fieldsWriter = structuresWriter.fragment();

        structWriter.print("typedef struct ").print(name).println(" {").indent();

        if (cls.getParent() == null || !cls.getParent().equals(Structure.class.getName())) {
            String parentName = cls.getParent();
            if (parentName == null) {
                parentName = RuntimeObject.class.getName();
            }
            structWriter.print("struct ").print(context.getNames().forClass(parentName)).println(" parent;");
        }

        int layoutIndex = currentLayoutIndex;

        FieldReference[] staticFields = new FieldReference[cls.getFields().size()];
        int staticIndex = 0;
        FieldReference[] instanceFields = new FieldReference[cls.getFields().size()];
        int instanceIndex = 0;
        for (FieldHolder field : cls.getFields()) {
            if (field.hasModifier(ElementModifier.STATIC)) {
                String fieldName = context.getNames().forStaticField(field.getReference());
                fieldsWriter.print("static ").printStrictType(field.getType()).print(" ").print(fieldName)
                        .println(";");
                if (isReferenceType(field.getType())) {
                    staticFields[staticIndex++] = field.getReference();
                }

                Object initialValue = field.getInitialValue();
                if (initialValue == null) {
                    initialValue = getDefaultValue(field.getType());
                }
                staticFieldInitWriter.print(fieldName + " = ");
                CodeGeneratorUtil.writeValue(staticFieldInitWriter, context, initialValue);
                staticFieldInitWriter.println(";");
            } else {
                String fieldName = context.getNames().forMemberField(field.getReference());
                structWriter.printStrictType(field.getType()).print(" ").print(fieldName).println(";");
                if (isReferenceType(field.getType())) {
                    instanceFields[instanceIndex++] = field.getReference();
                }
            }
        }

        if (staticIndex > 0) {
            staticGcRoots.add(Arrays.copyOf(staticFields, staticIndex));
        }
        if (instanceIndex > 0) {
            classLayoutOffsets.put(cls.getName(), layoutIndex);
            layouts.add(Arrays.copyOf(instanceFields, instanceIndex));
            currentLayoutIndex += instanceIndex + 1;
        }

        structWriter.outdent().print("} ").print(name).println(";");
    }

    private static Object getDefaultValue(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            ValueType.Primitive primitive = (ValueType.Primitive) type;
            switch (primitive.getKind()) {
                case BOOLEAN:
                    return false;
                case BYTE:
                    return (byte) 0;
                case SHORT:
                    return (short) 0;
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

    private void generateForwardClassStructure(ClassHolder cls) {
        if (isSystemClass(cls)) {
            return;
        }

        forwardDeclarationsWriter.print("struct ").print(context.getNames().forClass(cls.getName())).println(";");
    }

    private void generateVirtualTable(ValueType type) {
        if (!needsVirtualTable(context.getCharacteristics(), type)) {
            return;
        }

        generateIsSupertypeFunction(type);

        String className = null;
        if (type instanceof ValueType.Object) {
            className = ((ValueType.Object) type).getClassName();
            generateVirtualTableStructure(unprocessedClassSource.get(className));
        } else if (type instanceof ValueType.Array) {
            className = "java.lang.Object";
        }
        String structName = className != null
                ? context.getNames().forClassClass(className)
                : "JavaClass";
        String name = context.getNames().forClassInstance(type);

        vtableForwardWriter.print("static ").print(structName).print(" ").print(name).println(";");

        ClassReader cls = className != null ? context.getClassSource().get(className) : null;
        String enumConstants;
        if (cls != null && cls.hasModifier(ElementModifier.ENUM)) {
            enumConstants = writeEnumConstants(cls, name);
        } else {
            enumConstants = "NULL";
        }

        vtableWriter.print("static alignas(8) ").print(structName).print(" ").print(name).println(" = {").indent();

        if (className != null) {
            vtableWriter.println(".parent = {").indent();
            generateRuntimeClassInitializer(type, enumConstants);
            vtableWriter.outdent().println("},");

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
                    vtableWriter.print(".").print(methodName).print(" = ").print(implName);
                    if (i < entries.size() - 1) {
                        vtableWriter.print(",");
                    }
                    vtableWriter.println();
                }
            }
        } else {
            generateRuntimeClassInitializer(type, enumConstants);
        }

        vtableWriter.outdent().println("};");
    }

    private String writeEnumConstants(ClassReader cls, String baseName) {
        List<FieldReader> fields = cls.getFields().stream()
                .filter(f -> f.hasModifier(ElementModifier.ENUM))
                .collect(Collectors.toList());
        String name = baseName + "_enumConstants";
        vtableWriter.print("static void* " + name + "[" + (fields.size() + 1) + "] = { ");
        vtableWriter.print("(void*) (intptr_t) " + fields.size());
        for (FieldReader field : fields) {
            vtableWriter.print(", ").print("&" + context.getNames().forStaticField(field.getReference()));
        }
        vtableWriter.println(" };");
        return name;
    }

    private void generateRuntimeClassInitializer(ValueType type, String enumConstants) {
        String sizeExpr;
        int tag;
        String parent;
        String itemTypeExpr;
        int flags = 0;
        String layout = "NULL";
        String initFunction = "NULL";

        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = context.getClassSource().get(className);

            if (className.equals(Object.class.getName())) {
                className = RuntimeObject.class.getName();
            }

            if (cls != null && needsData(cls)) {
                String structName = context.getNames().forClass(className);
                sizeExpr = "(int32_t) (intptr_t) ALIGN(sizeof(" + structName + "), sizeof(void*))";
            } else {
                sizeExpr = "0";
            }
            if (cls != null && cls.hasModifier(ElementModifier.ENUM)) {
                flags |= RuntimeClass.ENUM;
            }
            List<TagRegistry.Range> ranges = tagRegistry.getRanges(className);
            tag = ranges != null && !ranges.isEmpty() ? ranges.get(0).lower : 0;

            parent = cls != null && cls.getParent() != null && types.contains(ValueType.object(cls.getParent()))
                    ? "&" + context.getNames().forClassInstance(ValueType.object(cls.getParent()))
                    : "NULL";
            itemTypeExpr = "NULL";
            int layoutOffset = classLayoutOffsets.getOrDefault(className, -1);
            layout = layoutOffset >= 0 ? "classLayouts + " + layoutOffset : "NULL";

            if (cls != null && needsInitializer(cls)) {
                initFunction = context.getNames().forClassInitializer(className);
            }
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
            flags = ClassGeneratorUtil.applyPrimitiveFlags(flags, type);
            itemTypeExpr = "NULL";
        }

        int nameRef = context.getStringPool().getStringIndex(nameOfType(type));
        String superTypeFunction = context.getNames().forSupertypeFunction(type);

        ValueType arrayType = ValueType.arrayOf(type);
        String arrayTypeExpr;
        if (types.contains(arrayType)) {
            arrayTypeExpr = "&" + context.getNames().forClassInstance(arrayType);
        } else {
            arrayTypeExpr = "NULL";
        }

        vtableWriter.print(".").print(classFieldName("size")).print(" = ").print(sizeExpr).println(",");
        vtableWriter.print(".").print(classFieldName("flags")).println(" = " + flags + ",");
        vtableWriter.print(".").print(classFieldName("tag")).print(" = ").print(String.valueOf(tag)).println(",");
        vtableWriter.print(".").print(classFieldName("canary")).println(" = 0,");
        vtableWriter.print(".").print(classFieldName("name")).println(" = stringPool + " + nameRef + ",");
        vtableWriter.print(".").print(classFieldName("simpleName")).println(" = NULL,");
        vtableWriter.print(".").print(classFieldName("arrayType")).println(" = " + arrayTypeExpr + ",");
        vtableWriter.print(".").print(classFieldName("itemType")).println(" = " + itemTypeExpr + ",");
        vtableWriter.print(".").print(classFieldName("isSupertypeOf")).println(" = &" + superTypeFunction + ",");
        vtableWriter.print(".").print(classFieldName("parent")).println(" = " + parent + ",");
        vtableWriter.print(".").print(classFieldName("enumValues")).println(" = NULL,");
        vtableWriter.print(".").print(classFieldName("layout")).println(" = " + layout + ",");
        vtableWriter.print(".").print(classFieldName("enumValues")).println(" = " + enumConstants + ",");
        vtableWriter.print(".").print(classFieldName("init")).println(" = " + initFunction);
    }

    private void generateVirtualTableStructure(ClassReader cls) {
        String name = context.getNames().forClassClass(cls.getName());

        vtableStructuresWriter.print("typedef struct ").print(name).println(" {").indent();
        vtableStructuresWriter.println("JavaClass parent;");

        VirtualTable virtualTable = context.getVirtualTableProvider().lookup(cls.getName());
        if (virtualTable != null) {
            for (VirtualTableEntry entry : virtualTable.getEntries().values()) {
                String methodName = context.getNames().forVirtualMethod(
                        new MethodReference(cls.getName(), entry.getMethod()));
                vtableStructuresWriter.printType(entry.getMethod().getResultType())
                        .print(" (*").print(methodName).print(")(");
                codeGenerator.generateMethodParameters(vtableStructuresWriter, entry.getMethod(), false, false);
                vtableStructuresWriter.println(");");
            }
        }

        vtableStructuresWriter.outdent().print("} ").print(name).println(";");
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

    private void generateStaticGCRoots() {
        int total = staticGcRoots.stream().mapToInt(c -> c.length).sum();

        staticGcRootsWriter.println("static void** gc_staticRoots[" + (total + 1) + "] = {").indent();
        staticGcRootsWriter.print("(void**) (intptr_t) " + total);

        for (FieldReference[] fields : staticGcRoots) {
            staticGcRootsWriter.print(",").println();

            boolean first = true;
            for (FieldReference field : fields) {
                if (!first) {
                    staticGcRootsWriter.print(", ");
                }
                first = false;
                String name = context.getNames().forStaticField(field);
                staticGcRootsWriter.print("(void**) &").print(name);
            }
        }

        staticGcRootsWriter.println().outdent().println("};");
    }

    private void generateLayoutArray() {
        int totalSize = layouts.stream().mapToInt(c -> c.length + 1).sum();

        layoutWriter.print("static int16_t classLayouts[" + totalSize + "] = {").indent();
        for (int i = 0; i < layouts.size(); ++i) {
            if (i > 0) {
                layoutWriter.print(",");
            }
            FieldReference[] fields = layouts.get(i);
            layoutWriter.println().print("INT16_C(" + fields.length + ")");

            for (FieldReference field : fields) {
                String className = context.getNames().forClass(field.getClassName());
                String fieldName = context.getNames().forMemberField(field);
                layoutWriter.print(", (int16_t) offsetof(" + className + ", " + fieldName + ")");
            }
        }
        layoutWriter.println().outdent().println("};");
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

    private boolean needsInitializer(ClassReader cls) {
        return !context.getCharacteristics().isStaticInit(cls.getName())
                && !context.getCharacteristics().isStructure(cls.getName())
                && cls.getMethod(new MethodDescriptor("<clinit>", ValueType.VOID)) != null;
    }

    private boolean tryDelegateToMethod(ClassHolder cls, MethodHolder method) {
        AnnotationHolder delegateToAnnot = method.getAnnotations().get(DelegateTo.class.getName());
        if (delegateToAnnot == null) {
            return false;
        }

        String methodName = delegateToAnnot.getValue("value").getString();
        for (MethodHolder candidate : cls.getMethods()) {
            if (candidate.getName().equals(methodName)) {
                generateMethodForwardDeclaration(method);
                delegateToMethod(method, candidate);
                return true;
            }
        }

        return false;
    }

    private void delegateToMethod(MethodHolder callingMethod, MethodHolder delegateMethod) {
        codeGenerator.generateMethodSignature(codeWriter, callingMethod.getReference(),
                callingMethod.hasModifier(ElementModifier.STATIC), true);
        codeWriter.println(" {").indent();

        if (callingMethod.getResultType() != ValueType.VOID) {
            codeWriter.print("return ");
        }

        codeWriter.print(context.getNames().forMethod(delegateMethod.getReference())).print("(");

        boolean isStatic = callingMethod.hasModifier(ElementModifier.STATIC);
        int start = 0;
        if (!isStatic) {
            codeWriter.print("_this_");
        } else {
            if (callingMethod.parameterCount() > 0) {
                codeWriter.print("local_1");
            }
            start++;
        }

        for (int i = start; i < callingMethod.parameterCount(); ++i) {
            codeWriter.print(", ").print("local_").print(String.valueOf(i + 1));
        }

        codeWriter.println(");");

        codeWriter.outdent().println("}");
    }

    private void tryUsingGenerator(MethodHolder method) {
        MethodReference methodRef = method.getReference();
        Generator generator = context.getGenerator(methodRef);
        if (generator == null) {
            return;
        }

        generateMethodForwardDeclaration(method);
        boolean isStatic = method.hasModifier(ElementModifier.STATIC);
        codeGenerator.generateMethodSignature(codeWriter, methodRef, isStatic, true);
        codeWriter.println(" {").indent();

        generator.generate(new GeneratorContext() {
            @Override
            public NameProvider names() {
                return context.getNames();
            }

            @Override
            public Diagnostics getDiagnotics() {
                return context.getDiagnostics();
            }

            @Override
            public ClassReaderSource getClassSource() {
                return context.getClassSource();
            }

            @Override
            public String getParameterName(int index) {
                return index == 0 ? "_this_" : "local_" + index;
            }
        }, codeWriter, methodRef);

        codeWriter.outdent().println("}");
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

    private void generateIsSupertypeFunction(ValueType type) {
        String name = context.getNames().forSupertypeFunction(type);
        vtableForwardWriter.println("static int32_t " + name + "(JavaClass*);");
        isSupertypeWriter.println("static int32_t " + name + "(JavaClass* cls) {").indent();

        if (type instanceof ValueType.Object) {
            generateIsSuperclassFunction(((ValueType.Object) type).getClassName());
        } else if (type instanceof ValueType.Primitive) {
            generateIsSuperPrimitiveFunction((ValueType.Primitive) type);
        } else if (type == ValueType.VOID) {
            generateIsSuperclassFunction("java.lang.Void");
        } else if (type instanceof ValueType.Array) {
            generateIsSuperArrayFunction(((ValueType.Array) type).getItemType());
        }

        isSupertypeWriter.outdent().println("}");
    }

    private void generateIsSuperclassFunction(String className) {
        List<TagRegistry.Range> ranges = tagRegistry.getRanges(className);
        if (ranges.isEmpty()) {
            isSupertypeWriter.println("return INT32_C(0);");
            return;
        }

        String tagName = context.getNames().forMemberField(new FieldReference(
                RuntimeClass.class.getName(), "tag"));
        isSupertypeWriter.println("int32_t tag = cls->" + tagName + ";");

        int lower = ranges.get(0).lower;
        int upper = ranges.get(ranges.size() - 1).upper;
        isSupertypeWriter.println("if (tag < " + lower + " || tag >= " + upper + ") return INT32_C(0);");

        for (int i = 1; i < ranges.size(); ++i) {
            lower = ranges.get(i - 1).upper;
            upper = ranges.get(i).lower;
            isSupertypeWriter.println("if (tag >= " + lower + " && tag < " + upper + ") return INT32_C(0);");
        }

        isSupertypeWriter.println("return INT32_C(1);");
    }

    private void generateIsSuperArrayFunction(ValueType itemType) {
        String itemTypeName = context.getNames().forMemberField(new FieldReference(
                RuntimeClass.class.getName(), "itemType"));
        isSupertypeWriter.println("JavaClass* itemType = cls->" + itemTypeName + ";");
        isSupertypeWriter.println("if (itemType == NULL) return INT32_C(0);");

        if (itemType instanceof ValueType.Primitive) {
            isSupertypeWriter.println("return itemType == &" + context.getNames().forClassInstance(itemType) + ";");
        } else {
            isSupertypeWriter.println("return " + context.getNames().forSupertypeFunction(itemType) + "(itemType);");
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
