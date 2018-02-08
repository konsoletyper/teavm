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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.teavm.backend.c.CTarget;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.backend.javascript.rendering.RenderingManager;
import org.teavm.backend.wasm.WasmTarget;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.cache.DiskCachedClassHolderSource;
import org.teavm.cache.DiskProgramCache;
import org.teavm.cache.DiskRegularMethodNodeCache;
import org.teavm.cache.FileSymbolTable;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationBuilder;
import org.teavm.dependency.DependencyInfo;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.ProgramReader;
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
import org.teavm.vm.spi.AbstractRendererListener;

public class TeaVMTool implements BaseTeaVMTool {
    private File targetDirectory = new File(".");
    private TeaVMTargetType targetType = TeaVMTargetType.JAVASCRIPT;
    private String targetFileName = "";
    private boolean minifying = true;
    private String mainClass;
    private RuntimeCopyOperation runtime = RuntimeCopyOperation.SEPARATE;
    private Properties properties = new Properties();
    private boolean debugInformationGenerated;
    private boolean sourceMapsFileGenerated;
    private boolean sourceFilesCopied;
    private boolean incremental;
    private File cacheDirectory = new File("./teavm-cache");
    private List<ClassHolderTransformer> transformers = new ArrayList<>();
    private List<String> classesToPreserve = new ArrayList<>();
    private TeaVMToolLog log = new EmptyTeaVMToolLog();
    private ClassLoader classLoader = TeaVMTool.class.getClassLoader();
    private DiskCachedClassHolderSource cachedClassSource;
    private DiskProgramCache programCache;
    private DiskRegularMethodNodeCache astCache;
    private FileSymbolTable symbolTable;
    private FileSymbolTable fileTable;
    private boolean cancelled;
    private TeaVMProgressListener progressListener;
    private TeaVM vm;
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

    @Override
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

    @Override
    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public boolean isIncremental() {
        return incremental;
    }

    @Override
    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public RuntimeCopyOperation getRuntime() {
        return runtime;
    }

    public void setRuntime(RuntimeCopyOperation runtime) {
        this.runtime = runtime;
    }

    public boolean isDebugInformationGenerated() {
        return debugInformationGenerated;
    }

    @Override
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

    @Override
    public void setSourceMapsFileGenerated(boolean sourceMapsFileGenerated) {
        this.sourceMapsFileGenerated = sourceMapsFileGenerated;
    }

    public boolean isSourceFilesCopied() {
        return sourceFilesCopied;
    }

