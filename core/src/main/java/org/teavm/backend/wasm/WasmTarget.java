/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.wasm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.backend.lowlevel.analyze.LowLevelInliningFilterFactory;
import org.teavm.backend.lowlevel.dependency.StringsDependencyListener;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.backend.lowlevel.generate.NameProviderWithSpecialNames;
import org.teavm.backend.lowlevel.transform.CoroutineTransformation;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.generate.WasmDependencyListener;
import org.teavm.backend.wasm.generate.WasmGenerationContext;
import org.teavm.backend.wasm.generate.WasmGenerator;
import org.teavm.backend.wasm.generate.WasmInteropFunctionGenerator;
import org.teavm.backend.wasm.generate.WasmNameProvider;
import org.teavm.backend.wasm.generate.WasmSpecialFunctionGenerator;
import org.teavm.backend.wasm.generate.WasmStringPool;
import org.teavm.backend.wasm.generators.ArrayGenerator;
import org.teavm.backend.wasm.generators.WasmMethodGenerator;
import org.teavm.backend.wasm.generators.WasmMethodGeneratorContext;
import org.teavm.backend.wasm.intrinsics.AddressIntrinsic;
import org.teavm.backend.wasm.intrinsics.AllocatorIntrinsic;
import org.teavm.backend.wasm.intrinsics.ClassIntrinsic;
import org.teavm.backend.wasm.intrinsics.ConsoleIntrinsic;
import org.teavm.backend.wasm.intrinsics.DoubleIntrinsic;
import org.teavm.backend.wasm.intrinsics.ExceptionHandlingIntrinsic;
import org.teavm.backend.wasm.intrinsics.FloatIntrinsic;
import org.teavm.backend.wasm.intrinsics.FunctionIntrinsic;
import org.teavm.backend.wasm.intrinsics.GCIntrinsic;
import org.teavm.backend.wasm.intrinsics.IntegerIntrinsic;
import org.teavm.backend.wasm.intrinsics.LongIntrinsic;
import org.teavm.backend.wasm.intrinsics.MemoryTraceIntrinsic;
import org.teavm.backend.wasm.intrinsics.MutatorIntrinsic;
import org.teavm.backend.wasm.intrinsics.ObjectIntrinsic;
import org.teavm.backend.wasm.intrinsics.PlatformClassIntrinsic;
import org.teavm.backend.wasm.intrinsics.PlatformClassMetadataIntrinsic;
import org.teavm.backend.wasm.intrinsics.PlatformIntrinsic;
import org.teavm.backend.wasm.intrinsics.PlatformObjectIntrinsic;
import org.teavm.backend.wasm.intrinsics.RuntimeClassIntrinsic;
import org.teavm.backend.wasm.intrinsics.ShadowStackIntrinsic;
import org.teavm.backend.wasm.intrinsics.StructureIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmHeapIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicFactory;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicFactoryContext;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.intrinsics.WasmRuntimeIntrinsic;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.backend.wasm.optimization.UnusedFunctionElimination;
import org.teavm.backend.wasm.render.WasmBinaryRenderer;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.backend.wasm.render.WasmBinaryWriter;
import org.teavm.backend.wasm.render.WasmCRenderer;
import org.teavm.backend.wasm.render.WasmRenderer;
import org.teavm.backend.wasm.transformation.IndirectCallTraceTransformation;
import org.teavm.backend.wasm.transformation.MemoryAccessTraceTransformation;
import org.teavm.common.ServiceRepository;
import org.teavm.dependency.ClassDependency;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.dependency.DependencyListener;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.Import;
import org.teavm.interop.Platforms;
import org.teavm.interop.StaticInit;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
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
import org.teavm.model.analysis.ClassMetadataRequirements;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTableBuilder;
import org.teavm.model.classes.VirtualTableProvider;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.Characteristics;
import org.teavm.model.lowlevel.CheckInstructionTransformation;
import org.teavm.model.lowlevel.ClassInitializerEliminator;
import org.teavm.model.lowlevel.ClassInitializerTransformer;
import org.teavm.model.lowlevel.LowLevelNullCheckFilter;
import org.teavm.model.lowlevel.ShadowStackTransformer;
import org.teavm.model.lowlevel.WriteBarrierInsertion;
import org.teavm.model.optimization.InliningFilterFactory;
import org.teavm.model.transformation.BoundCheckInsertion;
import org.teavm.model.transformation.ClassPatch;
import org.teavm.model.transformation.NullCheckInsertion;
import org.teavm.model.util.AsyncMethodFinder;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.EventQueue;
import org.teavm.runtime.ExceptionHandling;
import org.teavm.runtime.Fiber;
import org.teavm.runtime.GC;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;
import org.teavm.runtime.ShadowStack;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVMEntryPoint;
import org.teavm.vm.TeaVMTarget;
import org.teavm.vm.TeaVMTargetController;
import org.teavm.vm.spi.TeaVMHostExtension;

