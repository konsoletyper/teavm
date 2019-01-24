/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.tooling;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.teavm.backend.c.CTarget;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.backend.wasm.WasmTarget;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.cache.AlwaysStaleCacheStatus;
import org.teavm.cache.CacheStatus;
import org.teavm.cache.DiskCachedClassHolderSource;
import org.teavm.cache.DiskMethodNodeCache;
import org.teavm.cache.DiskProgramCache;
import org.teavm.cache.EmptyProgramCache;
import org.teavm.cache.FileSymbolTable;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationBuilder;
import org.teavm.dependency.DependencyInfo;
import org.teavm.dependency.FastDependencyAnalyzer;
import org.teavm.dependency.PreciseDependencyAnalyzer;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReader;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.tooling.sources.SourceFileProvider;
import org.teavm.tooling.sources.SourceFilesCopier;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.TeaVMProgressListener;
import org.teavm.vm.TeaVMTarget;

public class TeaVMTool {
    private File targetDirectory = new File(".");
    private TeaVMTargetType targetType = TeaVMTargetType.JAVASCRIPT;
    private String targetFileName = "";
    private boolean minifying = true;
    private String mainClass;
    private String entryPointName = "main";
    private Properties properties = new Properties();
    private boolean debugInformationGenerated;
    private boolean sourceMapsFileGenerated;
    private boolean sourceFilesCopied;
    private boolean incremental;
    private File cacheDirectory = new File("./teavm-cache");
    private List<String> transformers = new ArrayList<>();
    private List<String> classesToPreserve = new ArrayList<>();
    private TeaVMToolLog log = new EmptyTeaVMToolLog();
    private ClassLoader classLoader = TeaVMTool.class.getClassLoader();
    private DiskCachedClassHolderSource cachedClassSource;
    private DiskProgramCache programCache;
    private DiskMethodNodeCache astCache;
    private FileSymbolTable symbolTable;
    private FileSymbolTable fileTable;
    private boolean cancelled;
    private TeaVMProgressListener progressListener;
    private TeaVM vm;
    private boolean fastDependencyAnalysis;
    private TeaVMOptimizationLevel optimizationLevel = TeaVMOptimizationLevel.SIMPLE;
    private List<SourceFileProvider> sourceFileProviders = new ArrayList<>();
    private DebugInformationBuilder debugEmitter;
    private JavaScriptTarget javaScriptTarget;
    private WasmTarget webAssemblyTarget;
    private WasmBinaryVersion wasmVersion = WasmBinaryVersion.V_0x1;
    private CTarget cTarget;
    private Set<File> generatedFiles = new HashSet<>();
    private int minHeapSize = 32 * (1 << 20);

    public File getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public boolean isMinifying() {
        return minifying;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void setEntryPointName(String entryPointName) {
        this.entryPointName = entryPointName;
    }

    public boolean isDebugInformationGenerated() {
        return debugInformationGenerated;
    }

    public void setDebugInformationGenerated(boolean debugInformationGenerated) {
        this.debugInformationGenerated = debugInformationGenerated;
    }

    public File getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    public boolean isSourceMapsFileGenerated() {
        return sourceMapsFileGenerated;
    }

    public void setSourceMapsFileGenerated(boolean sourceMapsFileGenerated) {
        this.sourceMapsFileGenerated = sourceMapsFileGenerated;
    }

    public boolean isSourceFilesCopied() {
        return sourceFilesCopied;
    }

    public void setSourceFilesCopied(boolean sourceFilesCopied) {
        this.sourceFilesCopied = sourceFilesCopied;
    }

    public Properties getProperties() {
        return properties;
    }

    public List<String> getTransformers() {
        return transformers;
    }

    public List<String> getClassesToPreserve() {
        return classesToPreserve;
    }

    public TeaVMToolLog getLog() {
        return log;
    }

    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    public TeaVMTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TeaVMTargetType targetType) {
        this.targetType = targetType;
    }

    public TeaVMOptimizationLevel getOptimizationLevel() {
        return optimizationLevel;
    }

    public void setOptimizationLevel(TeaVMOptimizationLevel optimizationLevel) {
        this.optimizationLevel = optimizationLevel;
    }

    public boolean isFastDependencyAnalysis() {
        return fastDependencyAnalysis;
    }

    public void setFastDependencyAnalysis(boolean fastDependencyAnalysis) {
        this.fastDependencyAnalysis = fastDependencyAnalysis;
    }

