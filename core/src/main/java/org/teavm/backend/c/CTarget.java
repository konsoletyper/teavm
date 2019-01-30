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
package org.teavm.backend.c;

import com.carrotsearch.hppc.ObjectByteHashMap;
import com.carrotsearch.hppc.ObjectByteMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.backend.c.analyze.CDependencyListener;
import org.teavm.backend.c.generate.BufferedCodeWriter;
import org.teavm.backend.c.generate.ClassGenerator;
import org.teavm.backend.c.generate.CodeGeneratorUtil;
import org.teavm.backend.c.generate.CodeWriter;
import org.teavm.backend.c.generate.GenerationContext;
import org.teavm.backend.c.generate.NameProvider;
import org.teavm.backend.c.generate.StringPool;
import org.teavm.backend.c.generators.ArrayGenerator;
import org.teavm.backend.c.generators.Generator;
import org.teavm.backend.c.intrinsic.AddressIntrinsic;
import org.teavm.backend.c.intrinsic.AllocatorIntrinsic;
import org.teavm.backend.c.intrinsic.ExceptionHandlingIntrinsic;
import org.teavm.backend.c.intrinsic.FunctionIntrinsic;
import org.teavm.backend.c.intrinsic.GCIntrinsic;
import org.teavm.backend.c.intrinsic.IntegerIntrinsic;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicFactory;
import org.teavm.backend.c.intrinsic.LongIntrinsic;
import org.teavm.backend.c.intrinsic.MutatorIntrinsic;
import org.teavm.backend.c.intrinsic.PlatformClassIntrinsic;
import org.teavm.backend.c.intrinsic.PlatformClassMetadataIntrinsic;
import org.teavm.backend.c.intrinsic.PlatformIntrinsic;
import org.teavm.backend.c.intrinsic.PlatformObjectIntrinsic;
import org.teavm.backend.c.intrinsic.RuntimeClassIntrinsic;
import org.teavm.backend.c.intrinsic.ShadowStackIntrinsic;
import org.teavm.backend.c.intrinsic.StructureIntrinsic;
import org.teavm.cache.AlwaysStaleCacheStatus;
import org.teavm.dependency.ClassDependency;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.dependency.DependencyListener;
import org.teavm.interop.Address;
import org.teavm.interop.PlatformMarkers;
import org.teavm.interop.Structure;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTableProvider;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.lowlevel.Characteristics;
import org.teavm.model.lowlevel.ClassInitializerEliminator;
import org.teavm.model.lowlevel.ClassInitializerTransformer;
import org.teavm.model.lowlevel.ExportDependencyListener;
import org.teavm.model.lowlevel.NullCheckInsertion;
import org.teavm.model.lowlevel.NullCheckTransformation;
import org.teavm.model.lowlevel.ShadowStackTransformer;
import org.teavm.model.transformation.ClassInitializerInsertionTransformer;
import org.teavm.model.transformation.ClassPatch;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.ExceptionHandling;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVMEntryPoint;
import org.teavm.vm.TeaVMTarget;
import org.teavm.vm.TeaVMTargetController;
import org.teavm.vm.spi.TeaVMHostExtension;

public class CTarget implements TeaVMTarget, TeaVMCHost {
    private TeaVMTargetController controller;
    private ClassInitializerInsertionTransformer clinitInsertionTransformer;
    private ClassInitializerEliminator classInitializerEliminator;
    private ClassInitializerTransformer classInitializerTransformer;
    private ShadowStackTransformer shadowStackTransformer;
    private NullCheckInsertion nullCheckInsertion;
    private NullCheckTransformation nullCheckTransformation;
    private ExportDependencyListener exportDependencyListener = new ExportDependencyListener();
    private int minHeapSize = 32 * 1024 * 1024;
    private List<IntrinsicFactory> intrinsicFactories = new ArrayList<>();

    public void setMinHeapSize(int minHeapSize) {
        this.minHeapSize = minHeapSize;
    }

    @Override
    public List<ClassHolderTransformer> getTransformers() {
        List<ClassHolderTransformer> transformers = new ArrayList<>();
        transformers.add(new ClassPatch());
        transformers.add(new CDependencyListener());
        return transformers;
    }

    @Override
    public List<DependencyListener> getDependencyListeners() {
        return Arrays.asList(new CDependencyListener(), exportDependencyListener);
    }

    @Override
    public void setController(TeaVMTargetController controller) {
        this.controller = controller;
        Characteristics characteristics = new Characteristics(controller.getUnprocessedClassSource());
        classInitializerEliminator = new ClassInitializerEliminator(controller.getUnprocessedClassSource());
        classInitializerTransformer = new ClassInitializerTransformer();
        shadowStackTransformer = new ShadowStackTransformer(characteristics);
        clinitInsertionTransformer = new ClassInitializerInsertionTransformer(controller.getUnprocessedClassSource());
        nullCheckInsertion = new NullCheckInsertion(characteristics);
        nullCheckTransformation = new NullCheckTransformation();
    }