public class WasmTarget implements TeaVMTarget, TeaVMWasmHost {
    private static final MethodReference INIT_HEAP_REF = new MethodReference(WasmHeap.class, "initHeap",
            Address.class, int.class, int.class, int.class, void.class);
    private static final MethodReference RESIZE_HEAP_REF = new MethodReference(WasmHeap.class, "resizeHeap",
            int.class, void.class);
    private static final Set<MethodReference> VIRTUAL_METHODS = new HashSet<>(Arrays.asList(
            new MethodReference(Object.class, "clone", Object.class)));

    private TeaVMTargetController controller;
    private Characteristics characteristics;
    private boolean debugging;
    private boolean wastEmitted;
    private boolean cEmitted;
    private boolean cLineNumbersEmitted = Boolean.parseBoolean(System.getProperty("wasm.c.lineNumbers", "false"));
    private ClassInitializerEliminator classInitializerEliminator;
    private ClassInitializerTransformer classInitializerTransformer;
    private ShadowStackTransformer shadowStackTransformer;
    private WriteBarrierInsertion writeBarrierInsertion;
    private WasmBinaryVersion version = WasmBinaryVersion.V_0x1;
    private List<WasmIntrinsicFactory> additionalIntrinsics = new ArrayList<>();
    private NullCheckInsertion nullCheckInsertion;
    private BoundCheckInsertion boundCheckInsertion = new BoundCheckInsertion();
    private CheckInstructionTransformation checkTransformation = new CheckInstructionTransformation();
    private int minHeapSize = 2 * 1024 * 1024;
    private int maxHeapSize = 128 * 1024 * 1024;
    private boolean obfuscated;
    private Set<MethodReference> asyncMethods;
    private boolean hasThreads;

    @Override
    public void setController(TeaVMTargetController controller) {
        this.controller = controller;
        characteristics = new Characteristics(controller.getUnprocessedClassSource());
        classInitializerEliminator = new ClassInitializerEliminator(controller.getUnprocessedClassSource());
        classInitializerTransformer = new ClassInitializerTransformer();
        shadowStackTransformer = new ShadowStackTransformer(characteristics, true);
        nullCheckInsertion = new NullCheckInsertion(new LowLevelNullCheckFilter(characteristics));
        writeBarrierInsertion = new WriteBarrierInsertion(characteristics);

        controller.addVirtualMethods(VIRTUAL_METHODS::contains);
    }

    @Override
    public void add(WasmIntrinsicFactory intrinsic) {
        additionalIntrinsics.add(intrinsic);
    }

    @Override
    public List<TeaVMHostExtension> getHostExtensions() {
        return Collections.singletonList(this);
    }

    @Override
    public boolean requiresRegisterAllocation() {
        return true;
    }

    @Override
    public List<ClassHolderTransformer> getTransformers() {
        List<ClassHolderTransformer> transformers = new ArrayList<>();
        transformers.add(new ClassPatch());
        transformers.add(new WasmDependencyListener());
        return transformers;
    }

    @Override
    public List<DependencyListener> getDependencyListeners() {
        List<DependencyListener> listeners = new ArrayList<>();
        listeners.add(new WasmDependencyListener());
        return listeners;
    }

    public boolean isDebugging() {
        return debugging;
    }

    public void setDebugging(boolean debugging) {
        this.debugging = debugging;
    }

    public boolean isWastEmitted() {
        return wastEmitted;
    }

    public void setWastEmitted(boolean wastEmitted) {
        this.wastEmitted = wastEmitted;
    }

    public boolean isCEmitted() {
        return cEmitted;
    }

    public void setCEmitted(boolean cEmitted) {
        this.cEmitted = cEmitted;
    }

    public void setCLineNumbersEmitted(boolean cLineNumbersEmitted) {
        this.cLineNumbersEmitted = cLineNumbersEmitted;
    }

    public WasmBinaryVersion getVersion() {
        return version;
    }

    public void setVersion(WasmBinaryVersion version) {
        this.version = version;
    }

    public void setMinHeapSize(int minHeapSize) {
        this.minHeapSize = minHeapSize;
    }

    public void setMaxHeapSize(int maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
    }

    public void setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    @Override
    public void contributeDependencies(DependencyAnalyzer dependencyAnalyzer) {
        for (Class<?> type : Arrays.asList(int.class, long.class, float.class, double.class)) {
            MethodReference method = new MethodReference(WasmRuntime.class, "compare", type, type, int.class);
            dependencyAnalyzer.linkMethod(method).use();
        }
        for (Class<?> type : Arrays.asList(float.class, double.class)) {
            MethodReference method = new MethodReference(WasmRuntime.class, "remainder", type, type, type);
            dependencyAnalyzer.linkMethod(method).use();
        }

        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "align", Address.class, int.class,
                Address.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "fill", Address.class, int.class,
                int.class, void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "fillZero", Address.class, int.class,
                void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "moveMemoryBlock", Address.class,
                Address.class, int.class, void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "allocStack",
                int.class, Address.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "getStackTop", Address.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "getNextStackFrame", Address.class,
                Address.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "getStackRootCount", Address.class,
                int.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "getStackRootPointer", Address.class,
                Address.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "setExceptionHandlerId", Address.class,
                int.class, void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "getCallSiteId", Address.class,
                int.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "resourceMapKeys", Address.class,
                String[].class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "lookupResource", Address.class,
                String.class, Address.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "printString",
                String.class, void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "printInt",
                int.class, void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(WasmRuntime.class, "printOutOfMemory",
                void.class)).use();