    public void setMinHeapSize(int minHeapSize) {
        this.minHeapSize = minHeapSize;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public WasmBinaryVersion getWasmVersion() {
        return wasmVersion;
    }

    public void setWasmVersion(WasmBinaryVersion wasmVersion) {
        this.wasmVersion = wasmVersion;
    }

    public void setProgressListener(TeaVMProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public ProblemProvider getProblemProvider() {
        return vm != null ? vm.getProblemProvider() : null;
    }

    public DependencyInfo getDependencyInfo() {
        return vm.getDependencyInfo();
    }

    public Collection<String> getClasses() {
        return vm != null ? vm.getClasses() : Collections.emptyList();
    }

    public Set<File> getGeneratedFiles() {
        return generatedFiles;
    }

    public Collection<String> getUsedResources() {
        if (vm == null) {
            return Collections.emptyList();
        }

        return InstructionLocationReader.extractUsedResources(vm);
    }

    public void addSourceFileProvider(SourceFileProvider sourceFileProvider) {
        sourceFileProviders.add(sourceFileProvider);
    }

    private TeaVMTarget prepareTarget() {
        switch (targetType) {
            case JAVASCRIPT:
                return prepareJavaScriptTarget();
            case WEBASSEMBLY:
                return prepareWebAssemblyTarget();
            case C:
                return prepareCTarget();
        }
        throw new IllegalStateException("Unknown target type: " + targetType);
    }

    private TeaVMTarget prepareJavaScriptTarget() {
        javaScriptTarget = new JavaScriptTarget();
        javaScriptTarget.setMinifying(minifying);

        debugEmitter = debugInformationGenerated || sourceMapsFileGenerated
                ? new DebugInformationBuilder() : null;
        javaScriptTarget.setDebugEmitter(debugEmitter);

        return javaScriptTarget;
    }

    private WasmTarget prepareWebAssemblyTarget() {
        webAssemblyTarget = new WasmTarget();
        webAssemblyTarget.setDebugging(debugInformationGenerated);
        webAssemblyTarget.setCEmitted(debugInformationGenerated);
        webAssemblyTarget.setWastEmitted(debugInformationGenerated);
        webAssemblyTarget.setVersion(wasmVersion);
        webAssemblyTarget.setMinHeapSize(minHeapSize);
        return webAssemblyTarget;
    }

    private CTarget prepareCTarget() {
        cTarget = new CTarget();
        cTarget.setMinHeapSize(minHeapSize);
        return cTarget;
    }

    public void generate() throws TeaVMToolException {
        try {
            cancelled = false;
            log.info("Running TeaVM");
            TeaVMBuilder vmBuilder = new TeaVMBuilder(prepareTarget());
            CacheStatus cacheStatus;
            if (incremental) {
                cacheDirectory.mkdirs();
                symbolTable = new FileSymbolTable(new File(cacheDirectory, "symbols"));
                fileTable = new FileSymbolTable(new File(cacheDirectory, "files"));
                ClasspathClassHolderSource innerClassSource = new ClasspathClassHolderSource(classLoader);
                ClassHolderSource classSource = new PreOptimizingClassHolderSource(innerClassSource);
                cachedClassSource = new DiskCachedClassHolderSource(cacheDirectory, symbolTable, fileTable,
                        classSource, innerClassSource);
                programCache = new DiskProgramCache(cacheDirectory, symbolTable, fileTable);
                if (incremental && targetType == TeaVMTargetType.JAVASCRIPT) {
                    astCache = new DiskMethodNodeCache(cacheDirectory, symbolTable, fileTable);
                    javaScriptTarget.setAstCache(astCache);
                }
                try {
                    symbolTable.update();
                    fileTable.update();
                } catch (IOException e) {
                    log.info("Cache is missing");
                }
                vmBuilder.setClassLoader(classLoader).setClassSource(cachedClassSource);
                cacheStatus = cachedClassSource;
            } else {
                vmBuilder.setClassLoader(classLoader).setClassSource(new PreOptimizingClassHolderSource(
                        new ClasspathClassHolderSource(classLoader)));
                cacheStatus = AlwaysStaleCacheStatus.INSTANCE;
            }

            vmBuilder.setDependencyAnalyzerFactory(fastDependencyAnalysis
                    ? FastDependencyAnalyzer::new
                    : PreciseDependencyAnalyzer::new);

            vm = vmBuilder.build();
            if (progressListener != null) {
                vm.setProgressListener(progressListener);
            }

            vm.setProperties(properties);
            vm.setProgramCache(incremental ? programCache : EmptyProgramCache.INSTANCE);
            vm.setCacheStatus(cacheStatus);
            vm.setOptimizationLevel(!fastDependencyAnalysis && !incremental
                    ? optimizationLevel
                    : TeaVMOptimizationLevel.SIMPLE);
            if (incremental) {
                vm.addVirtualMethods(m -> true);
            }

            vm.installPlugins();
            for (ClassHolderTransformer transformer : resolveTransformers(classLoader)) {
                vm.add(transformer);
            }
            if (mainClass != null) {
                vm.entryPoint(mainClass, entryPointName);
            }
            for (String className : classesToPreserve) {
                vm.preserveType(className);
            }
            targetDirectory.mkdirs();

            BuildTarget buildTarget = new DirectoryBuildTarget(targetDirectory);
            String outputName = getResolvedTargetFileName();
            vm.build(buildTarget, outputName);
            if (vm.wasCancelled()) {
                log.info("Build cancelled");
                cancelled = true;
                return;
            }

            ProblemProvider problemProvider = vm.getProblemProvider();
            if (problemProvider.getProblems().isEmpty()) {
                log.info("Output file successfully built");
            } else if (problemProvider.getSevereProblems().isEmpty()) {
                log.info("Output file built with warnings");
            } else {
                log.info("Output file built with errors");
            }

            File outputFile = new File(targetDirectory, outputName);
            generatedFiles.add(outputFile);

            if (targetType == TeaVMTargetType.JAVASCRIPT) {
                try (OutputStream output = new FileOutputStream(new File(targetDirectory, outputName), true)) {
                    try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
                        additionalJavaScriptOutput(writer);
                    }
                }
            }

            if (incremental) {
                programCache.flush(vm.getDependencyClassSource());
                if (astCache != null) {
                    astCache.flush();
                }
                cachedClassSource.flush();
                symbolTable.flush();
                fileTable.flush();
                log.info("Cache updated");
            }

            printStats();
        } catch (IOException e) {
            throw new TeaVMToolException("IO error occurred", e);
        }
    }

    private String getResolvedTargetFileName() {
        if (targetFileName.isEmpty()) {
            switch (targetType) {
                case JAVASCRIPT:
                    return "classes.js";
                case WEBASSEMBLY:
                    return "classes.wasm";
                case C:
                    return "classes.c";
                default:
                    return "classes";
            }
        }
        return targetFileName;
    }

    private void additionalJavaScriptOutput(Writer writer) throws IOException {
        if (debugInformationGenerated) {
            assert debugEmitter != null;
            DebugInformation debugInfo = debugEmitter.getDebugInformation();
            File debugSymbolFile = new File(targetDirectory, getResolvedTargetFileName() + ".teavmdbg");
            try (OutputStream debugInfoOut = new BufferedOutputStream(new FileOutputStream(debugSymbolFile))) {
                debugInfo.write(debugInfoOut);
            }
            generatedFiles.add(debugSymbolFile);
            log.info("Debug information successfully written");
        }
        if (sourceMapsFileGenerated) {
            assert debugEmitter != null;
            DebugInformation debugInfo = debugEmitter.getDebugInformation();
            String sourceMapsFileName = getResolvedTargetFileName() + ".map";
            writer.append("\n//# sourceMappingURL=").append(sourceMapsFileName);
            File sourceMapsFile = new File(targetDirectory, sourceMapsFileName);
            try (Writer sourceMapsOut = new OutputStreamWriter(new FileOutputStream(sourceMapsFile),
                    StandardCharsets.UTF_8)) {
                debugInfo.writeAsSourceMaps(sourceMapsOut, "src", getResolvedTargetFileName());
            }
            generatedFiles.add(sourceMapsFile);
            log.info("Source maps successfully written");
        }
        if (sourceFilesCopied) {
            copySourceFiles();
            log.info("Source files successfully written");
        }
    }

    private void printStats() {
        if (vm == null || vm.getWrittenClasses() == null) {
            return;
        }

        int classCount = vm.getWrittenClasses().getClassNames().size();
        int methodCount = 0;
        for (String className : vm.getWrittenClasses().getClassNames()) {
            ClassReader cls = vm.getWrittenClasses().get(className);
            methodCount += cls.getMethods().size();
        }

        log.info("Classes compiled: " + classCount);
        log.info("Methods compiled: " + methodCount);
    }

    private void copySourceFiles() {
        if (vm.getWrittenClasses() == null) {
            return;
        }
        SourceFilesCopier copier = new SourceFilesCopier(sourceFileProviders, generatedFiles::add);
        copier.addClasses(vm.getWrittenClasses());
        copier.setLog(log);
        copier.copy(new File(targetDirectory, "src"));
    }

    private List<ClassHolderTransformer> resolveTransformers(ClassLoader classLoader) {
        List<ClassHolderTransformer> transformerInstances = new ArrayList<>();
        if (transformers == null) {
            return transformerInstances;
        }
        for (String transformerName : transformers) {
            Class<?> transformerRawType;
            try {
                transformerRawType = Class.forName(transformerName, true, classLoader);
            } catch (ClassNotFoundException e) {
                log.error("Transformer not found: " + transformerName, e);
                continue;
            }
            if (!ClassHolderTransformer.class.isAssignableFrom(transformerRawType)) {
                log.error("Transformer " + transformerName + " is not subtype of "
                        + ClassHolderTransformer.class.getName());
                continue;
            }
            Class<? extends ClassHolderTransformer> transformerType = transformerRawType.asSubclass(
                    ClassHolderTransformer.class);
            Constructor<? extends ClassHolderTransformer> ctor;
            try {
                ctor = transformerType.getConstructor();
            } catch (NoSuchMethodException e) {
                log.error("Transformer " + transformerName + " has no default constructor");
                continue;
            }
            try {
                ClassHolderTransformer transformer = ctor.newInstance();
                transformerInstances.add(transformer);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                log.error("Error instantiating transformer " + transformerName, e);
            }
        }
        return transformerInstances;
    }
}
