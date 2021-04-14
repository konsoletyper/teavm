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

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.ast.ControlFlowEntry;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.backend.c.generators.Generator;
import org.teavm.backend.c.util.InteropUtil;
import org.teavm.backend.lowlevel.generate.ClassGeneratorUtil;
import org.teavm.cache.AstCacheEntry;
import org.teavm.cache.AstDependencyExtractor;
import org.teavm.cache.CacheStatus;
import org.teavm.cache.EmptyMethodNodeCache;
import org.teavm.cache.MethodNodeCache;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.NoGcRoot;
import org.teavm.interop.Structure;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.analysis.ClassMetadataRequirements;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTable;
import org.teavm.model.classes.VirtualTableEntry;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.Characteristics;
import org.teavm.runtime.CallSite;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;
import org.teavm.runtime.RuntimeReference;
import org.teavm.runtime.RuntimeReferenceQueue;

public class ClassGenerator {
    private static final Set<String> classesWithDeclaredStructures = new HashSet<>(Arrays.asList(
            "java.lang.Object", "java.lang.String", "java.lang.Class",
            RuntimeArray.class.getName(), RuntimeClass.class.getName(), RuntimeObject.class.getName(),
            WeakReference.class.getName(), ReferenceQueue.class.getName(),
            RuntimeReferenceQueue.class.getName(), RuntimeReference.class.getName()
    ));

    private GenerationContext context;
    private Decompiler decompiler;
    private CacheStatus cacheStatus;
    private TagRegistry tagRegistry;
    private CodeGenerator codeGenerator;
    private FieldReference[] staticGcRoots;
    private FieldReference[] classLayout;
    private Set<ValueType> types = new LinkedHashSet<>();
    private CodeWriter prologueWriter;
    private CodeWriter codeWriter;
    private CodeWriter initWriter;
    private CodeWriter headerWriter;
    private CodeWriter callSitesWriter;
    private IncludeManager includes;
    private IncludeManager headerIncludes;
    private MethodNodeCache astCache = EmptyMethodNodeCache.INSTANCE;
    private AstDependencyExtractor dependencyExtractor = new AstDependencyExtractor();
    private List<CallSiteDescriptor> callSites;
    private ClassMetadataRequirements metadataRequirements;
    private static final int VT_STRUCTURE_INITIALIZER_DEPTH_THRESHOLD = 9;

    public ClassGenerator(GenerationContext context, TagRegistry tagRegistry, Decompiler decompiler,
            CacheStatus cacheStatus) {
        this.context = context;
        this.tagRegistry = tagRegistry;
        this.decompiler = decompiler;
        this.cacheStatus = cacheStatus;
        metadataRequirements = new ClassMetadataRequirements(context.getDependencies());
    }

    public void setAstCache(MethodNodeCache astCache) {
        this.astCache = astCache;
    }

    public void setCallSites(List<CallSiteDescriptor> callSites) {
        this.callSites = callSites;
    }