        dependencyAnalyzer.linkMethod(INIT_HEAP_REF).use();
        dependencyAnalyzer.linkMethod(RESIZE_HEAP_REF).use();

        dependencyAnalyzer.linkMethod(new MethodReference(Allocator.class, "allocate",
                RuntimeClass.class, Address.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Allocator.class, "allocateArray",
                RuntimeClass.class, int.class, Address.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Allocator.class, "allocateMultiArray",
                RuntimeClass.class, Address.class, int.class, RuntimeArray.class)).use();

        dependencyAnalyzer.linkMethod(new MethodReference(Allocator.class, "<clinit>", void.class)).use();

        dependencyAnalyzer.linkMethod(new MethodReference(ExceptionHandling.class, "throwException",
                Throwable.class, void.class)).use();

        dependencyAnalyzer.linkMethod(new MethodReference(ExceptionHandling.class, "catchException",
                Throwable.class)).use();

        dependencyAnalyzer.linkField(new FieldReference("java.lang.Object", "monitor"));

        dependencyAnalyzer.linkMethod(new MethodReference(String.class, "allocate", int.class, String.class))
                .use();

        ClassDependency runtimeClassDep = dependencyAnalyzer.linkClass(RuntimeClass.class.getName());
        ClassDependency runtimeObjectDep = dependencyAnalyzer.linkClass(RuntimeObject.class.getName());
        ClassDependency runtimeArrayDep = dependencyAnalyzer.linkClass(RuntimeArray.class.getName());
        for (ClassDependency classDep : Arrays.asList(runtimeClassDep, runtimeObjectDep, runtimeArrayDep)) {
            for (FieldReader field : classDep.getClassReader().getFields()) {
                dependencyAnalyzer.linkField(field.getReference());
            }
        }

