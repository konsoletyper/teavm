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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.decompilation.Decompiler;
import org.teavm.backend.c.analyze.CDependencyListener;
import org.teavm.backend.c.analyze.InteropDependencyListener;
import org.teavm.backend.c.generate.BufferedCodeWriter;
import org.teavm.backend.c.generate.CallSiteGenerator;
import org.teavm.backend.c.generate.ClassGenerationContext;
import org.teavm.backend.c.generate.ClassGenerator;
import org.teavm.backend.c.generate.CodeGenerationVisitor;
import org.teavm.backend.c.generate.CodeWriter;
import org.teavm.backend.c.generate.GenerationContext;
import org.teavm.backend.c.generate.IncludeManager;
import org.teavm.backend.c.generate.OutputFileUtil;
import org.teavm.backend.c.generate.SimpleIncludeManager;
import org.teavm.backend.c.generate.SimpleStringPool;
import org.teavm.backend.c.generate.StringPoolGenerator;
import org.teavm.backend.c.generators.ArrayGenerator;
import org.teavm.backend.c.generators.Generator;
import org.teavm.backend.c.generators.GeneratorFactory;
import org.teavm.backend.c.generators.ReferenceQueueGenerator;
import org.teavm.backend.c.generators.WeakReferenceGenerator;
import org.teavm.backend.c.intrinsic.AddressIntrinsic;
import org.teavm.backend.c.intrinsic.AllocatorIntrinsic;
import org.teavm.backend.c.intrinsic.ConsoleIntrinsic;
import org.teavm.backend.c.intrinsic.ExceptionHandlingIntrinsic;
import org.teavm.backend.c.intrinsic.FunctionIntrinsic;
import org.teavm.backend.c.intrinsic.GCIntrinsic;
import org.teavm.backend.c.intrinsic.IntegerIntrinsic;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.backend.c.intrinsic.IntrinsicFactory;
import org.teavm.backend.c.intrinsic.LongIntrinsic;
import org.teavm.backend.c.intrinsic.MemoryTraceIntrinsic;
import org.teavm.backend.c.intrinsic.MutatorIntrinsic;
import org.teavm.backend.c.intrinsic.PlatformClassIntrinsic;
import org.teavm.backend.c.intrinsic.PlatformClassMetadataIntrinsic;
import org.teavm.backend.c.intrinsic.PlatformIntrinsic;
import org.teavm.backend.c.intrinsic.PlatformObjectIntrinsic;
import org.teavm.backend.c.intrinsic.RuntimeClassIntrinsic;
import org.teavm.backend.c.intrinsic.ShadowStackIntrinsic;
import org.teavm.backend.c.intrinsic.StringsIntrinsic;
import org.teavm.backend.c.intrinsic.StructureIntrinsic;
import org.teavm.backend.lowlevel.analyze.LowLevelInliningFilterFactory;
import org.teavm.backend.lowlevel.dependency.ExceptionHandlingDependencyListener;
import org.teavm.backend.lowlevel.dependency.StringsDependencyListener;
import org.teavm.backend.lowlevel.dependency.WeakReferenceDependencyListener;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.backend.lowlevel.generate.NameProviderWithSpecialNames;
import org.teavm.backend.lowlevel.transform.CoroutineTransformation;
import org.teavm.backend.lowlevel.transform.WeakReferenceTransformation;
import org.teavm.cache.EmptyMethodNodeCache;
import org.teavm.cache.MethodNodeCache;
import org.teavm.common.JsonUtil;
import org.teavm.dependency.ClassDependency;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.dependency.DependencyListener;
import org.teavm.interop.Address;
import org.teavm.interop.Platforms;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
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
import org.teavm.model.lowlevel.ExportDependencyListener;
import org.teavm.model.lowlevel.LowLevelNullCheckFilter;
import org.teavm.model.lowlevel.ShadowStackTransformer;
import org.teavm.model.lowlevel.WriteBarrierInsertion;
import org.teavm.model.optimization.InliningFilterFactory;
import org.teavm.model.transformation.BoundCheckInsertion;
import org.teavm.model.transformation.ClassPatch;
import org.teavm.model.transformation.NullCheckInsertion;
import org.teavm.model.util.AsyncMethodFinder;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.CallSite;
import org.teavm.runtime.CallSiteLocation;
import org.teavm.runtime.EventQueue;
import org.teavm.runtime.ExceptionHandling;
import org.teavm.runtime.Fiber;
import org.teavm.runtime.GC;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVMEntryPoint;
import org.teavm.vm.TeaVMTarget;
import org.teavm.vm.TeaVMTargetController;
import org.teavm.vm.spi.TeaVMHostExtension;

