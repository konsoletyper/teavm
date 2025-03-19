/*
 *  Copyright 2024 Alexey Andreev.
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

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.teavm.backend.wasm.debug.CompositeDebugLines;
import org.teavm.backend.wasm.debug.DebugLines;
import org.teavm.backend.wasm.debug.ExternalDebugFile;
import org.teavm.backend.wasm.debug.GCDebugInfoBuilder;
import org.teavm.backend.wasm.debug.sourcemap.SourceMapBuilder;
import org.teavm.backend.wasm.gc.TeaVMWasmGCHost;
import org.teavm.backend.wasm.gc.WasmGCClassConsumer;
import org.teavm.backend.wasm.gc.WasmGCClassConsumerContext;
import org.teavm.backend.wasm.gc.WasmGCDependencies;
import org.teavm.backend.wasm.generate.gc.WasmGCDeclarationsGenerator;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCCustomTypeMapperFactory;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.generate.gc.strings.WasmGCStringProvider;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGenerator;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGeneratorFactory;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGenerators;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicFactory;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsics;
import org.teavm.backend.wasm.model.WasmCustomSection;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.optimization.WasmUsageCounter;
import org.teavm.backend.wasm.render.WasmBinaryRenderer;
import org.teavm.backend.wasm.render.WasmBinaryStatsCollector;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.backend.wasm.render.WasmBinaryWriter;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.backend.wasm.transformation.gc.BaseClassesTransformation;
import org.teavm.backend.wasm.transformation.gc.ClassLoaderResourceTransformation;
import org.teavm.backend.wasm.transformation.gc.EntryPointTransformation;
import org.teavm.backend.wasm.transformation.gc.ReferenceQueueTransformation;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.dependency.DependencyInfo;
import org.teavm.dependency.DependencyListener;
import org.teavm.interop.Address;
import org.teavm.interop.Platforms;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ListableClassHolderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.lowlevel.Characteristics;
import org.teavm.model.lowlevel.LowLevelNullCheckFilter;
import org.teavm.model.transformation.BoundCheckInsertion;
import org.teavm.model.transformation.NullCheckInsertion;
import org.teavm.model.util.VariableCategoryProvider;
import org.teavm.runtime.heap.Heap;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVMTarget;
import org.teavm.vm.TeaVMTargetController;
import org.teavm.vm.spi.TeaVMHostExtension;

public class WasmGCTarget implements TeaVMTarget, TeaVMWasmGCHost {
    private TeaVMTargetController controller;
    private NullCheckInsertion nullCheckInsertion;
    private BoundCheckInsertion boundCheckInsertion = new BoundCheckInsertion();
    private boolean strict;
    private boolean obfuscated;
    private boolean debugInfo;
    private boolean compactMode;
    private SourceMapBuilder sourceMapBuilder;
    private String sourceMapLocation;
    private WasmDebugInfoLocation debugLocation = WasmDebugInfoLocation.EXTERNAL;
    private WasmDebugInfoLevel debugLevel = WasmDebugInfoLevel.FULL;
    private int bufferHeapMinSize = 1024 * 1024 * 2;
    private int bufferHeapMaxSize = 1024 * 1024 * 32;
    private List<WasmGCIntrinsicFactory> intrinsicFactories = new ArrayList<>();
    private Map<MethodReference, WasmGCIntrinsic> customIntrinsics = new HashMap<>();
    private List<WasmGCCustomTypeMapperFactory> customTypeMapperFactories = new ArrayList<>();
    private Map<MethodReference, WasmGCCustomGenerator> customCustomGenerators = new HashMap<>();
    private List<WasmGCCustomGeneratorFactory> customGeneratorFactories = new ArrayList<>();
    private EntryPointTransformation entryPointTransformation = new EntryPointTransformation();
    private List<WasmGCClassConsumer> classConsumers = new ArrayList<>();

    public void setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public void setDebugInfo(boolean debug) {
        this.debugInfo = debug;
    }

    public void setDebugInfoLevel(WasmDebugInfoLevel debugLevel) {
        this.debugLevel = debugLevel;
    }

    public void setDebugInfoLocation(WasmDebugInfoLocation debugLocation) {
        this.debugLocation = debugLocation;
    }

    public void setSourceMapBuilder(SourceMapBuilder sourceMapBuilder) {
        this.sourceMapBuilder = sourceMapBuilder;
    }

    public void setSourceMapLocation(String sourceMapLocation) {
        this.sourceMapLocation = sourceMapLocation;
    }

    public void setBufferHeapMinSize(int bufferHeapMinSize) {
        this.bufferHeapMinSize = bufferHeapMinSize;
    }

    public void setBufferHeapMaxSize(int bufferHeapMaxSize) {
        this.bufferHeapMaxSize = bufferHeapMaxSize;
    }

    public void setCompactMode(boolean compactMode) {
        this.compactMode = compactMode;
    }

    @Override
    public void addIntrinsicFactory(WasmGCIntrinsicFactory intrinsicFactory) {
        intrinsicFactories.add(intrinsicFactory);
    }

    @Override
    public void addIntrinsic(MethodReference method, WasmGCIntrinsic intrinsic) {
        customIntrinsics.put(method, intrinsic);
    }

    @Override
    public void addGeneratorFactory(WasmGCCustomGeneratorFactory factory) {
        customGeneratorFactories.add(factory);
    }

    @Override
    public void addGenerator(MethodReference method, WasmGCCustomGenerator generator) {
        customCustomGenerators.put(method, generator);
    }

    @Override
    public void addCustomTypeMapperFactory(WasmGCCustomTypeMapperFactory customTypeMapperFactory) {
        customTypeMapperFactories.add(customTypeMapperFactory);
    }

    @Override
    public void addClassConsumer(WasmGCClassConsumer consumer) {
        classConsumers.add(consumer);
    }

    @Override
    public void setController(TeaVMTargetController controller) {
        var characteristics = new Characteristics(controller.getUnprocessedClassSource());
        nullCheckInsertion = new NullCheckInsertion(new LowLevelNullCheckFilter(characteristics));
        this.controller = controller;
    }

    @Override
    public void setEntryPoint(String entryPoint, String name) {
        entryPointTransformation.setEntryPoint(entryPoint);
        entryPointTransformation.setEntryPointName(name);
    }

    @Override
    public VariableCategoryProvider variableCategoryProvider() {
        return null;
    }

    @Override
    public List<DependencyListener> getDependencyListeners() {
        return List.of();
    }

    @Override
    public List<ClassHolderTransformer> getTransformers() {
        return List.of(
                new BaseClassesTransformation(),
                new ClassLoaderResourceTransformation(),
                new ReferenceQueueTransformation(),
                entryPointTransformation
        );
    }

    @Override
    public void contributeDependencies(DependencyAnalyzer dependencyAnalyzer) {
        var deps = new WasmGCDependencies(dependencyAnalyzer);
        deps.contribute();
        deps.contributeStandardExports();
    }

    @Override
    public List<TeaVMHostExtension> getHostExtensions() {
        return List.of(this);
    }

    @Override
    public void beforeInlining(Program program, MethodReader method) {
        if (strict) {
            nullCheckInsertion.transformProgram(program, method.getReference());
        }
    }

    @Override
    public void beforeOptimizations(Program program, MethodReader method) {
        if (strict) {
            boundCheckInsertion.transformProgram(program, method.getReference());
        }
    }

    @Override
    public void afterOptimizations(Program program, MethodReader method) {
    }

    @Override
    public String[] getPlatformTags() {
        return new String[] { Platforms.WEBASSEMBLY_GC };
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public void emit(ListableClassHolderSource classes, BuildTarget buildTarget, String outputName) throws IOException {
        var module = new WasmModule();
        module.memoryExportName = "teavm.memory";
        var customGenerators = new WasmGCCustomGenerators(classes, controller.getServices(),
                customGeneratorFactories, customCustomGenerators,
                controller.getProperties());
        var intrinsics = new WasmGCIntrinsics(classes, controller.getServices(), intrinsicFactories, customIntrinsics);
        var debugInfoBuilder = new GCDebugInfoBuilder();
        var declarationsGenerator = new WasmGCDeclarationsGenerator(
                module,
                classes,
                controller.getUnprocessedClassSource(),
                controller.getClassLoader(),
                controller.getClassInitializerInfo(),
                controller.getDependencyInfo(),
                controller.getDiagnostics(),
                customGenerators,
                intrinsics,
                customTypeMapperFactories,
                controller::isVirtual,
                strict,
                controller.getEntryPoint()
        );
        declarationsGenerator.setFriendlyToDebugger(controller.isFriendlyToDebugger());
        declarationsGenerator.setCompactMode(compactMode);
        var moduleGenerator = new WasmGCModuleGenerator(declarationsGenerator);

        var classConsumerContext = createClassConsumerContext(classes, declarationsGenerator);
        for (var cls : classes.getClassNames()) {
            for (var consumer : classConsumers) {
                consumer.accept(classConsumerContext, cls);
            }
        }

        var internMethod = controller.getDependencyInfo().getMethod(new MethodReference(String.class,
                "intern", String.class));
        if (internMethod != null && internMethod.isUsed()) {
            var removeStringEntryFunction = moduleGenerator.generateReportGarbageCollectedStringFunction();
            removeStringEntryFunction.setExportName("teavm.reportGarbageCollectedString");
        }

        var exceptionMessageRef = new MethodReference(Throwable.class, "getMessage", Throwable.class);
        if (controller.getDependencyInfo().getMethod(exceptionMessageRef) != null) {
            var exceptionMessageFunction = declarationsGenerator.functions().forInstanceMethod(exceptionMessageRef);
            exceptionMessageFunction.setExportName("teavm.exceptionMessage");
        }

        var refQueueSupplyRef = new MethodReference(ReferenceQueue.class, "supply", Reference.class, void.class);
        if (controller.getDependencyInfo().getMethod(refQueueSupplyRef) != null) {
            var refQueueSupplyFunction = declarationsGenerator.functions().forInstanceMethod(refQueueSupplyRef);
            refQueueSupplyFunction.setExportName("teavm.reportGarbageCollectedValue");
        }

        var buffersHeap = needsBuffersHeap(controller.getDependencyInfo());
        if (buffersHeap) {
            declarationsGenerator.functions().forStaticMethod(new MethodReference(Heap.class, "init",
                    Address.class, int.class, int.class, void.class));
        }
        moduleGenerator.generate();
        customGenerators.contributeToModule(module);
        generateExceptionExports(declarationsGenerator);
        adjustModuleMemory(module, moduleGenerator, buffersHeap);

        emitWasmFile(module, buildTarget, outputName, debugInfoBuilder);
    }

    private void generateExceptionExports(WasmGCDeclarationsGenerator declarationsGenerator) {
        var nativeExceptionField = declarationsGenerator.classInfoProvider().getThrowableNativeOffset();
        if (nativeExceptionField < 0) {
            return;
        }

        var throwableType = declarationsGenerator.classInfoProvider().getClassInfo("java.lang.Throwable")
                .getStructure();

        var getFunction = new WasmFunction(declarationsGenerator.functionTypes.of(
                WasmType.Reference.EXTERN, throwableType.getReference()
        ));
        getFunction.setName("teavm.getJsException");
        getFunction.setExportName("teavm.getJsException");
        var getParam = new WasmLocal(throwableType.getReference(), "javaException");
        getFunction.add(getParam);
        var getField = new WasmStructGet(throwableType, new WasmGetLocal(getParam), nativeExceptionField);
        getFunction.getBody().add(getField);
        declarationsGenerator.module.functions.add(getFunction);

        var setFunction = new WasmFunction(declarationsGenerator.functionTypes.of(null, throwableType.getReference(),
                WasmType.Reference.EXTERN));
        setFunction.setName("teavm.setJsException");
        setFunction.setExportName("teavm.setJsException");
        var setParam = new WasmLocal(throwableType.getReference(), "javaException");
        var setValue = new WasmLocal(WasmType.Reference.EXTERN, "jsException");
        setFunction.add(setParam);
        setFunction.add(setValue);
        var setField = new WasmStructSet(throwableType, new WasmGetLocal(setParam),
                nativeExceptionField, new WasmGetLocal(setValue));
        setFunction.getBody().add(setField);
        declarationsGenerator.module.functions.add(setFunction);
    }

    private WasmGCClassConsumerContext createClassConsumerContext(
            ClassReaderSource classes,
            WasmGCDeclarationsGenerator generator
    ) {
        return new WasmGCClassConsumerContext() {
            @Override
            public ClassReaderSource classes() {
                return classes;
            }

            @Override
            public WasmModule module() {
                return generator.module;
            }

            @Override
            public WasmFunctionTypes functionTypes() {
                return generator.functionTypes;
            }

            @Override
            public BaseWasmFunctionRepository functions() {
                return generator.functions();
            }

            @Override
            public WasmGCNameProvider names() {
                return generator.names();
            }

            @Override
            public WasmGCStringProvider strings() {
                return generator.strings();
            }

            @Override
            public WasmGCTypeMapper typeMapper() {
                return generator.typeMapper();
            }

            @Override
            public WasmTag exceptionTag() {
                return generator.exceptionTag();
            }

            @Override
            public String entryPoint() {
                return controller.getEntryPoint();
            }

            @Override
            public void addToInitializer(Consumer<WasmFunction> initializerContributor) {
                generator.addToInitializer(initializerContributor);
            }
        };
    }

    private void adjustModuleMemory(WasmModule module, WasmGCModuleGenerator moduleGenerator,
            boolean buffersHeap) {
        var memorySize = 0;
        for (var segment : module.getSegments()) {
            memorySize = Math.max(memorySize, segment.getOffset() + segment.getLength());
        }
        var maxMemorySize = memorySize;
        if (buffersHeap) {
            memorySize = ((memorySize - 1) / 256 + 1) * 256;
            moduleGenerator.initBuffersHeap(memorySize, bufferHeapMinSize, bufferHeapMaxSize);
            maxMemorySize = memorySize + bufferHeapMaxSize;
            memorySize += bufferHeapMinSize;
        }
        if (maxMemorySize == 0) {
            return;
        }

        var pages = (memorySize - 1) / WasmHeap.PAGE_SIZE + 1;
        module.setMinMemorySize(pages);

        pages = (maxMemorySize - 1) / WasmHeap.PAGE_SIZE + 1;
        module.setMaxMemorySize(pages);
    }

    private static boolean needsBuffersHeap(DependencyInfo dependencyInfo) {
        return dependencyInfo.getReachableMethods().stream()
                .anyMatch(m -> m.getClassName().equals(Heap.class.getName())
                        && !m.getName().equals("init"));
    }

    private void emitWasmFile(WasmModule module, BuildTarget buildTarget, String outputName,
            GCDebugInfoBuilder debugInfoBuilder) throws IOException {
        var binaryWriter = new WasmBinaryWriter();
        DebugLines debugLines = null;
        if (debugInfo) {
            if (sourceMapBuilder != null) {
                debugLines = new CompositeDebugLines(debugInfoBuilder.lines(), sourceMapBuilder);
            } else {
                debugLines = debugInfoBuilder.lines();
            }
        } else if (sourceMapBuilder != null) {
            debugLines = sourceMapBuilder;
        }
        if (!outputName.endsWith(".wasm")) {
            outputName += ".wasm";
        }
        if (sourceMapBuilder != null && sourceMapLocation != null) {
            var sourceMapBinding = new WasmBinaryWriter();
            sourceMapBinding.writeAsciiString(sourceMapLocation);
            var sourceMapSection = new WasmCustomSection("sourceMappingURL", sourceMapBinding.getData());
            module.add(sourceMapSection);
        }
        var binaryRenderer = new WasmBinaryRenderer(binaryWriter, WasmBinaryVersion.V_0x1, obfuscated,
                null, null, debugLines, null, WasmBinaryStatsCollector.EMPTY);
        optimizeIndexes(module);
        module.prepareForRendering();
        if (debugLocation == WasmDebugInfoLocation.EMBEDDED && debugInfo) {
            binaryRenderer.render(module, debugInfoBuilder::build);
        } else {
            binaryRenderer.render(module);
        }
        var data = binaryWriter.getData();
        try (var output = buildTarget.createResource(outputName)) {
            output.write(data);
        }
        if (debugLocation == WasmDebugInfoLocation.EXTERNAL && debugInfo) {
            var debugInfoData = ExternalDebugFile.write(debugInfoBuilder.build());
            if (debugInfoData != null) {
                try (var output = buildTarget.createResource(outputName + ".teadbg")) {
                    output.write(debugInfoData);
                }
            }
        }
    }

    private void optimizeIndexes(WasmModule module) {
        var usageCounter = new WasmUsageCounter();
        usageCounter.applyToModule(module);
        module.functions.sort(Comparator.comparingInt(f -> -usageCounter.usages(f)));
        module.globals.sort(Comparator.comparingInt(g -> -usageCounter.usages(g)));
        module.types.sort(Comparator.comparingInt(t -> -usageCounter.usages(t)));
    }

    @Override
    public boolean needsSystemArrayCopyOptimization() {
        return false;
    }

    @Override
    public boolean filterClassInitializer(String initializer) {
        if (initializer.equals(StringInternPool.class.getName())) {
            return false;
        }
        return true;
    }
}
