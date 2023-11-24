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
package org.teavm.tooling.builder;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.callgraph.CallGraph;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.tooling.EmptyTeaVMToolLog;
import org.teavm.tooling.TeaVMSourceFilePolicy;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.tooling.sources.JarSourceFileProvider;
import org.teavm.tooling.sources.SourceFileProvider;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.TeaVMProgressListener;

public class InProcessBuildStrategy implements BuildStrategy {
    private List<String> classPathEntries = new ArrayList<>();
    private TeaVMTargetType targetType;
    private String mainClass;
    private String entryPointName;
    private String targetDirectory;
    private String targetFileName = "";
    private boolean incremental;
    private String cacheDirectory;
    private TeaVMOptimizationLevel optimizationLevel = TeaVMOptimizationLevel.ADVANCED;
    private boolean fastDependencyAnalysis;
    private boolean obfuscated;
    private boolean strict;
    private boolean sourceMapsFileGenerated;
    private boolean debugInformationGenerated;
    private TeaVMSourceFilePolicy sourceMapsSourcePolicy;
    private String[] transformers = new String[0];
    private String[] classesToPreserve = new String[0];
    private WasmBinaryVersion wasmVersion = WasmBinaryVersion.V_0x1;
    private int minHeapSize = 4 * 1024 * 1024;
    private int maxHeapSize = 128 * 1024 * 1024;
    private final List<SourceFileProvider> sourceFileProviders = new ArrayList<>();
    private boolean heapDump;
    private TeaVMProgressListener progressListener;
    private Properties properties = new Properties();
    private TeaVMToolLog log = new EmptyTeaVMToolLog();
    private boolean shortFileNames;
    private boolean assertionsRemoved;

    @Override
    public void init() {
        sourceFileProviders.clear();
    }

    @Override
    public void addSourcesDirectory(String directory) {
        sourceFileProviders.add(new DirectorySourceFileProvider(new File(directory)));
    }

    @Override
    public void addSourcesJar(String jarFile) {
        sourceFileProviders.add(new JarSourceFileProvider(new File(jarFile)));
    }

    @Override
    public void setClassPathEntries(List<String> entries) {
        classPathEntries.clear();
        classPathEntries.addAll(entries);
    }

    @Override
    public void setTargetType(TeaVMTargetType targetType) {
        this.targetType = targetType;
    }

    @Override
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public void setEntryPointName(String entryPointName) {
        this.entryPointName = entryPointName;
    }

    @Override
    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    @Override
    public void setSourceMapsFileGenerated(boolean sourceMapsFileGenerated) {
        this.sourceMapsFileGenerated = sourceMapsFileGenerated;
    }

    @Override
    public void setDebugInformationGenerated(boolean debugInformationGenerated) {
        this.debugInformationGenerated = debugInformationGenerated;
    }

    @Override
    public void setSourceFilesCopied(boolean sourceFilesCopied) {
        if ((sourceMapsSourcePolicy == TeaVMSourceFilePolicy.COPY) == sourceFilesCopied) {
            return;
        }
        sourceMapsSourcePolicy = sourceFilesCopied
                ? TeaVMSourceFilePolicy.COPY
                : TeaVMSourceFilePolicy.DO_NOTHING;
    }

    @Override
    public void setSourceFilePolicy(TeaVMSourceFilePolicy sourceFilePolicy) {
        this.sourceMapsSourcePolicy = sourceFilePolicy;
    }