public class CTarget implements TeaVMTarget, TeaVMCHost {
    private static final Set<MethodReference> VIRTUAL_METHODS = new HashSet<>(Arrays.asList(
            new MethodReference(Object.class, "clone", Object.class)
    ));
    private static final String[] RUNTIME_FILES = { "core.c", "core.h", "date.c", "date.h", "definitions.h",
            "exceptions.h", "fiber.c", "fiber.h", "file.c", "file.h", "heapdump.c", "heapdump.h", "heaptrace.c",
            "heaptrace.h", "log.c", "log.h", "memory.c", "memory.h", "references.c", "references.h",
            "resource.c", "resource.h", "runtime.h", "stack.c", "stack.h", "string.c", "string.h",
            "stringhash.c", "stringhash.h", "time.c", "time.h", "virtcall.c", "virtcall.h"
    };

    private TeaVMTargetController controller;
    private NameProvider rawNameProvider;
    private ClassInitializerEliminator classInitializerEliminator;
    private ClassInitializerTransformer classInitializerTransformer;
    private ShadowStackTransformer shadowStackTransformer;
    private WriteBarrierInsertion writeBarrierInsertion;
    private NullCheckInsertion nullCheckInsertion;
    private BoundCheckInsertion boundCheckInsertion = new BoundCheckInsertion();
    private CheckInstructionTransformation checkTransformation;
    private ExportDependencyListener exportDependencyListener = new ExportDependencyListener();
    private int minHeapSize = 4 * 1024 * 1024;
    private int maxHeapSize = 128 * 1024 * 1024;
    private List<IntrinsicFactory> intrinsicFactories = new ArrayList<>();
    private List<GeneratorFactory> generatorFactories = new ArrayList<>();
    private Characteristics characteristics;
    private Set<MethodReference> asyncMethods;
    private boolean hasThreads;
    private MethodNodeCache astCache = EmptyMethodNodeCache.INSTANCE;
    private boolean incremental;
    private boolean lineNumbersGenerated;
    private SimpleStringPool stringPool;
    private boolean longjmpUsed = true;
    private boolean heapDump;
    private boolean obfuscated;
    private List<CallSiteDescriptor> callSites = new ArrayList<>();

    public CTarget(NameProvider nameProvider) {
        rawNameProvider = nameProvider;
    }

    public void setMinHeapSize(int minHeapSize) {
        this.minHeapSize = minHeapSize;
    }