    @Override
    public List<TeaVMHostExtension> getHostExtensions() {
        return Collections.singletonList(this);
    }

    @Override
    public void addIntrinsic(IntrinsicFactory intrinsicFactory) {
        intrinsicFactories.add(intrinsicFactory);
    }

    @Override
    public boolean requiresRegisterAllocation() {
        return true;
    }

    @Override
    public void contributeDependencies(DependencyAnalyzer dependencyAnalyzer) {
        dependencyAnalyzer.linkMethod(new MethodReference(Allocator.class, "allocate",
                RuntimeClass.class, Address.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Allocator.class, "allocateArray",
                RuntimeClass.class, int.class, Address.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Allocator.class, "allocateMultiArray",
                RuntimeClass.class, Address.class, int.class, RuntimeArray.class)).use();

        dependencyAnalyzer.linkMethod(new MethodReference(Allocator.class, "<clinit>", void.class)).use();

        dependencyAnalyzer.linkMethod(new MethodReference(ExceptionHandling.class, "throwException",
                Throwable.class, void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(ExceptionHandling.class, "throwClassCastException",
                void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(ExceptionHandling.class, "throwNullPointerException",
                void.class)).use();

        dependencyAnalyzer.linkMethod(new MethodReference(ExceptionHandling.class, "catchException",
                Throwable.class)).use();

        dependencyAnalyzer.linkClass("java.lang.String");
        dependencyAnalyzer.linkClass("java.lang.Class");
        dependencyAnalyzer.linkField(new FieldReference("java.lang.String", "hashCode"));

        ClassDependency runtimeClassDep = dependencyAnalyzer.linkClass(RuntimeClass.class.getName());
        ClassDependency runtimeObjectDep = dependencyAnalyzer.linkClass(RuntimeObject.class.getName());
        ClassDependency runtimeArrayDep = dependencyAnalyzer.linkClass(RuntimeArray.class.getName());
        for (ClassDependency classDep : Arrays.asList(runtimeClassDep, runtimeObjectDep, runtimeArrayDep)) {
            for (FieldReader field : classDep.getClassReader().getFields()) {
                dependencyAnalyzer.linkField(field.getReference());
            }
        }
    }

    @Override
    public void beforeOptimizations(Program program, MethodReader method) {
        nullCheckInsertion.transformProgram(program, method.getReference());
    }

    @Override
    public void afterOptimizations(Program program, MethodReader method) {
        clinitInsertionTransformer.apply(method, program);
        classInitializerEliminator.apply(program);
        classInitializerTransformer.transform(program);
        nullCheckTransformation.apply(program, method.getResultType());
        shadowStackTransformer.apply(program, method);
    }

    @Override
    public void emit(ListableClassHolderSource classes, BuildTarget buildTarget, String outputName) throws IOException {
        VirtualTableProvider vtableProvider = createVirtualTableProvider(classes);
        TagRegistry tagRegistry = new TagRegistry(classes);
        StringPool stringPool = new StringPool();

        Decompiler decompiler = new Decompiler(classes, controller.getClassLoader(),
                AlwaysStaleCacheStatus.INSTANCE, new HashSet<>(), new HashSet<>(), false, true);
        Characteristics characteristics = new Characteristics(controller.getUnprocessedClassSource());

        NameProvider nameProvider = new NameProvider(controller.getUnprocessedClassSource());

        List<Intrinsic> intrinsics = new ArrayList<>();
        intrinsics.add(new ShadowStackIntrinsic());
        intrinsics.add(new AddressIntrinsic());
        intrinsics.add(new AllocatorIntrinsic());
        intrinsics.add(new StructureIntrinsic(characteristics));
        intrinsics.add(new PlatformIntrinsic());
        intrinsics.add(new PlatformObjectIntrinsic());
        intrinsics.add(new PlatformClassIntrinsic());
        intrinsics.add(new PlatformClassMetadataIntrinsic());
        intrinsics.add(new GCIntrinsic());
        intrinsics.add(new MutatorIntrinsic());
        intrinsics.add(new ExceptionHandlingIntrinsic());
        intrinsics.add(new FunctionIntrinsic(characteristics, exportDependencyListener.getResolvedMethods()));
        intrinsics.add(new RuntimeClassIntrinsic());
        intrinsics.add(new LongIntrinsic());
        intrinsics.add(new IntegerIntrinsic());

        List<Generator> generators = new ArrayList<>();
        generators.add(new ArrayGenerator());

        GenerationContext context = new GenerationContext(vtableProvider, characteristics, stringPool, nameProvider,
                controller.getDiagnostics(), classes, intrinsics, generators);

        BufferedCodeWriter codeWriter = new BufferedCodeWriter();
        copyResource(codeWriter, "runtime.c");

        ClassGenerator classGenerator = new ClassGenerator(context, controller.getUnprocessedClassSource(),
                tagRegistry, decompiler, codeWriter);
        IntrinsicFactoryContextImpl intrinsicFactoryContext = new IntrinsicFactoryContextImpl(
                classGenerator.getStructuresWriter(), classGenerator.getPreCodeWriter(),
                controller.getUnprocessedClassSource(), controller.getClassLoader(), controller.getServices(),
                controller.getProperties());
        for (IntrinsicFactory intrinsicFactory : intrinsicFactories) {
            context.addIntrinsic(intrinsicFactory.createIntrinsic(intrinsicFactoryContext));
        }

        generateClasses(classes, classGenerator);
        generateSpecialFunctions(context, codeWriter);
        copyResource(codeWriter, "runtime-epilogue.c");

        List<ValueType> types = classGenerator.getTypes().stream()
                .filter(c -> ClassGenerator.needsVirtualTable(characteristics, c))
                .collect(Collectors.toList());

        generateArrayOfClassReferences(context, codeWriter, types);
        generateMain(context, codeWriter, classes, types);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                buildTarget.createResource(outputName), StandardCharsets.UTF_8))) {
            codeWriter.writeTo(writer);
        }
    }

    private void copyResource(CodeWriter writer, String resourceName) {
        ClassLoader classLoader = CTarget.class.getClassLoader();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                classLoader.getResourceAsStream("org/teavm/backend/c/" + resourceName)))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                writer.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateClasses(ListableClassHolderSource classes, ClassGenerator classGenerator) {
        List<String> classNames = sortClassNames(classes);

        for (String className : classNames) {
            ClassHolder cls = classes.get(className);
            classGenerator.generateClass(cls);
        }

        classGenerator.generateRemainingData(classNames, shadowStackTransformer);
    }

    private List<String> sortClassNames(ListableClassReaderSource classes) {
        List<String> classNames = new ArrayList<>(classes.getClassNames().size());
        Deque<String> stack = new ArrayDeque<>(classes.getClassNames());
        ObjectByteMap<String> stateMap = new ObjectByteHashMap<>();

        while (!stack.isEmpty()) {
            String className = stack.pop();
            byte state = stateMap.getOrDefault(className, (byte) 0);
            switch (state) {
                case 0: {
                    stateMap.put(className, (byte) 1);
                    stack.push(className);
                    ClassReader cls = classes.get(className);
                    String parent = cls != null ? cls.getParent() : null;
                    if (parent == null) {
                        parent = RuntimeObject.class.getName();
                    }
                    if (!parent.equals(Structure.class.getName())
                            && stateMap.getOrDefault(cls.getParent(), (byte) 0) == 0) {
                        stack.push(parent);
                    }
                    break;
                }
                case 1:
                    stateMap.put(className, (byte) 2);
                    classNames.add(className);
                    break;
            }
        }

        return classNames;
    }

    private VirtualTableProvider createVirtualTableProvider(ListableClassHolderSource classes) {
        Set<MethodReference> virtualMethods = new LinkedHashSet<>();

        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (MethodHolder method : cls.getMethods()) {
                Program program = method.getProgram();
                if (program == null) {
                    continue;
                }
                for (int i = 0; i < program.basicBlockCount(); ++i) {
                    BasicBlock block = program.basicBlockAt(i);
                    for (Instruction insn : block) {
                        if (insn instanceof InvokeInstruction) {
                            InvokeInstruction invoke = (InvokeInstruction) insn;
                            if (invoke.getType() == InvocationType.VIRTUAL) {
                                virtualMethods.add(invoke.getMethod());
                            }
                        } else if (insn instanceof CloneArrayInstruction) {
                            virtualMethods.add(new MethodReference(Object.class, "clone", Object.class));
                        }
                    }
                }
            }
        }

        return new VirtualTableProvider(classes, virtualMethods);
    }

    private void generateSpecialFunctions(GenerationContext context, CodeWriter writer) {
        generateThrowCCE(context, writer);
        generateAllocateStringArray(context, writer);
    }

    private void generateThrowCCE(GenerationContext context, CodeWriter writer) {
        writer.println("static void* throwClassCastException() {").indent();
        String methodName = context.getNames().forMethod(new MethodReference(ExceptionHandling.class,
                "throwClassCastException", void.class));
        writer.println(methodName + "();");
        writer.println("return NULL;");
        writer.outdent().println("}");
    }

    private void generateAllocateStringArray(GenerationContext context, CodeWriter writer) {
        writer.println("static JavaArray* teavm_allocateStringArray(int32_t size) {").indent();
        String allocateArrayName = context.getNames().forMethod(new MethodReference(Allocator.class,
                        "allocateArray", RuntimeClass.class, int.class, Address.class));
        String stringClassName = context.getNames().forClassInstance(ValueType.arrayOf(
                ValueType.object(String.class.getName())));
        writer.println("return (JavaArray*) " + allocateArrayName + "(&" + stringClassName + ", size);");
        writer.outdent().println("}");
    }

    private void generateArrayOfClassReferences(GenerationContext context, CodeWriter writer,
            List<? extends ValueType> types) {
        writer.print("static JavaClass* teavm_classReferences[" + types.size() + "] = {").indent();
        boolean first = true;
        for (ValueType type : types) {
            if (!first) {
                writer.print(", ");
            }
            writer.println();
            first = false;
            String typeName = context.getNames().forClassInstance(type);
            writer.print("(JavaClass*) &" + typeName);
        }
        if (!first) {
            writer.println();
        }
        writer.outdent().println("};");
    }

    private void generateMain(GenerationContext context, CodeWriter writer, ListableClassHolderSource classes,
            List<? extends ValueType> types) {
        writer.println("int main(int argc, char** argv) {").indent();

        writer.println("TeaVM_beforeInit();");
        writer.println("initHeap(" + minHeapSize + ");");
        generateVirtualTableHeaders(context, writer, types);
        generateStringPoolHeaders(context, writer);
        writer.println("initStaticFields();");
        generateStaticInitializerCalls(context, writer, classes);
        writer.println(context.getNames().forClassInitializer("java.lang.String") + "();");
        generateCallToMainMethod(context, writer);

        writer.outdent().println("}");
    }

    private void generateStaticInitializerCalls(GenerationContext context, CodeWriter writer,
            ListableClassReaderSource classes) {
        MethodDescriptor clinitDescriptor = new MethodDescriptor("<clinit>", ValueType.VOID);
        for (String className : classes.getClassNames()) {
            ClassReader cls = classes.get(className);
            if (!context.getCharacteristics().isStaticInit(cls.getName())
                    && !context.getCharacteristics().isStructure(cls.getName())) {
                continue;
            }


            if (cls.getMethod(clinitDescriptor) == null) {
                continue;
            }

            String clinitName = context.getNames().forMethod(new MethodReference(className, clinitDescriptor));
            writer.println(clinitName + "();");
        }
    }

    private void generateVirtualTableHeaders(GenerationContext context, CodeWriter writer,
            List<? extends ValueType> types) {
        writer.println("TeaVM_beforeClasses = (char*) teavm_classReferences[0];");
        writer.println("for (int i = 1; i < " + types.size() + "; ++i) {").indent();
        writer.println("char* c = (char*) teavm_classReferences[i];");
        writer.println("if (c < TeaVM_beforeClasses) TeaVM_beforeClasses = c;");
        writer.outdent().println("}");
        writer.println("TeaVM_beforeClasses -= 4096;");

        String classClassName = context.getNames().forClassInstance(ValueType.object("java.lang.Class"));
        writer.print("int32_t classHeader = PACK_CLASS(&" + classClassName + ") | ");
        CodeGeneratorUtil.writeValue(writer, context, RuntimeObject.GC_MARKED);
        writer.println(";");

        writer.println("for (int i = 0; i < " + types.size() + "; ++i) {").indent();
        writer.println("teavm_classReferences[i]->parent.header = classHeader;");
        writer.outdent().println("}");
    }

    private void generateStringPoolHeaders(GenerationContext context, CodeWriter writer) {
        String stringClassName = context.getNames().forClassInstance(ValueType.object("java.lang.String"));
        writer.print("int32_t stringHeader = PACK_CLASS(&" + stringClassName + ") | ");
        CodeGeneratorUtil.writeValue(writer, context, RuntimeObject.GC_MARKED);
        writer.println(";");

        int size = context.getStringPool().getStrings().size();
        writer.println("for (int i = 0; i < " + size + "; ++i) {").indent();
        writer.println("((JavaObject*) (stringPool + i))->header = stringHeader;");
        writer.outdent().println("}");
    }

    private void generateCallToMainMethod(GenerationContext context, CodeWriter writer) {
        TeaVMEntryPoint entryPoint = controller.getEntryPoints().get("main");
        if (entryPoint != null) {
            String mainMethod = context.getNames().forMethod(entryPoint.getMethod());
            writer.println(mainMethod + "(NULL);");
        }
    }

    @Override
    public String[] getPlatformTags() {
        return new String[] { PlatformMarkers.C, PlatformMarkers.LOW_LEVEL };
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }
}
