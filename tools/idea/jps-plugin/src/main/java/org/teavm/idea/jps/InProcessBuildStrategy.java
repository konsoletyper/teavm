/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.idea.jps;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.teavm.callgraph.CallGraph;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.idea.jps.model.TeaVMBuildResult;
import org.teavm.idea.jps.model.TeaVMBuildStrategy;
import org.teavm.idea.jps.util.ExceptionUtil;
import org.teavm.tooling.EmptyTeaVMToolLog;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.sources.DirectorySourceFileProvider;
import org.teavm.tooling.sources.JarSourceFileProvider;
import org.teavm.tooling.sources.SourceFileProvider;
import org.teavm.vm.TeaVMProgressListener;

public class InProcessBuildStrategy implements TeaVMBuildStrategy {
    private final CompileContext context;
    private List<String> classPathEntries = new ArrayList<>();
    private TeaVMTargetType targetType;
    private String mainClass;
    private String targetDirectory;
    private boolean sourceMapsFileGenerated;
    private boolean debugInformationGenerated;
    private boolean sourceFilesCopied;
    private final List<SourceFileProvider> sourceFileProviders = new ArrayList<>();
    private TeaVMProgressListener progressListener;
    private Properties properties = new Properties();

    public InProcessBuildStrategy(CompileContext context) {
        this.context = context;
    }

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
        this.sourceFilesCopied = sourceFilesCopied;
    }

    @Override
    public void setProgressListener(TeaVMProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    public void setIncremental(boolean incremental) {
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    @Override
    public TeaVMBuildResult build() {
        TeaVMTool tool = new TeaVMTool();
        tool.setProgressListener(progressListener);
        tool.setLog(new EmptyTeaVMToolLog());
        tool.setTargetType(targetType);
        tool.setMainClass(mainClass);
        tool.setTargetDirectory(new File(targetDirectory));
        tool.setClassLoader(buildClassLoader());

        tool.setSourceMapsFileGenerated(sourceMapsFileGenerated);
        tool.setDebugInformationGenerated(debugInformationGenerated);
        tool.setSourceFilesCopied(sourceFilesCopied);

        tool.setMinifying(false);

        tool.getProperties().putAll(properties);

        for (SourceFileProvider fileProvider : sourceFileProviders) {
            tool.addSourceFileProvider(fileProvider);
        }

        boolean errorOccurred = false;
        String stackTrace = null;
        try {
            tool.generate();
        } catch (TeaVMToolException | RuntimeException | Error e) {
            e.printStackTrace(System.err);
            stackTrace = ExceptionUtil.exceptionToString(e);
            context.processMessage(new CompilerMessage("TeaVM", e));
            errorOccurred = true;
        }

        Set<String> generatedFiles = tool.getGeneratedFiles().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toSet());

        return new InProcessBuildResult(tool.getDependencyInfo().getCallGraph(), errorOccurred, stackTrace,
                tool.getProblemProvider(), tool.getClasses(), tool.getUsedResources(), generatedFiles);
    }

    private ClassLoader buildClassLoader() {
        URL[] urls = classPathEntries.stream().map(entry -> {
            try {
                return new File(entry).toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(entry);
            }
        }).toArray(URL[]::new);

        RenamingClassLoader classLoader = new RenamingClassLoader(urls, TeaVMBuilder.class.getClassLoader());
        classLoader.rename("org/objectweb/asm/", "org/teavm/asm/");
        return classLoader;
    }

    static class InProcessBuildResult implements TeaVMBuildResult {
        private CallGraph callGraph;
        private boolean errorOccurred;
        private String stackTrace;
        private ProblemProvider problemProvider;
        private Collection<String> classes;
        private Collection<String> usedResources;
        private Collection<String> generatedFiles;

        InProcessBuildResult(CallGraph callGraph, boolean errorOccurred, String stackTrace,
                ProblemProvider problemProvider, Collection<String> classes, Collection<String> usedResources,
                Collection<String> generatedFiles) {
            this.callGraph = callGraph;
            this.errorOccurred = errorOccurred;
            this.stackTrace = stackTrace;
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
        public boolean isErrorOccurred() {
            return errorOccurred;
        }

        @Override
        public String getStackTrace() {
            return stackTrace;
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