    public void setMaxHeapSize(int maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public void setLineNumbersGenerated(boolean lineNumbersGenerated) {
        this.lineNumbersGenerated = lineNumbersGenerated;
    }

    public void setLongjmpUsed(boolean longjmpUsed) {
        this.longjmpUsed = longjmpUsed;
    }

    public void setHeapDump(boolean heapDump) {
        this.heapDump = heapDump;
    }

    public void setAstCache(MethodNodeCache astCache) {
        this.astCache = astCache;
    }

    public void setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    @Override
    public List<ClassHolderTransformer> getTransformers() {
        List<ClassHolderTransformer> transformers = new ArrayList<>();
        transformers.add(new ClassPatch());
        transformers.add(new CDependencyListener());
        transformers.add(new WeakReferenceTransformation());
        return transformers;
    }

    @Override
    public List<DependencyListener> getDependencyListeners() {
        return Arrays.asList(new CDependencyListener(), exportDependencyListener, new InteropDependencyListener(),
                new WeakReferenceDependencyListener());
    }

    @Override
    public void setController(TeaVMTargetController controller) {
        this.controller = controller;
        characteristics = new Characteristics(controller.getUnprocessedClassSource());
        classInitializerEliminator = new ClassInitializerEliminator(controller.getUnprocessedClassSource());
        classInitializerTransformer = new ClassInitializerTransformer();
        shadowStackTransformer = new ShadowStackTransformer(characteristics, !longjmpUsed);
        nullCheckInsertion = new NullCheckInsertion(new LowLevelNullCheckFilter(characteristics));
        checkTransformation = new CheckInstructionTransformation();
        writeBarrierInsertion = new WriteBarrierInsertion(characteristics);

        controller.addVirtualMethods(VIRTUAL_METHODS::contains);
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
    public void addGenerator(GeneratorFactory generatorFactory) {
        generatorFactories.add(generatorFactory);
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
        dependencyAnalyzer.linkMethod(new MethodReference(GC.class, "fixHeap", void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(GC.class, "tryShrink", void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(GC.class, "collectGarbage", void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(GC.class, "collectGarbageFull", void.class)).use();

        dependencyAnalyzer.linkMethod(new MethodReference(ExceptionHandling.class, "throwException",
                Throwable.class, void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(ExceptionHandling.class, "throwClassCastException",
                void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(ExceptionHandling.class, "throwNullPointerException",
                void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(ExceptionHandling.class,
                "throwArrayIndexOutOfBoundsException", void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(NullPointerException.class, "<init>", void.class))
                .propagate(0, NullPointerException.class.getName())
                .use();

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

        dependencyAnalyzer.linkMethod(new MethodReference(Fiber.class, "isResuming", boolean.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Fiber.class, "isSuspending", boolean.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Fiber.class, "current", Fiber.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Fiber.class, "startMain", String[].class, void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(EventQueue.class, "process", void.class)).use();
        dependencyAnalyzer.linkMethod(new MethodReference(Thread.class, "setCurrentThread", Thread.class,
                void.class)).use();

        ClassReader fiberClass = dependencyAnalyzer.getClassSource().get(Fiber.class.getName());
        for (MethodReader method : fiberClass.getMethods()) {
            if (method.getName().startsWith("pop") || method.getName().equals("push")) {
                dependencyAnalyzer.linkMethod(method.getReference()).use();
            }
        }

        dependencyAnalyzer.linkClass(CallSite.class.getName());
        dependencyAnalyzer.linkClass(CallSiteLocation.class.getName());

        dependencyAnalyzer.addDependencyListener(new ExceptionHandlingDependencyListener());
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
        if (!longjmpUsed) {
            checkTransformation.apply(program, method.getResultType());
        }
        new CoroutineTransformation(controller.getUnprocessedClassSource(), asyncMethods, hasThreads)
                .apply(program, method.getReference());
        ShadowStackTransformer shadowStackTransformer = !incremental
                ? this.shadowStackTransformer
                : new ShadowStackTransformer(characteristics, !longjmpUsed);
        shadowStackTransformer.apply(program, method);
        writeBarrierInsertion.apply(program);
    }

    @Override
    public void emit(ListableClassHolderSource classes, BuildTarget buildTarget, String outputName) throws IOException {
        VirtualTableProvider vtableProvider = !incremental ? createVirtualTableProvider(classes) : null;
        ClassHierarchy hierarchy = new ClassHierarchy(classes);
        TagRegistry tagRegistry = !incremental ? new TagRegistry(classes, hierarchy) : null;

        Decompiler decompiler = new Decompiler(classes, new HashSet<>(), controller.isFriendlyToDebugger());
        Characteristics characteristics = new Characteristics(controller.getUnprocessedClassSource());

        NameProvider nameProvider = new NameProviderWithSpecialNames(rawNameProvider,
                controller.getUnprocessedClassSource());

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
        intrinsics.add(new MemoryTraceIntrinsic());
        intrinsics.add(new MutatorIntrinsic());
        intrinsics.add(new ExceptionHandlingIntrinsic());
        intrinsics.add(new FunctionIntrinsic(characteristics, exportDependencyListener.getResolvedMethods()));
        intrinsics.add(new RuntimeClassIntrinsic());
        intrinsics.add(new FiberIntrinsic());
        intrinsics.add(new LongIntrinsic());
        intrinsics.add(new IntegerIntrinsic());
        intrinsics.add(new StringsIntrinsic());
        intrinsics.add(new ConsoleIntrinsic());

        List<Generator> generators = new ArrayList<>();
        generators.add(new ArrayGenerator());
        generators.add(new WeakReferenceGenerator());
        generators.add(new ReferenceQueueGenerator());

        stringPool = new SimpleStringPool();
        boolean vmAssertions = Boolean.parseBoolean(System.getProperty("teavm.c.vmAssertions", "false"));
        boolean gcStats = Boolean.parseBoolean(System.getProperty("teavm.c.gcStats", "false"));
        GenerationContext context = new GenerationContext(vtableProvider, characteristics,
                controller.getDependencyInfo(), stringPool, nameProvider, controller.getDiagnostics(), classes,
                intrinsics, generators, asyncMethods::contains, buildTarget,
                controller.getClassInitializerInfo(), incremental, longjmpUsed,
                vmAssertions, vmAssertions || heapDump, obfuscated);

        BufferedCodeWriter specialWriter = new BufferedCodeWriter(false);
        BufferedCodeWriter configHeaderWriter = new BufferedCodeWriter(false);

        configHeaderWriter.println("#pragma once");
        if (incremental) {
            configHeaderWriter.println("#define TEAVM_INCREMENTAL 1");
        }
        if (!longjmpUsed) {
            configHeaderWriter.println("#define TEAVM_USE_SETJMP 0");
        }
        if (vmAssertions) {
            configHeaderWriter.println("#define TEAVM_MEMORY_TRACE 1");
        }
        if (heapDump) {
            configHeaderWriter.println("#define TEAVM_HEAP_DUMP 1");
        }
        if (obfuscated) {
            configHeaderWriter.println("#define TEAVM_OBFUSCATED 1");
        }
        if (gcStats) {
            configHeaderWriter.println("#define TEAVM_GC_STATS 1");
        }

        ClassGenerator classGenerator = new ClassGenerator(context, tagRegistry, decompiler,
                controller.getCacheStatus());
        classGenerator.setAstCache(astCache);
        if (context.isLongjmp() && !context.isIncremental()) {
            classGenerator.setCallSites(callSites);
        }
        IntrinsicFactoryContextImpl intrinsicFactoryContext = new IntrinsicFactoryContextImpl(
                controller.getUnprocessedClassSource(), controller.getClassLoader(), controller.getServices(),
                controller.getProperties());
        for (IntrinsicFactory intrinsicFactory : intrinsicFactories) {
            context.addIntrinsic(intrinsicFactory.createIntrinsic(intrinsicFactoryContext));
        }
        for (GeneratorFactory generatorFactory : generatorFactories) {
            context.addGenerator(generatorFactory.createGenerator(intrinsicFactoryContext));
        }

        generateClasses(classes, classGenerator, buildTarget);

        generateSpecialFunctions(context, specialWriter);
        OutputFileUtil.write(configHeaderWriter, "config.h", buildTarget);
        OutputFileUtil.write(specialWriter, "special.c", buildTarget);
        for (String runtimeFile : RUNTIME_FILES) {
            copyResource(runtimeFile, buildTarget);
        }
        generateCallSites(buildTarget, context, classes.getClassNames());
        generateStrings(buildTarget, context);

        List<ValueType> types = classGenerator.getTypes().stream()
                .filter(c -> ClassGenerator.needsVirtualTable(characteristics, c))
                .collect(Collectors.toList());
        generateMainFile(context, classes, types, buildTarget);
        generateAllFile(classes, types, buildTarget);
    }

    private void copyResource(String name, BuildTarget buildTarget) throws IOException {
        BufferedCodeWriter writer = new BufferedCodeWriter(false);
        emitResource(writer, name);
        OutputFileUtil.write(writer, name, buildTarget);
    }

    private void emitResource(CodeWriter writer, String resourceName) {
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

    private void generateClasses(ListableClassHolderSource classes, ClassGenerator classGenerator,
            BuildTarget buildTarget) throws IOException {
        classGenerator.prepare(classes);

        for (String className : classes.getClassNames()) {
            BufferedCodeWriter writer = new BufferedCodeWriter(lineNumbersGenerated);
            BufferedCodeWriter headerWriter = new BufferedCodeWriter(false);
            ClassHolder cls = classes.get(className);
            if (cls != null) {
                classGenerator.generateClass(writer, headerWriter, cls);
            }
            String name = ClassGenerator.fileName(className);
            OutputFileUtil.write(writer, name + ".c", buildTarget);
            OutputFileUtil.write(headerWriter, name + ".h", buildTarget);
            if (incremental) {
                stringPool.reset();
            }
        }

        for (ValueType type : classGenerator.getTypes()) {
            if (type instanceof ValueType.Object) {
                continue;
            }
            BufferedCodeWriter writer = new BufferedCodeWriter(false);
            BufferedCodeWriter headerWriter = new BufferedCodeWriter(false);
            classGenerator.generateType(writer, headerWriter, type);
            String name = ClassGenerator.fileName(type);
            OutputFileUtil.write(writer, name + ".c", buildTarget);
            OutputFileUtil.write(headerWriter, name + ".h", buildTarget);
            if (incremental) {
                stringPool.reset();
            }
        }
    }

    private void generateCallSites(BuildTarget buildTarget, GenerationContext context,
            Collection<? extends String> classNames) throws IOException {
        BufferedCodeWriter writer = new BufferedCodeWriter(false);

        IncludeManager includes = new SimpleIncludeManager(writer);
        includes.init("callsites.c");

        if (!incremental) {
            generateFastCallSites(context, writer, includes, classNames);
        }

        OutputFileUtil.write(writer, "callsites.c", buildTarget);
    }

    private void generateFastCallSites(GenerationContext context, CodeWriter writer, IncludeManager includes,
            Collection<? extends String> classNames) {
        List<? extends CallSiteDescriptor> callSites = context.isLongjmp()
                ? this.callSites
                : CallSiteDescriptor.extract(context.getClassSource(), classNames);
        new CallSiteGenerator(context, writer, includes, "teavm_callSites").generate(callSites);
        if (obfuscated) {
            generateCallSitesJson(context.getBuildTarget(), callSites);
        }
    }

    private void generateCallSitesJson(BuildTarget buildTarget, List<? extends CallSiteDescriptor> callSites) {
        try (OutputStream output = buildTarget.createResource("callsites.json");
                Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
            writer.append("[\n");
            boolean first = true;
            for (CallSiteDescriptor descriptor : callSites) {
                if (!first) {
                    writer.append(",\n");
                }
                first = false;
                writer.append("{\"id\":").append(Integer.toString(descriptor.getId()));
                writer.append(",\"locations\":[");
                org.teavm.model.lowlevel.CallSiteLocation[] locations = descriptor.getLocations();
                if (locations != null) {
                    boolean firstLocation = true;
                    for (org.teavm.model.lowlevel.CallSiteLocation location : locations) {
                        if (!firstLocation) {
                            writer.append(",");
                        }
                        firstLocation = false;
                        writer.append("{\"class\":");
                        appendJsonString(writer, location.getClassName());
                        writer.append(",\"method\":");
                        appendJsonString(writer, location.getMethodName());
                        writer.append(",\"file\":");
                        appendJsonString(writer, location.getFileName());
                        writer.append(",\"line\":").append(Integer.toString(location.getLineNumber()));
                        writer.append("}");
                    }
                }
                writer.append("]}");
            }
            if (!first) {
                writer.append("\n");
            }
            writer.append("]");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void appendJsonString(Writer writer, String string) throws IOException {
        if (string == null) {
            writer.append("null");
            return;
        }

        writer.append("\"");
        JsonUtil.writeEscapedString(writer, string);
        writer.append("\"");
    }

    private void generateStrings(BuildTarget buildTarget, GenerationContext context) throws IOException {
        BufferedCodeWriter writer = new BufferedCodeWriter(false);
        IncludeManager includes = new SimpleIncludeManager(writer);
        includes.init("strings.c");
        BufferedCodeWriter headerWriter = new BufferedCodeWriter(false);

        headerWriter.println("#pragma once");
        headerWriter.println("#include \"runtime.h\"");
        headerWriter.println("extern void teavm_initStringPool();");
        if (!incremental) {
            headerWriter.println("extern TeaVM_String* teavm_stringPool[];");
            headerWriter.println("#define TEAVM_GET_STRING(i) teavm_stringPool[i]");
            headerWriter.println("#define TEAVM_GET_STRING_ADDRESS(i) (teavm_stringPool + i)");

            includes.includePath("strings.h");
            includes.includePath("stringhash.h");
            StringPoolGenerator poolGenerator = new StringPoolGenerator(context, "teavm_stringPool");
            poolGenerator.generate(writer);
            writer.println("void teavm_initStringPool() {").indent();
            poolGenerator.generateStringPoolHeaders(writer, includes);
            writer.outdent().println("}");
        } else {
            writer.println("void teavm_initStringPool() {}");
        }

        OutputFileUtil.write(writer, "strings.c", buildTarget);
        OutputFileUtil.write(headerWriter, "strings.h", buildTarget);
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

    private void generateSpecialFunctions(GenerationContext context, CodeWriter writer) {
        IncludeManager includes = new SimpleIncludeManager(writer);
        includes.init("special.c");
        includes.includePath("core.h");
        includes.includePath("string.h");
        ClassGenerationContext classContext = new ClassGenerationContext(context, includes,
                writer.fragment(), null, null);
        generateThrowCCE(classContext, writer);
        generateAllocateStringArray(classContext, writer, includes);
        generateAllocateCharArray(classContext, writer, includes);
        generateCreateString(classContext, writer, includes);
    }

    private void generateThrowCCE(ClassGenerationContext classContext, CodeWriter writer) {
        MethodReference methodRef = new MethodReference(ExceptionHandling.class,
                "throwClassCastException", void.class);
        classContext.importMethod(methodRef, true);
        writer.println("void* teavm_throwClassCastException() {").indent();
        String methodName = classContext.getContext().getNames().forMethod(methodRef);
        writer.println(methodName + "();");
        writer.println("return NULL;");
        writer.outdent().println("}");
    }

    private void generateAllocateStringArray(ClassGenerationContext context, CodeWriter writer,
            IncludeManager includes) {
        NameProvider names = context.getContext().getNames();
        MethodReference allocMethod = new MethodReference(Allocator.class,
                "allocateArray", RuntimeClass.class, int.class, Address.class);
        context.importMethod(allocMethod, true);
        includes.includeType(ValueType.parse(String[].class));
        writer.println("TeaVM_Array* teavm_allocateStringArray(int32_t size) {").indent();
        String allocateArrayName = names.forMethod(allocMethod);
        String stringClassName = names.forClassInstance(ValueType.arrayOf(
                ValueType.object(String.class.getName())));
        writer.println("return (TeaVM_Array*) " + allocateArrayName + "(&" + stringClassName + ", size);");
        writer.outdent().println("}");
    }

    private void generateAllocateCharArray(ClassGenerationContext context, CodeWriter writer,
            IncludeManager includes) {
        NameProvider names = context.getContext().getNames();
        MethodReference allocMethod = new MethodReference(Allocator.class,
                "allocateArray", RuntimeClass.class, int.class, Address.class);
        context.importMethod(allocMethod, true);
        includes.includeType(ValueType.parse(char[].class));
        writer.println("TeaVM_Array* teavm_allocateCharArray(int32_t size) {").indent();
        String allocateArrayName = names.forMethod(allocMethod);
        String charClassName = names.forClassInstance(ValueType.arrayOf(ValueType.CHARACTER));
        writer.println("return (TeaVM_Array*) " + allocateArrayName + "(&" + charClassName + ", size);");
        writer.outdent().println("}");
    }

    private void generateCreateString(ClassGenerationContext context, CodeWriter writer, IncludeManager includes) {
        NameProvider names = context.getContext().getNames();
        context.importMethod(CodeGenerationVisitor.ALLOC_METHOD, true);
        includes.includeClass(String.class.getName());
        writer.println("TeaVM_String* teavm_createString(TeaVM_Array* array) {").indent();
        writer.print("TeaVM_String* str = (TeaVM_String*) ").print(names.forMethod(CodeGenerationVisitor.ALLOC_METHOD))
                .print("(&").print(names.forClassInstance(ValueType.object("java.lang.String"))).println(");");
        writer.print("str->characters = array;");
        writer.println("return str;");
        writer.outdent().println("}");
    }

    private void generateMainFile(GenerationContext context, ListableClassHolderSource classes,
            List<? extends ValueType> types, BuildTarget buildTarget) throws IOException {
        BufferedCodeWriter writer = new BufferedCodeWriter(false);
        IncludeManager includes = new SimpleIncludeManager(writer);
        includes.init("main.c");
        includes.includePath("runtime.h");
        includes.includePath("strings.h");

        generateArrayOfClassReferences(context, writer, includes, types);
        generateMain(context, writer, includes, classes, types);
        OutputFileUtil.write(writer, "main.c", buildTarget);
    }

    private void generateAllFile(ListableClassHolderSource classes, List<? extends ValueType> types,
            BuildTarget buildTarget) throws IOException {
        List<String> allFiles = getGeneratedFiles(classes, types);

        BufferedCodeWriter writer = new BufferedCodeWriter(false);
        writer.println("#define _XOPEN_SOURCE");
        writer.println("#define __USE_XOPEN");
        writer.println("#define _GNU_SOURCE");

        IncludeManager includes = new SimpleIncludeManager(writer);
        includes.init("all.c");
        for (String file : allFiles) {
            includes.includePath(file);
        }

        OutputFileUtil.write(writer, "all.c", buildTarget);

        writer = new BufferedCodeWriter(false);
        for (String file : allFiles) {
            writer.println(file);
        }
        OutputFileUtil.write(writer, "all.txt", buildTarget);
    }

    private List<String> getGeneratedFiles(ListableClassHolderSource classes, List<? extends ValueType> types) {
        List<String> files = new ArrayList<>();
        files.add("callsites.c");
        files.add("core.c");
        files.add("date.c");
        files.add("fiber.c");
        files.add("file.c");
        files.add("heapdump.c");
        files.add("heaptrace.c");
        files.add("log.c");
        files.add("memory.c");
        files.add("references.c");
        files.add("resource.c");
        files.add("special.c");
        files.add("stack.c");
        files.add("string.c");
        files.add("stringhash.c");
        files.add("strings.c");
        files.add("time.c");
        files.add("virtcall.c");

        for (String className : classes.getClassNames()) {
            files.add(ClassGenerator.fileName(className) + ".c");
        }
        for (ValueType type : types) {
            files.add(ClassGenerator.fileName(type) + ".c");
        }

        files.add("main.c");

        files.sort(String::compareTo);
        return files;
    }

    private void generateArrayOfClassReferences(GenerationContext context, CodeWriter writer, IncludeManager includes,
            List<? extends ValueType> types) {
        writer.print("TeaVM_Class* teavm_classReferences[" + types.size() + "] = {").indent();
        boolean first = true;
        for (ValueType type : types) {
            if (!first) {
                writer.print(", ");
            }
            writer.println();
            first = false;
            String typeName = context.getNames().forClassInstance(type);
            includes.includeType(type);
            writer.print("(TeaVM_Class*) &" + typeName);
        }
        if (!first) {
            writer.println();
        }
        writer.outdent().println("};");

        writer.println("int32_t teavm_classReferencesCount = " + types.size() + ";");
    }

    private void generateMain(GenerationContext context, CodeWriter writer, IncludeManager includes,
            ListableClassHolderSource classes, List<? extends ValueType> types) {
        Iterator<? extends TeaVMEntryPoint> entryPointIter = controller.getEntryPoints().values().iterator();
        String mainFunctionName = entryPointIter.hasNext() ? entryPointIter.next().getPublicName() : null;
        if (mainFunctionName == null) {
            mainFunctionName = "main";
        }

        ClassGenerationContext classContext = new ClassGenerationContext(context, includes, writer.fragment(),
                null, null);

        writer.println("int " + mainFunctionName + "(int argc, char** argv) {").indent();

        writer.println("teavm_beforeInit();");
        writer.println("teavm_initHeap(" + minHeapSize + ", " + maxHeapSize + ");");
        generateVirtualTableHeaders(context, writer);
        writer.println("teavm_initStringPool();");
        for (ValueType type : types) {
            includes.includeType(type);
            writer.println(context.getNames().forClassSystemInitializer(type) + "();");
        }
        writer.println("teavm_afterInitClasses();");
        generateStaticInitializerCalls(classContext, writer, classes);
        if (context.getClassInitializerInfo().isDynamicInitializer("java.lang.String")) {
            writer.println(context.getNames().forClassInitializer("java.lang.String") + "();");
        }
        generateFiberStart(classContext, writer);

        writer.println("return 0;");

        writer.outdent().println("}");
    }

    private void generateStaticInitializerCalls(ClassGenerationContext context, CodeWriter writer,
            ListableClassReaderSource classes) {
        NameProvider names = context.getContext().getNames();
        Characteristics characteristics = context.getContext().getCharacteristics();
        MethodDescriptor clinitDescriptor = new MethodDescriptor("<clinit>", ValueType.VOID);
        if (classes.getClassNames().contains(GC.class.getName())) {
            MethodReference methodRef = new MethodReference(GC.class.getName(), clinitDescriptor);
            context.importMethod(methodRef, true);
            writer.println(names.forMethod(methodRef) + "();");
        }

        for (String className : classes.getClassNames()) {
            if (className.equals(GC.class.getName())) {
                continue;
            }
            ClassReader cls = classes.get(className);
            if (!characteristics.isStaticInit(cls.getName()) && !characteristics.isStructure(cls.getName())) {
                continue;
            }


            if (cls.getMethod(clinitDescriptor) == null) {
                continue;
            }

            MethodReference methodRef = new MethodReference(className, clinitDescriptor);
            context.importMethod(methodRef, true);
            writer.println(names.forMethod(methodRef) + "();");
        }
    }

    private void generateVirtualTableHeaders(GenerationContext context, CodeWriter writer) {
        writer.println("teavm_classClass = (TeaVM_Class*) &" + context.getNames().forClassInstance(
                ValueType.object("java.lang.Class")) + ";");
        writer.println("teavm_objectClass = (TeaVM_Class*) &" + context.getNames().forClassInstance(
                ValueType.object("java.lang.Object")) + ";");
        writer.println("teavm_stringClass = (TeaVM_Class*) &" + context.getNames().forClassInstance(
                ValueType.object("java.lang.String")) + ";");
        writer.println("teavm_charArrayClass = (TeaVM_Class*) &" + context.getNames().forClassInstance(
                ValueType.arrayOf(ValueType.CHARACTER)) + ";");
        writer.println("teavm_initClasses();");
    }

    private void generateFiberStart(ClassGenerationContext context, CodeWriter writer) {
        NameProvider names = context.getContext().getNames();
        MethodReference startRef = new MethodReference(Fiber.class, "startMain", String[].class, void.class);
        MethodReference processRef = new MethodReference(EventQueue.class, "process", void.class);
        context.importMethod(startRef, true);
        context.importMethod(processRef, true);
        writer.println(names.forMethod(startRef) + "(teavm_parseArguments(argc, argv));");
        writer.println(names.forMethod(processRef) + "();");
    }

    class FiberIntrinsic implements Intrinsic {
        @Override
        public boolean canHandle(MethodReference method) {
            if (!method.getClassName().equals(Fiber.class.getName())) {
                return false;
            }
            switch (method.getName()) {
                case "runMain":
                case "setCurrentThread":
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void apply(IntrinsicContext context, InvocationExpr invocation) {
            switch (invocation.getMethod().getName()) {
                case "runMain":
                    generateCallToMainMethod(context, invocation);
                    break;
                case "setCurrentThread":
                    MethodReference methodRef = new MethodReference(Thread.class,
                            "setCurrentThread", Thread.class, void.class);
                    context.importMethod(methodRef, true);
                    context.writer().print(context.names().forMethod(methodRef)).print("(");
                    context.emit(invocation.getArguments().get(0));
                    context.writer().print(")");
                    break;
            }
        }
    }

    private void generateCallToMainMethod(IntrinsicContext context, InvocationExpr invocation) {
        NameProvider names = context.names();
        Iterator<? extends TeaVMEntryPoint> entryPointIter = controller.getEntryPoints().values().iterator();
        if (entryPointIter.hasNext()) {
            TeaVMEntryPoint entryPoint = entryPointIter.next();
            context.importMethod(entryPoint.getMethod(), true);
            String mainMethod = names.forMethod(entryPoint.getMethod());
            context.writer().print(mainMethod + "(");
            context.emit(invocation.getArguments().get(0));
            context.writer().print(")");
        }
    }

    @Override
    public String[] getPlatformTags() {
        return new String[] { Platforms.C, Platforms.LOW_LEVEL };
    }

    @Override
    public boolean isAsyncSupported() {
        return true;
    }

    @Override
    public InliningFilterFactory getInliningFilter() {
        return new LowLevelInliningFilterFactory(characteristics);
    }
}