    public void prepare(ListableClassHolderSource classes) {
        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            prepareClass(cls);
        }
    }

    private void prepareClass(ClassHolder cls) {
        types.add(ValueType.object(cls.getName()));
        if (cls.getParent() != null) {
            types.add(ValueType.object(cls.getParent()));
        }
        for (String itf : cls.getInterfaces()) {
            types.add(ValueType.object(itf));
        }
        for (MethodHolder method : cls.getMethods()) {
            if (method.getProgram() != null) {
                prepareProgram(method.getProgram());
            }
        }
    }

    private void prepareProgram(Program program) {
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction insn : block) {
                insn.acceptVisitor(prepareVisitor);
            }
        }
    }

    private void addType(ValueType type) {
        if (!types.add(type)) {
            return;
        }
        if (type instanceof ValueType.Array) {
            addType(((ValueType.Array) type).getItemType());
        }
    }

    private InstructionVisitor prepareVisitor = new AbstractInstructionVisitor() {
        @Override
        public void visit(ClassConstantInstruction insn) {
            addType(insn.getConstant());
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            addType(ValueType.object("java.lang.String"));
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            addType(ValueType.arrayOf(insn.getItemType()));
        }

        @Override
        public void visit(ConstructInstruction insn) {
            addType(ValueType.object(insn.getType()));
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            addType(insn.getType());
        }

        @Override
        public void visit(CastInstruction insn) {
            addType(insn.getTargetType());
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            ValueType type = insn.getItemType();
            for (int i = 0; i < insn.getDimensions().size(); ++i) {
                type = ValueType.arrayOf(type);
            }
            addType(type);
        }
    };

    public void generateClass(CodeWriter writer, CodeWriter headerWriter, ClassHolder cls) {
        ValueType type = ValueType.object(cls.getName());
        init(writer, headerWriter, fileName(cls.getName()), type);

        generateStringPoolDecl(type);
        generateClassStructure(cls);
        generateClassStaticFields(cls);
        generateClassMethods(cls);
        generateInitializer(cls);
        generateVirtualTable(ValueType.object(cls.getName()));
        generateStaticGCRoots(cls.getName());
        generateLayoutArray(cls.getName());
        generateStringPool(type);
    }

    private void generateCallSites(List<? extends CallSiteDescriptor> callSites, String callSitesName) {
        CallSiteGenerator generator = new CallSiteGenerator(context, callSitesWriter, includes, callSitesName);
        generator.setStatic(true);
        generator.generate(callSites);
    }

    public void generateType(CodeWriter writer, CodeWriter headerWriter, ValueType type) {
        init(writer, headerWriter, fileName(type), type);
        generateStringPoolDecl(type);
        includes.includeType(type);
        generateVirtualTable(type);
        generateStringPool(type);
    }

    private void init(CodeWriter writer, CodeWriter headerWriter, String fileName, ValueType type) {
        staticGcRoots = null;
        classLayout = null;

        includes = new SimpleIncludeManager(writer);
        includes.init(fileName + ".c");
        prologueWriter = writer.fragment();
        codeWriter = writer.fragment();
        this.headerWriter = headerWriter;

        headerWriter.println("#pragma once");
        headerIncludes = new SimpleIncludeManager(headerWriter);
        headerIncludes.init(fileName + ".h");
        headerIncludes.includePath("runtime.h");

        String currentClassName = type instanceof ValueType.Object ? ((ValueType.Object) type).getClassName() : null;

        String sysInitializerName = context.getNames().forClassSystemInitializer(type);
        headerWriter.println("extern void " + sysInitializerName + "();");
        writer.println("void " + sysInitializerName + "() {").indent();
        initWriter = writer.fragment();
        writer.outdent().println("}");
        includes.includeType(type);

        ClassGenerationContext classContext = new ClassGenerationContext(context, includes, prologueWriter,
                initWriter, currentClassName);
        codeGenerator = new CodeGenerator(classContext, codeWriter, includes);
        if (context.isLongjmp() && !context.isIncremental()) {
            codeGenerator.setCallSites(callSites);
        }
    }

    private void generateStringPoolDecl(ValueType type) {
        if (!context.isIncremental()) {
            return;
        }

        String poolName = "strings_" + context.getNames().forClassInstance(type);
        codeWriter.println("TeaVM_String* " + poolName + "[];");
        codeWriter.println("#ifdef TEAVM_GET_STRING");
        codeWriter.println("#undef TEAVM_GET_STRING");
        codeWriter.println("#endif");
        codeWriter.println("#define TEAVM_GET_STRING(i) " + poolName + "[i]");
    }

    private void generateStringPool(ValueType type) {
        if (!context.isIncremental() || context.getStringPool().getStrings().isEmpty()) {
            return;
        }

        codeWriter.println("#undef TEAVM_GET_STRING");

        String poolName = "strings_" + context.getNames().forClassInstance(type);
        StringPoolGenerator poolGenerator = new StringPoolGenerator(context, poolName);
        includes.includePath("stringhash.h");
        poolGenerator.generate(codeWriter);
        poolGenerator.generateStringPoolHeaders(initWriter, includes);
    }

    public Set<ValueType> getTypes() {
        return types;
    }

    private void generateClassMethods(ClassHolder cls) {
        boolean needsVirtualTable = needsVirtualTable(context.getCharacteristics(), ValueType.object(cls.getName()));
        for (MethodHolder method : cls.getMethods()) {
            if (method.hasModifier(ElementModifier.ABSTRACT)) {
                continue;
            }

            if (method.hasModifier(ElementModifier.NATIVE)) {
                if (tryDelegateToMethod(cls, method) || tryUsingGenerator(method)) {
                    if (needsVirtualTable) {
                        addToVirtualTable(method);
                    }
                }
                continue;
            } else if (method.getProgram() == null) {
                continue;
            }

            if (needsVirtualTable) {
                addToVirtualTable(method);
            }

            if (context.isIncremental()) {
                callSitesWriter = codeWriter.fragment();
            }

            generateMethodForwardDeclaration(method);
            RegularMethodNode methodNode;
            AstCacheEntry entry = !cacheStatus.isStaleMethod(method.getReference())
                    ? astCache.get(method.getReference(), cacheStatus)
                    : null;
            if (entry == null) {
                methodNode = decompiler.decompileRegular(method);
                astCache.store(method.getReference(), new AstCacheEntry(methodNode, new ControlFlowEntry[0]),
                        () -> dependencyExtractor.extract(methodNode));
            } else {
                methodNode = entry.method;
            }

            List<CallSiteDescriptor> callSites = null;
            if (context.isLongjmp()) {
                if (context.isIncremental()) {
                    callSites = new ArrayList<>();
                    codeGenerator.setCallSites(callSites);
                }
            }

            codeGenerator.generateMethod(methodNode);

            if (context.isIncremental()) {
                generateCallSites(method.getReference(),
                        context.isLongjmp() ? callSites : CallSiteDescriptor.extract(method.getProgram()));
                codeWriter.println("#undef TEAVM_ALLOC_STACK");
            }
        }
    }

    private void addToVirtualTable(MethodReader method) {
        if (!context.isIncremental()) {
            return;
        }
        if (method.hasModifier(ElementModifier.STATIC) || method.getLevel() == AccessLevel.PRIVATE
                || method.getName().equals("<init>")) {
            return;
        }

        String className = context.getNames().forClassInstance(ValueType.object(method.getOwnerName()));
        String idVar = codeGenerator.getClassContext().getVirtualMethodId(method.getDescriptor());
        initWriter.println("teavm_vc_registerMethod(&" + className + ", " + idVar + ", &"
                + context.getNames().forMethod(method.getReference()) + ");");
    }

    private void generateMethodForwardDeclaration(MethodHolder method) {
        if (context.isIncremental()) {
            codeGenerator.getClassContext().importMethod(method.getReference(),
                    method.hasModifier(ElementModifier.STATIC));
            return;
        }
        boolean isStatic = method.hasModifier(ElementModifier.STATIC);
        headerWriter.print("extern ");
        CodeGenerator.generateMethodSignature(headerWriter, context.getNames(), method.getReference(), isStatic, false);
        headerWriter.println(";");
    }

    private void generateCallSites(MethodReference method, List<? extends CallSiteDescriptor> callSites) {
        String callSitesName;
        if (!callSites.isEmpty()) {
            callSitesName = "callsites_" + context.getNames().forMethod(method);
            includes.includeClass(CallSite.class.getName());
            generateCallSites(callSites, callSitesName);
        } else {
            callSitesName = "NULL";
        }
        callSitesWriter.println("#define TEAVM_ALLOC_STACK(size) TEAVM_ALLOC_STACK_DEF(size, "
                + callSitesName + ")");
    }

    private void generateInitializer(ClassHolder cls) {
        if (!needsInitializer(cls)) {
            return;
        }

        String initializerName = context.getNames().forClassInitializer(cls.getName());
        headerWriter.print("extern void ").print(initializerName).println("();");

        codeWriter.print("void ").print(initializerName).println("() {").indent();

        String classInstanceName = context.getNames().forClassInstance(ValueType.object(cls.getName()));
        String clinitName = context.getNames().forMethod(
                new MethodReference(cls.getName(), "<clinit>", ValueType.VOID));
        codeWriter.print("TeaVM_Class* cls = (TeaVM_Class*) &").print(classInstanceName).println(";");
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

        String name = context.getNames().forClass(cls.getName());

        boolean writeNeeded = !classesWithDeclaredStructures.contains(cls.getName());

        if (writeNeeded) {
            headerWriter.print("typedef struct ").print(name).println(" {").indent();
        }

        if (cls.getParent() == null || !cls.getParent().equals(Structure.class.getName())) {
            String parentName = cls.getParent();
            if (parentName == null) {
                parentName = RuntimeObject.class.getName();
            }
            if (writeNeeded) {
                headerIncludes.includeClass(parentName);
                headerWriter.print("struct ").print(context.getNames().forClass(parentName)).println(" parent;");
            }
            includes.includeClass(parentName);
        }

        FieldReference[] instanceFields = new FieldReference[cls.getFields().size()];
        int instanceIndex = 0;
        for (FieldHolder field : cls.getFields()) {
            if (field.hasModifier(ElementModifier.STATIC) || isMonitorField(field.getReference())) {
                continue;
            }

            String fieldName = context.getNames().forMemberField(field.getReference());
            if (writeNeeded) {
                headerWriter.printStrictType(field.getType()).print(" ").print(fieldName).println(";");
            }
            if (isReferenceType(field.getType())) {
                instanceFields[instanceIndex++] = field.getReference();
            }
        }

        if (instanceIndex > 0) {
            classLayout = Arrays.copyOf(instanceFields, instanceIndex);
        }

        if (writeNeeded) {
            headerWriter.outdent().print("} ").print(name).println(";");
        }
    }

    private boolean isMonitorField(FieldReference field) {
        return field.getClassName().equals("java.lang.Object") && field.getFieldName().equals("monitor");
    }

    private void generateClassStaticFields(ClassHolder cls) {
        CodeWriter fieldsWriter = codeWriter.fragment();

        FieldReference[] staticFields = new FieldReference[cls.getFields().size()];
        int staticIndex = 0;
        for (FieldHolder field : cls.getFields()) {
            if (!field.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            String fieldName = context.getNames().forStaticField(field.getReference());
            headerWriter.print("extern ").printStrictType(field.getType()).print(" ").print(fieldName).println(";");
            fieldsWriter.printStrictType(field.getType()).print(" ").print(fieldName).println(";");
            if (isReferenceType(field.getType()) && field.getAnnotations().get(NoGcRoot.class.getName()) == null) {
                staticFields[staticIndex++] = field.getReference();
            }

            Object initialValue = field.getInitialValue();
            if (initialValue == null) {
                initialValue = getDefaultValue(field.getType());
            }
            initWriter.print(fieldName + " = ");
            CodeGeneratorUtil.writeValue(initWriter, context, includes, initialValue);
            initWriter.println(";");
        }

        if (staticIndex > 0) {
            staticGcRoots = Arrays.copyOf(staticFields, staticIndex);
        }
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

    private void generateVirtualTable(ValueType type) {
        if (!needsVirtualTable(context.getCharacteristics(), type)) {
            return;
        }

        generateIsSupertypeFunction(type);

        String className = null;
        if (type instanceof ValueType.Object) {
            className = ((ValueType.Object) type).getClassName();
            if (!context.isIncremental()) {
                generateVirtualTableStructure(className);
            }
        } else if (type instanceof ValueType.Array) {
            className = "java.lang.Object";
        }
        ClassReader cls = className != null ? context.getClassSource().get(className) : null;

        String structName;
        if (context.isIncremental()) {
            structName = className != null ? "TeaVM_DynamicClass" : "TeaVM_Class";
        } else {
            structName = className != null && (cls == null || !cls.hasModifier(ElementModifier.INTERFACE))
                    ? context.getNames().forClassClass(className)
                    : "TeaVM_Class";
        }
        if (className != null && !context.isIncremental()) {
            headerIncludes.includeClass(className);
        }
        String name = context.getNames().forClassInstance(type);

        String enumConstants;
        if (cls != null && cls.hasModifier(ElementModifier.ENUM)) {
            enumConstants = writeEnumConstants(cls, name);
        } else {
            enumConstants = "NULL";
        }

        headerWriter.print("extern ").print(structName).print(" ").print(name).println(";");
        if (classLayout != null) {
            codeWriter.println("static int16_t teavm_classLayouts_" + name + "[" + (classLayout.length + 1) + "];");
        }
        codeWriter.print("alignas(8) ").print(structName).print(" ").print(name);

        if (className != null) {
            if (context.isIncremental()) {
                generateDynamicVirtualTable(name, type, enumConstants);
            } else {
                VirtualTable virtualTable = context.getVirtualTableProvider().lookup(className);
                if (cls.hasModifier(ElementModifier.INTERFACE)) {
                    codeWriter.println(" = {").indent();
                    generateRuntimeClassInitializer(type, enumConstants, false, 0);
                    codeWriter.outdent().print("}");
                } else if (virtualTable != null) {
                    boolean tooDeep = getInheritanceDepth(className) > VT_STRUCTURE_INITIALIZER_DEPTH_THRESHOLD;
                    if (tooDeep) {
                        initWriter.print(structName).print("* vt_0 = &").print(name).println(";");
                    } else {
                        codeWriter.println(" = {").indent();
                    }
                    generateVirtualTableContent(virtualTable, virtualTable, type, enumConstants, tooDeep, 0);
                    if (!tooDeep) {
                        codeWriter.outdent().print("}");
                    }
                } else {
                    codeWriter.println(" = {").indent();
                    codeWriter.println(".parent = {").indent();
                    generateRuntimeClassInitializer(type, enumConstants, false, 0);
                    codeWriter.outdent().println("}");
                    codeWriter.outdent().println("}");
                }
            }
        } else {
            codeWriter.println(" = {").indent();
            generateRuntimeClassInitializer(type, enumConstants, false, 0);
            codeWriter.outdent().println("}");
        }

        codeWriter.outdent().println(";");
    }

    private int getInheritanceDepth(String className) {
        int depth = 0;
        while (true) {
            ++depth;
            ClassReader cls = context.getClassSource().get(className);
            if (cls.getParent() == null) {
                break;
            }
            className = cls.getParent();
        }
        return depth;
    }

    private void generateDynamicVirtualTable(String name, ValueType type, String enumConstants) {
        codeWriter.println(".parent = {").indent();
        generateRuntimeClassInitializer(type, enumConstants, false, 0);
        codeWriter.outdent().println("}");

        String[] parentClasses;
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = context.getClassSource().get(className);
            if (cls != null) {
                int count = cls.getInterfaces().size();
                if (cls.getParent() != null) {
                    count++;
                }
                parentClasses = new String[count];
                cls.getInterfaces().toArray(parentClasses);
                if (cls.getParent() != null) {
                    parentClasses[count - 1] = cls.getParent();
                }
            } else {
                parentClasses = new String[0];
            }
        } else {
            parentClasses = new String[] { "java.lang.Object" };
        }

        for (String parentClass : parentClasses) {
            includes.includeClass(parentClass);
            String parentClassName = context.getNames().forClassInstance(ValueType.object(parentClass));
            initWriter.println("teavm_vc_copyMethods(&" + parentClassName + ", &" + name + ");");
        }
    }

    private void generateVirtualTableContent(VirtualTable current, VirtualTable original, ValueType type,
            String enumConstants, boolean initMethod, int depth) {
        if (!initMethod) {
            codeWriter.println(".parent = {").indent();
        } else {
            String parentStructName = current.getParent() != null
                    ? context.getNames().forClassClass(current.getParent().getClassName())
                    : "TeaVM_Class";
            initWriter.print(parentStructName).print("* vt_").print(String.valueOf(depth + 1))
                    .print(" = (").print(parentStructName).print("*) vt_").print(String.valueOf(depth)).println(";");
        }

        if (current.getParent() == null) {
            generateRuntimeClassInitializer(type, enumConstants, initMethod, depth + 1);
        } else {
            generateVirtualTableContent(current.getParent(), original, type, enumConstants, initMethod, depth + 1);
        }

        if (!initMethod) {
            codeWriter.outdent().print("}");
        }

        for (MethodDescriptor method : current.getMethods()) {
            if (method == null) {
                continue;
            }
            VirtualTableEntry entry = original.getEntry(method);
            if (entry == null) {
                continue;
            }

            if (!initMethod) {
                codeWriter.println(",");
            }
            String methodName = context.getNames().forVirtualMethod(method);
            String implName = "&" + context.getNames().forMethod(entry.getImplementor());
            includes.includeClass(entry.getImplementor().getClassName());

            if (initMethod) {
                initWriter.print("vt_").print(String.valueOf(depth)).print("->").print(methodName)
                        .print(" = ").print(implName).println(";");
            } else {
                codeWriter.print(".").print(methodName).print(" = ").print(implName);
            }
        }

        if (!initMethod) {
            codeWriter.println();
        }
    }

    private String writeEnumConstants(ClassReader cls, String baseName) {
        List<FieldReader> fields = cls.getFields().stream()
                .filter(f -> f.hasModifier(ElementModifier.ENUM))
                .collect(Collectors.toList());
        String name = baseName + "_enumConstants";
        codeWriter.print("static void* " + name + "[" + (fields.size() + 1) + "] = { ");
        codeWriter.print("(void*) (intptr_t) " + fields.size());
        for (FieldReader field : fields) {
            codeWriter.print(", ").print("&" + context.getNames().forStaticField(field.getReference()));
        }
        codeWriter.println(" };");
        return name;
    }

    private void generateRuntimeClassInitializer(ValueType type, String enumConstants, boolean initMethod, int depth) {
        String sizeExpr;
        int tag;
        String parent;
        String itemTypeExpr;
        int flags = 0;
        String layout = "NULL";
        String initFunction = "NULL";
        String superinterfaceCount = "0";
        String superinterfaces = "NULL";
        String simpleName = null;
        String declaringClass = "NULL";
        String enclosingClass = "NULL";

        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = context.getClassSource().get(className);

            if (className.equals(Object.class.getName())) {
                className = RuntimeObject.class.getName();
            }

            if (cls != null && needsData(cls) && !className.equals("java.lang.Class")) {
                String structName = context.getNames().forClass(className);
                sizeExpr = "(int32_t) (intptr_t) TEAVM_ALIGN(sizeof(" + structName + "), sizeof(void*))";
            } else {
                sizeExpr = "0";
            }
            if (cls != null) {
                if (cls.hasModifier(ElementModifier.ENUM)) {
                    flags |= RuntimeClass.ENUM;
                }
                if (cls.hasModifier(ElementModifier.SYNTHETIC)) {
                    flags |= RuntimeClass.SYNTHETIC;
                }
            }
            List<TagRegistry.Range> ranges = tagRegistry != null ? tagRegistry.getRanges(className) : null;
            tag = !context.isIncremental() && ranges != null && !ranges.isEmpty() ? ranges.get(0).lower : 0;

            if (cls != null && cls.getParent() != null && types.contains(ValueType.object(cls.getParent()))) {
                includes.includeClass(cls.getParent());
                parent = "(TeaVM_Class*) &" + context.getNames().forClassInstance(ValueType.object(cls.getParent()));
            } else {
                parent = "NULL";
            }
            itemTypeExpr = "NULL";
            layout = classLayout != null ? "teavm_classLayouts_" + context.getNames().forClassInstance(type) : "NULL";

            if (cls != null && needsInitializer(cls)) {
                initFunction = context.getNames().forClassInitializer(className);
            }

            Set<String> interfaces = cls != null
                    ? cls.getInterfaces().stream()
                            .filter(c -> types.contains(ValueType.object(c)))
                            .collect(Collectors.toSet())
                    : Collections.emptySet();
            if (!interfaces.isEmpty()) {
                superinterfaceCount = Integer.toString(cls.getInterfaces().size());
                StringBuilder sb = new StringBuilder("(TeaVM_Class*[]) { ");
                boolean first = true;
                for (String itf : interfaces) {
                    if (!first) {
                        sb.append(", ");
                    }
                    first = false;
                    includes.includeClass(itf);
                    sb.append("(TeaVM_Class*) &").append(context.getNames().forClassInstance(ValueType.object(itf)));
                }
                superinterfaces = sb.append(" }").toString();
            }

            switch (className) {
                case "java.lang.ref.WeakReference":
                    flags |= RuntimeClass.VM_TYPE_WEAKREFERENCE << RuntimeClass.VM_TYPE_SHIFT;
                    break;
                case "java.lang.ref.ReferenceQueue":
                    flags |= RuntimeClass.VM_TYPE_REFERENCEQUEUE << RuntimeClass.VM_TYPE_SHIFT;
                    break;
            }

            if (cls != null) {
                simpleName = cls.getSimpleName();

                if (cls.getDeclaringClassName() != null
                        && context.getDependencies().getClass(cls.getDeclaringClassName()) != null) {
                    declaringClass = "(TeaVM_Class*) &" + context.getNames().forClassInstance(
                            ValueType.object(cls.getDeclaringClassName()));
                    includes.includeClass(cls.getDeclaringClassName());
                }

                if (cls.getOwnerName() != null
                        && context.getDependencies().getClass(cls.getOwnerName()) != null) {
                    enclosingClass = "(TeaVM_Class*) &" + context.getNames().forClassInstance(
                            ValueType.object(cls.getOwnerName()));
                    includes.includeClass(cls.getOwnerName());
                }
            }

        } else if (type instanceof ValueType.Array) {
            includes.includeClass("java.lang.Object");
            parent = "(TeaVM_Class*) &" + context.getNames().forClassInstance(ValueType.object("java.lang.Object"));
            tag = !context.isIncremental() ? tagRegistry.getRanges("java.lang.Object").get(0).lower : 0;
            ValueType itemType = ((ValueType.Array) type).getItemType();
            sizeExpr = "sizeof(" + CodeWriter.strictTypeAsString(itemType) + ")";
            includes.includeType(itemType);
            itemTypeExpr = "(TeaVM_Class*) &" + context.getNames().forClassInstance(itemType);
        } else if (type == ValueType.VOID) {
            parent = "NULL";
            tag = 0;
            sizeExpr = "0";
            itemTypeExpr = "NULL";
            flags |= RuntimeClass.PRIMITIVE;
            flags = ClassGeneratorUtil.applyPrimitiveFlags(flags, type);
        } else {
            parent = "NULL";
            tag = Integer.MAX_VALUE;
            sizeExpr = "sizeof(" + CodeWriter.strictTypeAsString(type) + ")";
            flags |= RuntimeClass.PRIMITIVE;
            flags = ClassGeneratorUtil.applyPrimitiveFlags(flags, type);
            itemTypeExpr = "NULL";
        }

        String metadataName = nameOfType(type);
        String nameRef = metadataName != null
                ? "(TeaVM_Object**) TEAVM_GET_STRING_ADDRESS("
                + context.getStringPool().getStringIndex(metadataName) + ")"
                : "NULL";
        String superTypeFunction = context.getNames().forSupertypeFunction(type);

        ValueType arrayType = ValueType.arrayOf(type);
        String arrayTypeExpr;
        if (types.contains(arrayType)) {
            includes.includeType(arrayType);
            arrayTypeExpr = "(TeaVM_Class*) &" + context.getNames().forClassInstance(arrayType);
        } else {
            arrayTypeExpr = "NULL";
        }

        if (simpleName == null) {
            simpleName = "NULL";
        } else {
            int simpleNameIndex = context.getStringPool().getStringIndex(simpleName);
            simpleName = "(TeaVM_Object**) TEAVM_GET_STRING_ADDRESS(" + simpleNameIndex + ")";
        }

        includes.includePath("strings.h");

        List<FieldInitializer> initializers = new ArrayList<>();
        initializers.add(new FieldInitializer("size", sizeExpr));
        initializers.add(new FieldInitializer("flags", String.valueOf(flags)));
        initializers.add(new FieldInitializer("tag", String.valueOf(tag)));
        initializers.add(new FieldInitializer("canary", "0"));
        initializers.add(new FieldInitializer("name", nameRef));
        initializers.add(new FieldInitializer("simpleName", simpleName));
        initializers.add(new FieldInitializer("arrayType", arrayTypeExpr));
        initializers.add(new FieldInitializer("itemType", itemTypeExpr));
        initializers.add(new FieldInitializer("isSupertypeOf", "&" + superTypeFunction));
        initializers.add(new FieldInitializer("superclass", parent));
        initializers.add(new FieldInitializer("superinterfaceCount", superinterfaceCount));
        initializers.add(new FieldInitializer("superinterfaces", superinterfaces));
        initializers.add(new FieldInitializer("layout", layout));
        initializers.add(new FieldInitializer("enumValues", enumConstants));
        initializers.add(new FieldInitializer("declaringClass", declaringClass));
        initializers.add(new FieldInitializer("enclosingClass", enclosingClass));
        initializers.add(new FieldInitializer("init", initFunction));

        if (initMethod) {
            for (FieldInitializer initializer : initializers) {
                initWriter.print("vt_").print(String.valueOf(depth)).print("->").print(initializer.name)
                        .print(" = ").print(initializer.value).println(";");
            }
        } else {
            for (int i = 0; i < initializers.size(); ++i) {
                if (i > 0) {
                    codeWriter.println(",");
                }
                FieldInitializer initializer = initializers.get(i);
                codeWriter.print(".").print(initializer.name).print(" = ").print(initializer.value);
            }
        }

        if (context.isHeapDump() && type instanceof ValueType.Object) {
            ClassReader cls = context.getClassSource().get(((ValueType.Object) type).getClassName());
            generateHeapDumpMetadata(initMethod ? initWriter : codeWriter, cls, initMethod, depth);
        }

        if (!initMethod) {
            codeWriter.println();
        }
    }

    static class FieldInitializer {
        final String name;
        final String value;

        FieldInitializer(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private void generateHeapDumpMetadata(CodeWriter codeWriter, ClassReader cls, boolean initMethod,
            int depth) {
        List<HeapDumpField> fields = getHeapDumpFields(cls);
        List<HeapDumpField> staticFields = getHeapDumpStaticFields(cls);
        if (staticFields.isEmpty() && fields.isEmpty()) {
            return;
        }
        codeWriter.println().println("#if TEAVM_HEAP_DUMP").indent();
        if (!fields.isEmpty()) {
            if (initMethod) {
                codeWriter.print("vt_" + depth + "->");
            } else {
                codeWriter.println(",");
                codeWriter.print(".");
            }

            codeWriter.println("fieldDescriptors = (TeaVM_FieldDescriptors*) "
                    + "&(struct { uint32_t count; TeaVM_FieldDescriptor data["
                    + fields.size() + "]; }) {").indent();
            generateHeapDumpFields(codeWriter, fields);
            codeWriter.outdent().print("}");
            if (initMethod) {
                codeWriter.println(";");
            }
        }
        if (!staticFields.isEmpty()) {
            if (initMethod) {
                codeWriter.print("vt_" + depth + "->");
            } else {
                codeWriter.println(",");
                codeWriter.print(".");
            }
            codeWriter.println("staticFieldDescriptors = (TeaVM_StaticFieldDescriptors*) "
                    + "&(struct { uint32_t count; TeaVM_StaticFieldDescriptor data["
                    + staticFields.size() + "]; }) {").indent();
            generateHeapDumpFields(codeWriter, staticFields);
            codeWriter.outdent().print("}");
            if (initMethod) {
                codeWriter.println(";");
            }
        }
        codeWriter.println().outdent().println("#endif");
    }

    private void generateHeapDumpFields(CodeWriter codeWriter, List<HeapDumpField> fields) {
        codeWriter.println(".count = " + fields.size() + ",");
        codeWriter.println(".data = {").indent();
        for (int i = 0; i < fields.size(); ++i) {
            if (i > 0) {
                codeWriter.println(",");
            }
            HeapDumpField field = fields.get(i);
            codeWriter.print("{ .name = u");
            StringPoolGenerator.generateSimpleStringLiteral(codeWriter, field.name);
            codeWriter.print(", .offset = " + field.offset + ", .type = " + field.type + " }");
        }
        codeWriter.println().outdent().println("}");
    }

    private static final String TYPE_OBJECT = "TEAVM_FIELD_TYPE_OBJECT";

    private List<HeapDumpField> getHeapDumpFields(ClassReader cls) {
        List<HeapDumpField> fields = new ArrayList<>();
        switch (cls.getName()) {
            case "java.lang.Object":
            case "java.lang.ref.ReferenceQueue":
            case "java.lang.ref.WeakReference":
            case "java.lang.ref.SoftReference":
                break;
            case "java.lang.Class":
                fields.add(new HeapDumpField("name", "offsetof(TeaVM_Class, name)", TYPE_OBJECT));
                fields.add(new HeapDumpField("simpleName", "offsetof(TeaVM_Class, simpleName)", TYPE_OBJECT));
                break;
            case "java.lang.ref.Reference":
                fields.add(new HeapDumpField("referent", "offsetof(TeaVM_Reference, object)", TYPE_OBJECT));
                fields.add(new HeapDumpField("queue", "offsetof(TeaVM_Reference, queue)", TYPE_OBJECT));
                break;
            default: {
                for (FieldReader field : cls.getFields()) {
                    if (field.hasModifier(ElementModifier.STATIC) || !isManaged(field)) {
                        continue;
                    }
                    String className = context.getNames().forClass(cls.getName());
                    String offset = "offsetof(" + className + ", "
                            + context.getNames().forMemberField(field.getReference()) + ")";
                    fields.add(new HeapDumpField(field.getName(), offset, typeForHeapDump(field.getType())));
                }
                break;
            }
        }
        return fields;
    }

    private List<HeapDumpField> getHeapDumpStaticFields(ClassReader cls) {
        List<HeapDumpField> fields = new ArrayList<>();
        switch (cls.getName()) {
            case "java.lang.Object":
            case "java.lang.Class":
            case "java.lang.ref.ReferenceQueue":
            case "java.lang.ref.Reference":
            case "java.lang.ref.WeakReference":
            case "java.lang.ref.SoftReference":
                break;
            default: {
                for (FieldReader field : cls.getFields()) {
                    if (!field.hasModifier(ElementModifier.STATIC) || !isManaged(field)) {
                        continue;
                    }
                    String offset = "(unsigned char*) &" + context.getNames().forStaticField(field.getReference());
                    fields.add(new HeapDumpField(field.getName(), offset, typeForHeapDump(field.getType())));
                }
                break;
            }
        }
        return fields;
    }

    private boolean isManaged(FieldReader field) {
        ValueType type = field.getType();
        return !(type instanceof ValueType.Object)
                || context.getCharacteristics().isManaged(((ValueType.Object) type).getClassName());
    }

    static String typeForHeapDump(ValueType type) {
        String result = "127";
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    result = "TEAVM_FIELD_TYPE_BOOLEAN";
                    break;
                case BYTE:
                    result = "TEAVM_FIELD_TYPE_BYTE";
                    break;
                case SHORT:
                    result = "TEAVM_FIELD_TYPE_SHORT";
                    break;
                case CHARACTER:
                    result = "TEAVM_FIELD_TYPE_CHAR";
                    break;
                case INTEGER:
                    result = "TEAVM_FIELD_TYPE_INT";
                    break;
                case FLOAT:
                    result = "TEAVM_FIELD_TYPE_FLOAT";
                    break;
                case LONG:
                    result = "TEAVM_FIELD_TYPE_LONG";
                    break;
                case DOUBLE:
                    result = "TEAVM_FIELD_TYPE_DOUBLE";
                    break;
            }
        } else if (type instanceof ValueType.Array) {
            result = "TEAVM_FIELD_TYPE_ARRAY";
        } else {
            result = "TEAVM_FIELD_TYPE_OBJECT";
        }
        return result;
    }

    static class HeapDumpField {
        String name;
        String offset;
        String type;

        HeapDumpField(String name, String offset, String type) {
            this.name = name;
            this.offset = offset;
            this.type = type;
        }
    }

    private void generateVirtualTableStructure(String className) {
        String name = context.getNames().forClassClass(className);

        headerWriter.print("typedef struct ").print(name).println(" {").indent();

        VirtualTable virtualTable = context.getVirtualTableProvider().lookup(className);
        if (virtualTable != null) {
            String parentName = "TeaVM_Class";
            int index = 0;
            if (virtualTable.getParent() != null) {
                headerIncludes.includeClass(virtualTable.getParent().getClassName());
                parentName = context.getNames().forClassClass(virtualTable.getParent().getClassName());
                index = virtualTable.getParent().size();
            }

            headerWriter.println(parentName + " parent;");
            int padIndex = 0;
            for (MethodDescriptor method : virtualTable.getMethods()) {
                if (method != null) {
                    String methodName = context.getNames().forVirtualMethod(method);
                    headerWriter.printType(method.getResultType())
                            .print(" (*").print(methodName).print(")(");
                    CodeGenerator.generateMethodParameters(headerWriter, method, false, false);
                    headerWriter.print(")");
                } else {
                    headerWriter.print("void (*pad" + padIndex++ + ")()");
                }
                headerWriter.println("; // " + index++);
            }
        }

        headerWriter.outdent().print("} ").print(name).println(";");
    }

    private boolean isReferenceType(ValueType type) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            return !context.getCharacteristics().isStructure(className)
                    && !context.getCharacteristics().isFunction(className)
                    && !className.equals(Address.class.getName());
        } else {
            return type instanceof ValueType.Array;
        }
    }

    private void generateStaticGCRoots(String className) {
        if (staticGcRoots == null) {
            return;
        }

        String suffix = context.getNames().forClassInstance(ValueType.object(className));
        String varName = "teavm_gc_localStaticRoots_" + suffix;
        codeWriter.println("static void** " + varName + "[" + staticGcRoots.length + "] = {").indent();

        boolean first = true;
        for (FieldReference field : staticGcRoots) {
            if (!first) {
                codeWriter.print(", ");
            }
            first = false;
            String name = context.getNames().forStaticField(field);
            codeWriter.print("(void**) &").print(name);
        }

        codeWriter.println().outdent().println("};");
        initWriter.println("teavm_registerStaticGcRoots(" + varName + ", " + staticGcRoots.length + ");");
    }

    private void generateLayoutArray(String className) {
        if (classLayout == null) {
            return;
        }

        String name = context.getNames().forClassInstance(ValueType.object(className));
        codeWriter.print("static int16_t teavm_classLayouts_" + name + "[" + (classLayout.length + 1) + "] = {")
                .indent();
        codeWriter.println().print("INT16_C(" + classLayout.length + ")");

        for (FieldReference field : classLayout) {
            String structName = context.getNames().forClass(field.getClassName());
            String fieldName = context.getNames().forMemberField(field);
            codeWriter.print(", (int16_t) offsetof(" + structName + ", " + fieldName + ")");
        }
        codeWriter.println().outdent().println("};");
    }

    private boolean needsData(ClassReader cls) {
        if (cls.hasModifier(ElementModifier.INTERFACE)) {
            return false;
        }
        if (InteropUtil.isNative(cls)) {
            return false;
        }
        return !cls.getName().equals(Structure.class.getName())
                && !cls.getName().equals(Address.class.getName());
    }

    public static boolean needsVirtualTable(Characteristics characteristics, ValueType type) {
        if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            return characteristics.isManaged(className);
        } else if (type instanceof ValueType.Array) {
            return needsVirtualTable(characteristics, ((ValueType.Array) type).getItemType());
        } else {
            return true;
        }
    }

    private boolean needsInitializer(ClassReader cls) {
        return !context.getCharacteristics().isStaticInit(cls.getName())
                && !context.getCharacteristics().isStructure(cls.getName())
                && cls.getMethod(new MethodDescriptor("<clinit>", ValueType.VOID)) != null
                && context.getClassInitializerInfo().isDynamicInitializer(cls.getName());
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
        CodeGenerator.generateMethodSignature(codeWriter, context.getNames(), callingMethod.getReference(),
                callingMethod.hasModifier(ElementModifier.STATIC), true);
        codeWriter.println(" {").indent();

        if (callingMethod.getResultType() != ValueType.VOID) {
            codeWriter.print("return ");
        }

        codeWriter.print(context.getNames().forMethod(delegateMethod.getReference())).print("(");

        boolean isStatic = callingMethod.hasModifier(ElementModifier.STATIC);
        int start = 0;
        if (!isStatic) {
            codeWriter.print("teavm_this_");
        } else {
            if (callingMethod.parameterCount() > 0) {
                codeWriter.print("teavm_local_1");
            }
            start++;
        }

        for (int i = start; i < callingMethod.parameterCount(); ++i) {
            codeWriter.print(", ").print("teavm_local_").print(String.valueOf(i + 1));
        }

        codeWriter.println(");");

        codeWriter.outdent().println("}");
    }

    private boolean tryUsingGenerator(MethodHolder method) {
        MethodReference methodRef = method.getReference();
        Generator generator = context.getGenerator(methodRef);
        if (generator == null) {
            return false;
        }

        generateMethodForwardDeclaration(method);
        CodeWriter writerBefore = codeWriter.fragment();
        boolean isStatic = method.hasModifier(ElementModifier.STATIC);
        CodeGenerator.generateMethodSignature(codeWriter, context.getNames(), methodRef, isStatic, true);
        codeWriter.println(" {").indent();
        CodeWriter bodyWriter = codeWriter.fragment();
        codeWriter.outdent().println("}");

        GeneratorContextImpl generatorContext = new GeneratorContextImpl(codeGenerator.getClassContext(),
                bodyWriter, writerBefore, codeWriter, includes, callSites, context.isLongjmp());
        generator.generate(generatorContext, methodRef);
        try {
            generatorContext.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public String nameOfType(ValueType type) {
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
            if (isArrayOfPrimitives(type)) {
                return type.toString().replace('/', '.');
            } else {
                return null;
            }
        } else if (type == ValueType.VOID) {
            return "void";
        } else if (type instanceof ValueType.Object) {
            String name = ((ValueType.Object) type).getClassName();
            return metadataRequirements.getInfo(name).name() ? name : null;
        } else {
            throw new AssertionError();
        }
    }

    private static boolean isArrayOfPrimitives(ValueType type) {
        while (type instanceof ValueType.Array) {
            type = ((ValueType.Array) type).getItemType();
        }
        return type instanceof ValueType.Primitive || type == ValueType.VOID;
    }

    private void generateIsSupertypeFunction(ValueType type) {
        String name = context.getNames().forSupertypeFunction(type);
        headerWriter.println("extern int32_t " + name + "(TeaVM_Class*);");
        codeWriter.println("int32_t " + name + "(TeaVM_Class* cls) {").indent();

        if (type instanceof ValueType.Object) {
            generateIsSuperclassFunction(((ValueType.Object) type).getClassName());
        } else if (type instanceof ValueType.Primitive) {
            generateIsSuperPrimitiveFunction((ValueType.Primitive) type);
        } else if (type == ValueType.VOID) {
            generateIsSuperclassFunction("java.lang.Void");
        } else if (type instanceof ValueType.Array) {
            generateIsSuperArrayFunction(((ValueType.Array) type).getItemType());
        }

        codeWriter.outdent().println("}");
    }

    private void generateIsSuperclassFunction(String className) {
        if (context.isIncremental()) {
            generateIncrementalSuperclassFunction(className);
        } else {
            generateFastIsSuperclassFunction(className);
        }
    }

    private void generateFastIsSuperclassFunction(String className) {
        List<TagRegistry.Range> ranges = tagRegistry.getRanges(className);
        if (ranges.isEmpty()) {
            codeWriter.println("return INT32_C(0);");
            return;
        }

        String tagName = context.getNames().forMemberField(new FieldReference(
                RuntimeClass.class.getName(), "tag"));
        codeWriter.println("int32_t tag = cls->" + tagName + ";");

        int lower = ranges.get(0).lower;
        int upper = ranges.get(ranges.size() - 1).upper;
        codeWriter.println("if (tag < " + lower + " || tag >= " + upper + ") return INT32_C(0);");

        for (int i = 1; i < ranges.size(); ++i) {
            lower = ranges.get(i - 1).upper;
            upper = ranges.get(i).lower;
            codeWriter.println("if (tag >= " + lower + " && tag < " + upper + ") return INT32_C(0);");
        }

        codeWriter.println("return INT32_C(1);");
    }

    private void generateIncrementalSuperclassFunction(String className) {
        String functionName = context.getNames().forSupertypeFunction(ValueType.object(className));
        ClassReader cls = context.getClassSource().get(className);
        if (cls != null && types.contains(ValueType.object(className))) {
            includes.includeClass(className);
            String name = context.getNames().forClassInstance(ValueType.object(className));
            codeWriter.println("if (cls == (TeaVM_Class*) &" + name + ") return INT32_C(1);");

            codeWriter.println("if (cls->superclass != NULL && " + functionName + "(cls->superclass)) "
                    + "return INT32_C(1);");
            codeWriter.println("for (int32_t i = 0; i < cls->superinterfaceCount; ++i) {").indent();
            codeWriter.println("if (" + functionName + "(cls->superinterfaces[i])) "
                    + "return INT32_C(1);");
            codeWriter.outdent().println("}");
        }

        codeWriter.println("return INT32_C(0);");
    }

    private void generateIsSuperArrayFunction(ValueType itemType) {
        String itemTypeName = context.getNames().forMemberField(new FieldReference(
                RuntimeClass.class.getName(), "itemType"));
        codeWriter.println("TeaVM_Class* itemType = cls->" + itemTypeName + ";");
        codeWriter.println("if (itemType == NULL) return INT32_C(0);");

        if (itemType instanceof ValueType.Primitive) {
            codeWriter.println("return itemType == &" + context.getNames().forClassInstance(itemType) + ";");
        } else {
            codeWriter.println("return " + context.getNames().forSupertypeFunction(itemType) + "(itemType);");
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

    public static String fileName(ValueType type) {
        StringBuilder sb = new StringBuilder();
        fileNameRec(type, sb);
        return sb.toString();
    }

    private static void fileNameRec(ValueType type, StringBuilder sb) {
        if (type instanceof ValueType.Object) {
            sb.append("classes/");
            escape(((ValueType.Object) type).getClassName(), sb);
        } else if (type instanceof ValueType.Array) {
            sb.append("arrays/");
            fileNameRec(((ValueType.Array) type).getItemType(), sb);
        } else if (type instanceof ValueType.Primitive) {
            sb.append("primitives/");
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    sb.append("boolean");
                    break;
                case BYTE:
                    sb.append("byte");
                    break;
                case SHORT:
                    sb.append("short");
                    break;
                case CHARACTER:
                    sb.append("char");
                    break;
                case INTEGER:
                    sb.append("int");
                    break;
                case LONG:
                    sb.append("long");
                    break;
                case FLOAT:
                    sb.append("float");
                    break;
                case DOUBLE:
                    sb.append("double");
                    break;
            }
        } else if (type == ValueType.VOID) {
            sb.append("primitives/void");
        }
    }

    public static String fileName(String className) {
        StringBuilder sb = new StringBuilder("classes/");
        escape(className, sb);
        return sb.toString();
    }

    static void escape(String className, StringBuilder sb) {
        for (int i = 0; i < className.length(); ++i) {
            char c = className.charAt(i);
            switch (c) {
                case '.':
                    sb.append('/');
                    break;
                case '@':
                    sb.append("@@");
                    break;
                case '/':
                    sb.append("@s");
                    break;
                case '\\':
                    sb.append("@b");
                    break;
                case ':':
                    sb.append("@c");
                    break;
                case ';':
                    sb.append("@e");
                    break;
                case '*':
                    sb.append("@m");
                    break;
                case '"':
                    sb.append("@q");
                    break;
                case '<':
                    sb.append("@l");
                    break;
                case '>':
                    sb.append("@g");
                    break;
                case '|':
                    sb.append("@p");
                    break;
                case '$':
                    sb.append("@d");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
    }
}