    @Override
    public void setProgressListener(TeaVMProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    @Override
    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    @Override
    public void setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    @Override
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    public void setTransformers(String[] transformers) {
        this.transformers = transformers.clone();
    }

    @Override
    public void setOptimizationLevel(TeaVMOptimizationLevel level) {
        this.optimizationLevel = level;
    }

    @Override
    public void setFastDependencyAnalysis(boolean fastDependencyAnalysis) {
        this.fastDependencyAnalysis = fastDependencyAnalysis;
    }

    @Override
    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    @Override
    public void setClassesToPreserve(String[] classesToPreserve) {
        this.classesToPreserve = classesToPreserve.clone();
    }

    @Override
    public void setCacheDirectory(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    public void setWasmVersion(WasmBinaryVersion wasmVersion) {
        this.wasmVersion = wasmVersion;
    }

    @Override
    public void setMinHeapSize(int minHeapSize) {
        this.minHeapSize = minHeapSize;
    }

    @Override
    public void setMaxHeapSize(int maxHeapSize) {
        this.maxHeapSize = maxHeapSize;
    }

    @Override
    public void setHeapDump(boolean heapDump) {
        this.heapDump = heapDump;
    }

    @Override
    public void setShortFileNames(boolean shortFileNames) {
        this.shortFileNames = shortFileNames;
    }

    @Override
    public void setAssertionsRemoved(boolean assertionsRemoved) {
        this.assertionsRemoved = assertionsRemoved;
    }

    @Override
    public BuildResult build() throws BuildException {
        TeaVMTool tool = new TeaVMTool();
        tool.setProgressListener(progressListener);
        tool.setLog(log);
        tool.setTargetType(targetType);
        tool.setMainClass(mainClass);
        tool.setEntryPointName(entryPointName);
        tool.setTargetDirectory(new File(targetDirectory));
        tool.setTargetFileName(targetFileName);
        var classLoader = buildClassLoader();
        tool.setClassLoader(classLoader);
        tool.setOptimizationLevel(optimizationLevel);
        tool.setFastDependencyAnalysis(fastDependencyAnalysis);

        tool.setSourceMapsFileGenerated(sourceMapsFileGenerated);
        tool.setDebugInformationGenerated(debugInformationGenerated);
        tool.setSourceFilePolicy(sourceMapsSourcePolicy);

        tool.setObfuscated(obfuscated);
        tool.setStrict(strict);
        tool.setIncremental(incremental);
        tool.getTransformers().addAll(Arrays.asList(transformers));
        tool.getClassesToPreserve().addAll(Arrays.asList(classesToPreserve));
        tool.setCacheDirectory(cacheDirectory != null ? new File(cacheDirectory) : null);
        tool.setWasmVersion(wasmVersion);
        tool.setMinHeapSize(minHeapSize);
        tool.setMaxHeapSize(maxHeapSize);
        tool.setHeapDump(heapDump);
        tool.setShortFileNames(shortFileNames);
        tool.setAssertionsRemoved(assertionsRemoved);

        tool.getProperties().putAll(properties);

        for (SourceFileProvider fileProvider : sourceFileProviders) {
            tool.addSourceFileProvider(fileProvider);
        }

        try {
            tool.generate();
            classLoader.close();
        } catch (TeaVMToolException | RuntimeException | Error | IOException e) {
            throw new BuildException(e);
        }

        var generatedFiles = tool.getGeneratedFiles().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toSet());

        return new InProcessBuildResult(tool.getDependencyInfo().getCallGraph(),
                tool.getProblemProvider(), tool.getClasses(), tool.getUsedResources(), generatedFiles);
    }

    private URLClassLoader buildClassLoader() {
        URL[] urls = classPathEntries.stream().map(entry -> {
            try {
                return new File(entry).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(entry);
            }
        }).toArray(URL[]::new);

        return new URLClassLoader(urls, InProcessBuildStrategy.class.getClassLoader());
    }

    static class InProcessBuildResult implements BuildResult {
        private CallGraph callGraph;
        private ProblemProvider problemProvider;
        private Collection<String> classes;
        private Collection<String> usedResources;
        private Collection<String> generatedFiles;

        InProcessBuildResult(CallGraph callGraph, ProblemProvider problemProvider,
                Collection<String> classes, Collection<String> usedResources, Collection<String> generatedFiles) {
            this.callGraph = callGraph;
            this.problemProvider = problemProvider;
            this.classes = classes;
            this.usedResources = usedResources;
            this.generatedFiles = generatedFiles;
        }

        @Override
        public CallGraph getCallGraph() {
            return callGraph;
        }

        @Override
        public ProblemProvider getProblems() {
            return problemProvider;
        }

        @Override
        public Collection<String> getClasses() {
            return classes;
        }

        @Override
        public Collection<String> getUsedResources() {
            return usedResources;
        }

        @Override
        public Collection<String> getGeneratedFiles() {
            return generatedFiles;
        }
    }
}