    @Override
    public void setSourceFilesCopied(boolean sourceFilesCopied) {
        this.sourceFilesCopied = sourceFilesCopied;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public List<ClassHolderTransformer> getTransformers() {
        return transformers;
    }

    public List<String> getClassesToPreserve() {
        return classesToPreserve;
    }

    public TeaVMToolLog getLog() {
        return log;
    }

    @Override
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

    public void setMinHeapSize(int minHeapSize) {
        this.minHeapSize = minHeapSize;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
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

        Set<String> resources = new HashSet<>();
        ClassReaderSource classSource = vm.getDependencyClassSource();
        InstructionLocationReader reader = new InstructionLocationReader(resources);
        for (MethodReference methodRef : vm.getMethods()) {
            ClassReader cls = classSource.get(methodRef.getClassName());
            if (cls == null) {
                continue;
            }

            MethodReader method = cls.getMethod(methodRef.getDescriptor());
            if (method == null) {
                continue;
            }

            ProgramReader program = method.getProgram();
            if (program == null) {
                continue;
            }

            for (int i = 0; i < program.basicBlockCount(); ++i) {
                program.basicBlockAt(i).readAllInstructions(reader);
            }
        }

        return resources;
    }

    @Override
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

        if (incremental) {
            javaScriptTarget.setAstCache(astCache);
        }

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
            log.info("Building JavaScript file");
            TeaVMBuilder vmBuilder = new TeaVMBuilder(prepareTarget());
            if (incremental) {
                cacheDirectory.mkdirs();
                symbolTable = new FileSymbolTable(new File(cacheDirectory, "symbols"));
                fileTable = new FileSymbolTable(new File(cacheDirectory, "files"));
                ClasspathClassHolderSource innerClassSource = new ClasspathClassHolderSource(classLoader);
                ClassHolderSource classSource = new PreOptimizingClassHolderSource(innerClassSource);
                cachedClassSource = new DiskCachedClassHolderSource(cacheDirectory, symbolTable, fileTable,
                        classSource, innerClassSource);
                programCache = new DiskProgramCache(cacheDirectory, symbolTable, fileTable, innerClassSource);

                if (targetType == TeaVMTargetType.JAVASCRIPT) {
                    astCache = new DiskRegularMethodNodeCache(cacheDirectory, symbolTable, fileTable, innerClassSource);
                }
                try {
                    symbolTable.update();
                    fileTable.update();
                } catch (IOException e) {
                    log.info("Cache is missing");
                }
                vmBuilder.setClassLoader(classLoader).setClassSource(cachedClassSource);
            } else {
                vmBuilder.setClassLoader(classLoader).setClassSource(new PreOptimizingClassHolderSource(
                        new ClasspathClassHolderSource(classLoader)));
            }
            vm = vmBuilder.build();
            if (progressListener != null) {
                vm.setProgressListener(progressListener);
            }

            vm.setProperties(properties);
            vm.setProgramCache(programCache);
            vm.setIncremental(incremental);
            vm.setOptimizationLevel(optimizationLevel);

            vm.installPlugins();
            for (ClassHolderTransformer transformer : transformers) {
                vm.add(transformer);
            }
            if (mainClass != null) {
                MethodDescriptor mainMethodDesc = new MethodDescriptor("main", String[].class, void.class);
                vm.entryPoint("main", new MethodReference(mainClass, mainMethodDesc))
                        .withValue(1, "[java.lang.String")
                        .withArrayValue(1, "java.lang.String")
                        .async();
            }
            for (String className : classesToPreserve) {
                vm.preserveType(className);
            }
            targetDirectory.mkdirs();

            if (runtime == RuntimeCopyOperation.MERGED) {
                javaScriptTarget.add(runtimeInjector);
            }
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
                TeaVMProblemRenderer.describeProblems(vm, log);
            } else {
                log.info("Output file built with errors");
                TeaVMProblemRenderer.describeProblems(vm, log);
            }

            File outputFile = new File(targetDirectory, outputName);
            generatedFiles.add(outputFile);

            if (targetType == TeaVMTargetType.JAVASCRIPT) {
                try (OutputStream output = new FileOutputStream(new File(targetDirectory, outputName), true)) {
                    try (Writer writer = new OutputStreamWriter(output, "UTF-8")) {
                        additionalJavaScriptOutput(writer);
                    }
                }
            }

            if (incremental) {
                programCache.flush();
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
        if (mainClass != null) {
            writer.append("main = $rt_mainStarter(main);\n");
        }

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
            try (Writer sourceMapsOut = new OutputStreamWriter(new FileOutputStream(sourceMapsFile), "UTF-8")) {
                debugInfo.writeAsSourceMaps(sourceMapsOut, "src", getResolvedTargetFileName());
            }
            generatedFiles.add(sourceMapsFile);
            log.info("Source maps successfully written");
        }
        if (sourceFilesCopied) {
            copySourceFiles();
            log.info("Source files successfully written");
        }

        if (runtime == RuntimeCopyOperation.SEPARATE) {
            resourceToFile("org/teavm/backend/javascript/runtime.js", "runtime.js");
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

    private AbstractRendererListener runtimeInjector = new AbstractRendererListener() {
        @Override
        public void begin(RenderingManager manager, BuildTarget buildTarget) throws IOException {
            StringWriter writer = new StringWriter();
            resourceToWriter("org/teavm/backend/javascript/runtime.js", writer);
            writer.close();
            manager.getWriter().append(writer.toString()).newLine();
        }
    };

    private void resourceToFile(String resource, String fileName) throws IOException {
        try (InputStream input = TeaVMTool.class.getClassLoader().getResourceAsStream(resource)) {
            File outputFile = new File(targetDirectory, fileName);
            try (OutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                IOUtils.copy(new BufferedInputStream(input), output);
            }
            generatedFiles.add(outputFile);
        }
    }

    private void resourceToWriter(String resource, Writer writer) throws IOException {
        try (InputStream input = TeaVMTool.class.getClassLoader().getResourceAsStream(resource)) {
            IOUtils.copy(new BufferedInputStream(input), writer, "UTF-8");
        }
    }
}