        dependencyAnalyzer.linkMethod(new MethodReference(Fiber.class, "isResuming", boolean.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Fiber.class, "isSuspending", boolean.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Fiber.class, "current", Fiber.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Fiber.class, "startMain", String[].class, void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(EventQueue.class, "processSingle", void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(EventQueue.class, "isStopped", boolean.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Thread.class, "setCurrentThread", Thread.class,
                void.class)).use();

        ClassReader fiberClass = dependencyAnalyzer.getClassSource().get(Fiber.class.getName());
        for (MethodReader method : fiberClass.getMethods()) {
            if (method.getName().startsWith("pop") || method.getName().equals("push")) {
                dependencyAnalyzer.linkMethod(method.getReference()).use();
            }
        }

        dependencyAnalyzer.addDependencyListener(new StringsDependencyListener());
    }

    @Override
    public void analyzeBeforeOptimizations(ListableClassReaderSource classSource) {
        AsyncMethodFinder asyncFinder = new AsyncMethodFinder(controller.getDependencyInfo().getCallGraph(),
                controller.getDependencyInfo());
        asyncFinder.find(classSource);
        asyncMethods = new HashSet<>(asyncFinder.getAsyncMethods());
        asyncMethods.addAll(asyncFinder.getAsyncFamilyMethods());
        hasThreads = asyncFinder.hasAsyncMethods();
    }

    @Override
    public void beforeOptimizations(Program program, MethodReader method) {
        nullCheckInsertion.transformProgram(program, method.getReference());
        boundCheckInsertion.transformProgram(program, method.getReference());
    }

    @Override
    public void afterOptimizations(Program program, MethodReader method) {
        classInitializerEliminator.apply(program);
        classInitializerTransformer.transform(program);
        new CoroutineTransformation(controller.getUnprocessedClassSource(), asyncMethods, hasThreads)
                .apply(program, method.getReference());
        checkTransformation.apply(program, method.getResultType());
        shadowStackTransformer.apply(program, method);
        writeBarrierInsertion.apply(program);
    }

    @Override
    public void emit(ListableClassHolderSource classes, BuildTarget buildTarget, String outputName)
            throws IOException {
        WasmModule module = new WasmModule();
        WasmFunction initFunction = new WasmFunction("__start__");

        VirtualTableProvider vtableProvider = createVirtualTableProvider(classes);
        ClassHierarchy hierarchy = new ClassHierarchy(classes);
        TagRegistry tagRegistry = new TagRegistry(classes, hierarchy);
        BinaryWriter binaryWriter = new BinaryWriter(256);
        NameProvider names = new NameProviderWithSpecialNames(new WasmNameProvider(),
                controller.getUnprocessedClassSource());
        ClassMetadataRequirements metadataRequirements = new ClassMetadataRequirements(controller.getDependencyInfo());
        WasmClassGenerator classGenerator = new WasmClassGenerator(classes, controller.getUnprocessedClassSource(),
                vtableProvider, tagRegistry, binaryWriter, names, metadataRequirements,
                controller.getClassInitializerInfo());

        Decompiler decompiler = new Decompiler(classes, new HashSet<>(), false);
        WasmStringPool stringPool = classGenerator.getStringPool();
        WasmGenerationContext context = new WasmGenerationContext(classes, module, controller.getDiagnostics(),
                vtableProvider, tagRegistry, stringPool, names);

        context.addIntrinsic(new AddressIntrinsic(classGenerator));
        context.addIntrinsic(new StructureIntrinsic(classes, classGenerator));
        context.addIntrinsic(new FunctionIntrinsic(classGenerator));
        WasmRuntimeIntrinsic wasmRuntimeIntrinsic = new WasmRuntimeIntrinsic();
        context.addIntrinsic(wasmRuntimeIntrinsic);
        context.addIntrinsic(new AllocatorIntrinsic(classGenerator));
        context.addIntrinsic(new PlatformIntrinsic());
        context.addIntrinsic(new PlatformClassIntrinsic());
        context.addIntrinsic(new PlatformObjectIntrinsic(classGenerator));
        context.addIntrinsic(new PlatformClassMetadataIntrinsic());
        context.addIntrinsic(new ClassIntrinsic());
        context.addIntrinsic(new RuntimeClassIntrinsic());
        context.addIntrinsic(new FloatIntrinsic());
        context.addIntrinsic(new DoubleIntrinsic());
        context.addIntrinsic(new LongIntrinsic());
        context.addIntrinsic(new IntegerIntrinsic());
        context.addIntrinsic(new ObjectIntrinsic());
        context.addIntrinsic(new ConsoleIntrinsic());
        context.addGenerator(new ArrayGenerator());
        if (!Boolean.parseBoolean(System.getProperty("teavm.wasm.vmAssertions", "false"))) {
            context.addIntrinsic(new MemoryTraceIntrinsic());
        }
        context.addIntrinsic(new WasmHeapIntrinsic());
        context.addIntrinsic(new FiberIntrinsic());

        IntrinsicFactoryContext intrinsicFactoryContext = new IntrinsicFactoryContext();
        for (WasmIntrinsicFactory additionalIntrinsicFactory : additionalIntrinsics) {
            context.addIntrinsic(additionalIntrinsicFactory.create(intrinsicFactoryContext));
        }

        GCIntrinsic gcIntrinsic = new GCIntrinsic();
        context.addIntrinsic(gcIntrinsic);
        MutatorIntrinsic mutatorIntrinsic = new MutatorIntrinsic();
        context.addIntrinsic(mutatorIntrinsic);
        context.addIntrinsic(new ShadowStackIntrinsic());
        ExceptionHandlingIntrinsic exceptionHandlingIntrinsic = new ExceptionHandlingIntrinsic(binaryWriter,
                classGenerator, stringPool, obfuscated);
        context.addIntrinsic(exceptionHandlingIntrinsic);

        WasmGenerator generator = new WasmGenerator(decompiler, classes, context, classGenerator, binaryWriter,
                asyncMethods::contains);

        generateMethods(classes, context, generator, classGenerator, binaryWriter, module);
        new WasmInteropFunctionGenerator(classGenerator).generateFunctions(module);
        exceptionHandlingIntrinsic.postProcess(CallSiteDescriptor.extract(classes, classes.getClassNames()));
        generateIsSupertypeFunctions(tagRegistry, module, classGenerator);
        classGenerator.postProcess();
        new WasmSpecialFunctionGenerator(classGenerator, gcIntrinsic.regionSizeExpressions)
                .generateSpecialFunctions(module);
        mutatorIntrinsic.setStaticGcRootsAddress(classGenerator.getStaticGcRootsAddress());
        mutatorIntrinsic.setClassesAddress(classGenerator.getClassesAddress());
        mutatorIntrinsic.setClassCount(classGenerator.getClassCount());

        WasmMemorySegment dataSegment = new WasmMemorySegment();
        dataSegment.setData(binaryWriter.getData());
        dataSegment.setOffset(256);
        module.getSegments().add(dataSegment);

        renderMemoryLayout(module, binaryWriter.getAddress(), gcIntrinsic);
        renderClinit(classes, classGenerator, module);
        if (controller.wasCancelled()) {
            return;
        }

        generateInitFunction(classes, initFunction, names, binaryWriter.getAddress());
        module.add(initFunction);
        module.setStartFunction(initFunction);
        module.add(createStartFunction(names));

        for (String functionName : classGenerator.getFunctionTable()) {
            WasmFunction function = module.getFunctions().get(functionName);
            assert function != null : "Function referenced from function table not found: " + functionName;
            module.getFunctionTable().add(function);
        }

        new UnusedFunctionElimination(module).apply();

        if (Boolean.parseBoolean(System.getProperty("wasm.memoryTrace", "false"))) {
            new MemoryAccessTraceTransformation(module).apply();
        }
        if (Boolean.parseBoolean(System.getProperty("wasm.indirectCallTrace", "false"))) {
            new IndirectCallTraceTransformation(module).apply();
        }

        WasmBinaryWriter writer = new WasmBinaryWriter();
        WasmBinaryRenderer renderer = new WasmBinaryRenderer(writer, version, obfuscated);
        renderer.render(module);

        try (OutputStream output = buildTarget.createResource(outputName)) {
            output.write(writer.getData());
            output.flush();
        }

        if (wastEmitted) {
            emitWast(module, buildTarget, getBaseName(outputName) + ".wast");
        }
        if (cEmitted) {
            emitC(module, buildTarget, getBaseName(outputName) + ".wasm.c");
        }

        emitRuntime(buildTarget, getBaseName(outputName) + ".wasm-runtime.js");
    }

    private WasmFunction createStartFunction(NameProvider names) {
        WasmFunction function = new WasmFunction("teavm_start");
        function.setExportName("start");
        function.getParameters().add(WasmType.INT32);

        WasmLocal local = new WasmLocal(WasmType.INT32, "args");
        function.add(local);

        WasmCall call = new WasmCall(names.forMethod(new MethodReference(Fiber.class, "startMain", String[].class,
                void.class)));
        call.getArguments().add(new WasmGetLocal(local));
        function.getBody().add(call);

        return function;
    }

    private class IntrinsicFactoryContext implements WasmIntrinsicFactoryContext {
        @Override
        public ClassReaderSource getClassSource() {
            return controller.getUnprocessedClassSource();
        }

        @Override
        public ClassLoader getClassLoader() {
            return controller.getClassLoader();
        }

        @Override
        public ServiceRepository getServices() {
            return controller.getServices();
        }

        @Override
        public Properties getProperties() {
            return controller.getProperties();
        }
    }

    private void generateInitFunction(ListableClassReaderSource classes, WasmFunction initFunction,
            NameProvider names, int heapAddress) {

        for (Class<?> javaCls : new Class<?>[] { WasmRuntime.class, WasmHeap.class }) {
            ClassReader cls = classes.get(javaCls.getName());
            MethodReader clinit = cls.getMethod(new MethodDescriptor("<clinit>", void.class));
            if (clinit == null) {
                continue;
            }
            initFunction.getBody().add(new WasmCall(names.forClassInitializer(cls.getName())));
        }

        initFunction.getBody().add(new WasmCall(names.forMethod(INIT_HEAP_REF),
                new WasmInt32Constant(heapAddress), new WasmInt32Constant(minHeapSize),
                new WasmInt32Constant(maxHeapSize), new WasmInt32Constant(WasmHeap.DEFAULT_STACK_SIZE)));

        for (Class<?> javaCls : new Class<?>[] { GC.class }) {
            ClassReader cls = classes.get(javaCls.getName());
            MethodReader clinit = cls.getMethod(new MethodDescriptor("<clinit>", void.class));
            if (clinit == null) {
                continue;
            }
            initFunction.getBody().add(new WasmCall(names.forClassInitializer(cls.getName())));
        }

        for (String className : classes.getClassNames()) {
            if (className.equals(WasmRuntime.class.getName()) || className.equals(WasmHeap.class.getName())
                    || className.equals(GC.class.getName())) {
                continue;
            }
            ClassReader cls = classes.get(className);
            if (cls.getAnnotations().get(StaticInit.class.getName()) == null) {
                continue;
            }
            MethodReader clinit = cls.getMethod(new MethodDescriptor("<clinit>", void.class));
            if (clinit == null) {
                continue;
            }
            initFunction.getBody().add(new WasmCall(names.forClassInitializer(className)));
        }

    }

    private String getBaseName(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? name : name.substring(0, index);
    }

    private void emitWast(WasmModule module, BuildTarget buildTarget, String outputName) throws IOException {
        WasmRenderer renderer = new WasmRenderer();
        renderer.setLineNumbersEmitted(debugging);
        renderer.render(module);
        try (OutputStream output = buildTarget.createResource(outputName);
                Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            writer.write(renderer.toString());
        }
    }

    private void emitC(WasmModule module, BuildTarget buildTarget, String outputName) throws IOException {
        WasmCRenderer renderer = new WasmCRenderer();
        renderer.setLineNumbersEmitted(cLineNumbersEmitted);
        renderer.setMemoryAccessChecked(Boolean.parseBoolean(System.getProperty("wasm.c.assertMemory", "false")));
        renderer.render(module);
        try (OutputStream output = buildTarget.createResource(outputName);
                Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            writer.write(renderer.toString());
        }
    }

    private void emitRuntime(BuildTarget buildTarget, String outputName) throws IOException {
        ClassLoader loader = controller.getClassLoader();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                loader.getResourceAsStream("org/teavm/backend/wasm/wasm-runtime.js"), StandardCharsets.UTF_8));
                Writer writer = new OutputStreamWriter(buildTarget.createResource(outputName),
                        StandardCharsets.UTF_8)) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                writer.append(line).append('\n');
            }
        }
    }

    private void generateMethods(ListableClassHolderSource classes, WasmGenerationContext context,
            WasmGenerator generator, WasmClassGenerator classGenerator, BinaryWriter binaryWriter, WasmModule module) {
        List<MethodHolder> methods = new ArrayList<>();
        for (String className : classes.getClassNames()) {
            ClassHolder cls = classes.get(className);
            for (MethodHolder method : cls.getMethods()) {
                if (method.hasModifier(ElementModifier.ABSTRACT)
                        || context.getIntrinsic(method.getReference()) != null) {
                    continue;
                }
                module.add(generator.generateDefinition(method.getReference()));
                methods.add(method);
            }
        }

        MethodGeneratorContextImpl methodGeneratorContext = new MethodGeneratorContextImpl(binaryWriter,
                context.getStringPool(), context.getDiagnostics(), context.names, classGenerator, classes);

        for (MethodHolder method : methods) {
            ClassHolder cls = classes.get(method.getOwnerName());

            MethodHolder implementor = method;
            AnnotationHolder delegateAnnot = method.getAnnotations().get(DelegateTo.class.getName());
            if (delegateAnnot != null) {
                String methodName = delegateAnnot.getValue("value").getString();
                boolean found = false;
                for (MethodHolder candidate : cls.getMethods()) {
                    if (candidate.getName().equals(methodName)) {
                        if (found) {
                            controller.getDiagnostics().error(new CallLocation(method.getReference()),
                                    "Method is delegated to " + methodName + " but several implementations "
                                            + "found");
                            break;
                        }
                        implementor = candidate;
                        found = true;
                    }
                }
            }

            if (implementor.hasModifier(ElementModifier.NATIVE)) {
                WasmMethodGenerator methodGenerator = context.getGenerator(method.getReference());
                if (methodGenerator != null) {
                    WasmFunction function = context.getFunction(context.names.forMethod(method.getReference()));
                    methodGenerator.apply(method.getReference(), function, methodGeneratorContext);
                } else if (!isShadowStackMethod(method.getReference())) {
                    if (context.getImportedMethod(method.getReference()) == null) {
                        CallLocation location = new CallLocation(method.getReference());
                        controller.getDiagnostics().error(location, "Method {{m0}} is native but "
                                + "has no {{c1}} annotation on it", method.getReference(), Import.class.getName());
                    }
                    generator.generateNative(method.getReference());
                }
                continue;
            }
            if (implementor.getProgram() == null || implementor.getProgram().basicBlockCount() == 0) {
                continue;
            }
            if (method == implementor) {
                generator.generate(method.getReference(), implementor);
            } else {
                generateStub(context.names, module, method, implementor);
            }
            if (controller.wasCancelled()) {
                return;
            }
        }
    }

    private boolean isShadowStackMethod(MethodReference method) {
        if (!method.getClassName().equals(ShadowStack.class.getName())) {
            return false;
        }
        switch (method.getName()) {
            case "allocStack":
            case "registerGCRoot":
            case "removeGCRoot":
            case "releaseStack":
                return true;
            default:
                return false;
        }
    }

    private void generateIsSupertypeFunctions(TagRegistry tagRegistry, WasmModule module,
            WasmClassGenerator classGenerator) {
        for (ValueType type : classGenerator.getRegisteredClasses()) {
            WasmFunction function = new WasmFunction(classGenerator.names.forSupertypeFunction(type));
            function.getParameters().add(WasmType.INT32);
            function.setResult(WasmType.INT32);
            module.add(function);

            WasmLocal subtypeVar = new WasmLocal(WasmType.INT32, "subtype");
            function.add(subtypeVar);

            if (type instanceof ValueType.Object) {
                String className = ((ValueType.Object) type).getClassName();
                generateIsClass(subtypeVar, classGenerator, tagRegistry, className, function.getBody());
            } else if (type instanceof ValueType.Array) {
                ValueType itemType = ((ValueType.Array) type).getItemType();
                generateIsArray(subtypeVar, classGenerator, itemType, function.getBody());
            } else {
                int expected = classGenerator.getClassPointer(type);
                WasmExpression condition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ,
                        new WasmGetLocal(subtypeVar), new WasmInt32Constant(expected));
                function.getBody().add(new WasmReturn(condition));
            }
        }
    }

    private void generateIsClass(WasmLocal subtypeVar, WasmClassGenerator classGenerator, TagRegistry tagRegistry,
            String className, List<WasmExpression> body) {
        List<TagRegistry.Range> ranges = tagRegistry.getRanges(className);
        if (ranges.isEmpty()) {
            body.add(new WasmReturn(new WasmInt32Constant(0)));
            return;
        }

        int tagOffset = classGenerator.getFieldOffset(new FieldReference(RuntimeClass.class.getName(), "tag"));

        WasmExpression tagExpression = new WasmGetLocal(subtypeVar);
        tagExpression = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, tagExpression,
                new WasmInt32Constant(tagOffset));
        tagExpression = new WasmLoadInt32(4, tagExpression, WasmInt32Subtype.INT32);
        body.add(new WasmSetLocal(subtypeVar, tagExpression));

        ranges.sort(Comparator.comparingInt(range -> range.lower));

        int lower = ranges.get(0).lower;
        int upper = ranges.get(ranges.size() - 1).upper;

        WasmExpression lowerCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                new WasmGetLocal(subtypeVar), new WasmInt32Constant(lower));
        WasmConditional testLower = new WasmConditional(lowerCondition);
        testLower.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(0)));
        body.add(testLower);

        WasmExpression upperCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GE_SIGNED,
                new WasmGetLocal(subtypeVar), new WasmInt32Constant(upper));
        WasmConditional testUpper = new WasmConditional(upperCondition);
        testUpper.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(0)));
        body.add(testUpper);

        for (int i = 1; i < ranges.size(); ++i) {
            int lowerHole = ranges.get(i - 1).upper;
            int upperHole = ranges.get(i).lower;

            lowerCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED,
                    new WasmGetLocal(subtypeVar), new WasmInt32Constant(lowerHole));
            testLower = new WasmConditional(lowerCondition);
            body.add(testLower);

            upperCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                    new WasmGetLocal(subtypeVar), new WasmInt32Constant(upperHole));
            testUpper = new WasmConditional(upperCondition);
            testUpper.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(0)));

            testLower.getThenBlock().getBody().add(testUpper);
        }

        body.add(new WasmReturn(new WasmInt32Constant(1)));
    }

    private void generateIsArray(WasmLocal subtypeVar, WasmClassGenerator classGenerator, ValueType itemType,
            List<WasmExpression> body) {
        int itemOffset = classGenerator.getFieldOffset(new FieldReference(RuntimeClass.class.getName(), "itemType"));

        WasmExpression itemExpression = new WasmGetLocal(subtypeVar);
        itemExpression = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, itemExpression,
                new WasmInt32Constant(itemOffset));
        itemExpression = new WasmLoadInt32(4, itemExpression, WasmInt32Subtype.INT32);
        body.add(new WasmSetLocal(subtypeVar, itemExpression));

        WasmExpression itemCondition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ,
                new WasmGetLocal(subtypeVar), new WasmInt32Constant(0));
        WasmConditional itemTest = new WasmConditional(itemCondition);
        itemTest.setType(WasmType.INT32);
        itemTest.getThenBlock().getBody().add(new WasmInt32Constant(0));

        WasmCall delegateToItem = new WasmCall(classGenerator.names.forSupertypeFunction(itemType));
        delegateToItem.getArguments().add(new WasmGetLocal(subtypeVar));
        itemTest.getElseBlock().getBody().add(delegateToItem);

        body.add(new WasmReturn(itemTest));
    }

    private void generateStub(NameProvider names, WasmModule module, MethodHolder method, MethodHolder implementor) {
        WasmFunction function = module.getFunctions().get(names.forMethod(method.getReference()));

        WasmCall call = new WasmCall(names.forMethod(implementor.getReference()));
        for (WasmType param : function.getParameters()) {
            WasmLocal local = new WasmLocal(param);
            function.add(local);
            call.getArguments().add(new WasmGetLocal(local));
        }

        if (method.getResultType() == ValueType.VOID) {
            function.getBody().add(call);
        } else {
            function.getBody().add(new WasmReturn(call));
        }
    }

    private void renderClinit(ListableClassReaderSource classes, WasmClassGenerator classGenerator,
            WasmModule module) {
        for (ValueType type : classGenerator.getRegisteredClasses()) {
            if (!(type instanceof ValueType.Object)) {
                continue;
            }
            String className = ((ValueType.Object) type).getClassName();
            if (classGenerator.isStructure(className)) {
                continue;
            }

            ClassReader cls = classes.get(className);
            if (cls == null) {
                continue;
            }
            MethodReader method = cls.getMethod(new MethodDescriptor("<clinit>", void.class));
            if (method == null) {
                continue;
            }

            WasmFunction initFunction = new WasmFunction(classGenerator.names.forClassInitializer(className));
            module.add(initFunction);

            WasmBlock block = new WasmBlock(false);

            int index = classGenerator.getClassPointer(ValueType.object(className))
                    + classGenerator.getFieldOffset(new FieldReference(RuntimeClass.class.getName(), "flags"));
            WasmExpression initFlag = new WasmLoadInt32(4, new WasmInt32Constant(index), WasmInt32Subtype.INT32);
            initFlag = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.AND, initFlag,
                    new WasmInt32Constant(RuntimeClass.INITIALIZED));
            block.getBody().add(new WasmBranch(initFlag, block));
            initFunction.getBody().add(block);

            initFlag = new WasmLoadInt32(4, new WasmInt32Constant(index), WasmInt32Subtype.INT32);
            initFlag = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.OR, initFlag,
                    new WasmInt32Constant(RuntimeClass.INITIALIZED));
            block.getBody().add(new WasmStoreInt32(4, new WasmInt32Constant(index), initFlag,
                    WasmInt32Subtype.INT32));

            block.getBody().add(new WasmCall(classGenerator.names.forMethod(method.getReference())));

            if (controller.wasCancelled()) {
                break;
            }
        }
    }

    private void renderMemoryLayout(WasmModule module, int address, GCIntrinsic gcIntrinsic) {
        module.setMinMemorySize(WasmRuntime.align(address, WasmHeap.PAGE_SIZE) / WasmHeap.PAGE_SIZE);

        int newStorageSize = WasmHeap.calculateStorageSize(maxHeapSize);
        int newRegionsCount = WasmHeap.calculateRegionsCount(maxHeapSize, WasmHeap.DEFAULT_REGION_SIZE);
        int newRegionsSize = WasmHeap.calculateRegionsSize(newRegionsCount);

        address = WasmRuntime.align(address + WasmHeap.DEFAULT_STACK_SIZE, 16);
        address = WasmRuntime.align(address + maxHeapSize, 16);
        address = WasmRuntime.align(address + newRegionsSize, 16);
        address = WasmRuntime.align(address + newRegionsCount, 16);
        address = WasmRuntime.align(address + newStorageSize, 16);
        gcIntrinsic.setRegionSize(WasmHeap.DEFAULT_REGION_SIZE);

        module.setMaxMemorySize(WasmRuntime.align(address, WasmHeap.PAGE_SIZE) / WasmHeap.PAGE_SIZE);
    }

    private VirtualTableProvider createVirtualTableProvider(ListableClassHolderSource classes) {
        VirtualTableBuilder builder = new VirtualTableBuilder(classes);
        builder.setMethodsUsedAtCallSites(getMethodsUsedOnCallSites(classes));
        builder.setMethodCalledVirtually(controller::isVirtual);
        return builder.build();
    }

    private Set<MethodReference> getMethodsUsedOnCallSites(ListableClassHolderSource classes) {
        Set<MethodReference> virtualMethods = new HashSet<>();

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

        return virtualMethods;
    }

    @Override
    public String[] getPlatformTags() {
        return new String[] { Platforms.WEBASSEMBLY, Platforms.LOW_LEVEL };
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public InliningFilterFactory getInliningFilter() {
        return new LowLevelInliningFilterFactory(characteristics);
    }

    static class MethodGeneratorContextImpl implements WasmMethodGeneratorContext {
        private BinaryWriter binaryWriter;
        private WasmStringPool stringPool;
        private Diagnostics diagnostics;
        private NameProvider names;
        private WasmClassGenerator classGenerator;
        private ClassReaderSource classSource;

        MethodGeneratorContextImpl(BinaryWriter binaryWriter, WasmStringPool stringPool,
                Diagnostics diagnostics, NameProvider names, WasmClassGenerator classGenerator,
                ClassReaderSource classSource) {
            this.binaryWriter = binaryWriter;
            this.stringPool = stringPool;
            this.diagnostics = diagnostics;
            this.names = names;
            this.classGenerator = classGenerator;
            this.classSource = classSource;
        }

        @Override
        public BinaryWriter getBinaryWriter() {
            return binaryWriter;
        }

        @Override
        public WasmStringPool getStringPool() {
            return stringPool;
        }

        @Override
        public Diagnostics getDiagnostics() {
            return diagnostics;
        }

        @Override
        public NameProvider getNames() {
            return names;
        }

        @Override
        public WasmClassGenerator getClassGenerator() {
            return classGenerator;
        }

        @Override
        public ClassReaderSource getClassSource() {
            return classSource;
        }
    }


    class FiberIntrinsic implements WasmIntrinsic {
        @Override
        public boolean isApplicable(MethodReference methodReference) {
            if (!methodReference.getClassName().equals(Fiber.class.getName())) {
                return false;
            }
            switch (methodReference.getName()) {
                case "runMain":
                case "setCurrentThread":
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
            switch (invocation.getMethod().getName()) {
                case "runMain": {
                    Iterator<? extends TeaVMEntryPoint> entryPointIter = controller.getEntryPoints().values()
                            .iterator();
                    if (entryPointIter.hasNext()) {
                        TeaVMEntryPoint entryPoint = entryPointIter.next();
                        String name = manager.getNames().forMethod(entryPoint.getMethod());
                        WasmCall call = new WasmCall(name);
                        call.getArguments().add(manager.generate(invocation.getArguments().get(0)));
                        call.setLocation(invocation.getLocation());
                        return call;
                    } else {
                        WasmUnreachable unreachable = new WasmUnreachable();
                        unreachable.setLocation(invocation.getLocation());
                        return unreachable;
                    }
                }
                case "setCurrentThread": {
                    String name = manager.getNames().forMethod(new MethodReference(Thread.class,
                            "setCurrentThread", Thread.class, void.class));
                    WasmCall call = new WasmCall(name);
                    call.getArguments().add(manager.generate(invocation.getArguments().get(0)));
                    call.setLocation(invocation.getLocation());
                    return call;
                }
                default:
                    throw new IllegalArgumentException();
            }
        }
    }
}