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

import java.io.*;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.teavm.cache.DiskCachedClassHolderSource;
import org.teavm.cache.DiskProgramCache;
import org.teavm.cache.DiskRegularMethodNodeCache;
import org.teavm.cache.FileSymbolTable;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationBuilder;
import org.teavm.dependency.DependencyInfo;
import org.teavm.diagnostics.ProblemProvider;
import org.teavm.javascript.RenderingContext;
import org.teavm.model.*;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.vm.*;
import org.teavm.vm.spi.AbstractRendererListener;

/**
 *
 * @author Alexey Andreev
 */
public class TeaVMTool {
    private File targetDirectory = new File(".");
    private String targetFileName = "classes.js";
    private boolean minifying = true;
    private String mainClass;
    private RuntimeCopyOperation runtime = RuntimeCopyOperation.SEPARATE;
    private Properties properties = new Properties();
    private boolean mainPageIncluded;
    private boolean bytecodeLogging;
    private boolean debugInformationGenerated;
    private boolean sourceMapsFileGenerated;
    private boolean sourceFilesCopied;
    private boolean incremental;
    private File cacheDirectory = new File("./teavm-cache");
    private List<ClassHolderTransformer> transformers = new ArrayList<>();
    private List<ClassAlias> classAliases = new ArrayList<>();
    private List<MethodAlias> methodAliases = new ArrayList<>();
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
    private List<SourceFileProvider> sourceFileProviders = new ArrayList<>();

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

    public RuntimeCopyOperation getRuntime() {
        return runtime;
    }

    public void setRuntime(RuntimeCopyOperation runtime) {
        this.runtime = runtime;
    }

    public boolean isMainPageIncluded() {
        return mainPageIncluded;
    }

    public void setMainPageIncluded(boolean mainPageIncluded) {
        this.mainPageIncluded = mainPageIncluded;
    }

    public boolean isBytecodeLogging() {
        return bytecodeLogging;
    }

    public void setBytecodeLogging(boolean bytecodeLogging) {
        this.bytecodeLogging = bytecodeLogging;
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

    public List<ClassHolderTransformer> getTransformers() {
        return transformers;
    }

    public List<ClassAlias> getClassAliases() {
        return classAliases;
    }

    public List<MethodAlias> getMethodAliases() {
        return methodAliases;
    }

    public TeaVMToolLog getLog() {
        return log;
    }

    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
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
        return vm != null ? vm.getClasses() : Collections.<String>emptyList();
    }

    public void addSourceFileProvider(SourceFileProvider sourceFileProvider) {
        sourceFileProviders.add(sourceFileProvider);
    }

    public void generate() throws TeaVMToolException {
        try {
            cancelled = false;
            log.info("Building JavaScript file");
            TeaVMBuilder vmBuilder = new TeaVMBuilder();
            if (incremental) {
                cacheDirectory.mkdirs();
                symbolTable = new FileSymbolTable(new File(cacheDirectory, "symbols"));
                fileTable = new FileSymbolTable(new File(cacheDirectory, "files"));
                ClasspathClassHolderSource innerClassSource = new ClasspathClassHolderSource(classLoader);
                ClassHolderSource classSource = new PreOptimizingClassHolderSource(innerClassSource);
                cachedClassSource = new DiskCachedClassHolderSource(cacheDirectory, symbolTable, fileTable,
                        classSource, innerClassSource);
                programCache = new DiskProgramCache(cacheDirectory, symbolTable, fileTable, innerClassSource);
                astCache = new DiskRegularMethodNodeCache(cacheDirectory, symbolTable, fileTable, innerClassSource);
                try {
                    symbolTable.update();
                    fileTable.update();
                } catch (IOException e) {
                    log.info("Cache was not read");
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
            vm.setMinifying(minifying);
            vm.setBytecodeLogging(bytecodeLogging);
            vm.setProperties(properties);
            DebugInformationBuilder debugEmitter = debugInformationGenerated || sourceMapsFileGenerated ?
                    new DebugInformationBuilder() : null;
            vm.setDebugEmitter(debugEmitter);
            vm.setIncremental(incremental);
            if (incremental) {
                vm.setAstCache(astCache);
                vm.setProgramCache(programCache);
            }
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
            for (ClassAlias alias : classAliases) {
                vm.exportType(alias.getAlias(), alias.getClassName());
            }
            for (MethodAlias methodAlias : methodAliases) {
                MethodReference ref = new MethodReference(methodAlias.getClassName(), methodAlias.getMethodName(),
                        MethodDescriptor.parseSignature(methodAlias.getDescriptor()));
                TeaVMEntryPoint entryPoint = vm.entryPoint(methodAlias.getAlias(), ref).async();
                if (methodAlias.getTypes() != null) {
                    for (int i = 0; i < methodAlias.getTypes().length; ++i) {
                        String types = methodAlias.getTypes()[i];
                        if (types != null) {
                            for (String type : types.split(" +")) {
                                type = type.trim();
                                if (!type.isEmpty()) {
                                    entryPoint.withValue(i, type);
                                }
                            }
                        }
                    }
                }
            }
            targetDirectory.mkdirs();
            try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(
                    new FileOutputStream(new File(targetDirectory, targetFileName))), "UTF-8")) {
                if (runtime == RuntimeCopyOperation.MERGED) {
                    vm.add(runtimeInjector);
                }
                vm.build(writer, new DirectoryBuildTarget(targetDirectory));
                if (vm.wasCancelled()) {
                    log.info("Build cancelled");
                    cancelled = true;
                    return;
                }
                if (mainClass != null) {
                    writer.append("main = $rt_mainWrapper(main);\n");
                }
                ProblemProvider problemProvider = vm.getProblemProvider();
                if (problemProvider.getProblems().isEmpty()) {
                    log.info("JavaScript file successfully built");
                } else if (problemProvider.getSevereProblems().isEmpty()) {
                    log.info("JavaScript file built with warnings");
                    TeaVMProblemRenderer.describeProblems(vm, log);
                } else {
                    log.info("JavaScript file built with errors");
                    TeaVMProblemRenderer.describeProblems(vm, log);
                }
                if (debugInformationGenerated) {
                    DebugInformation debugInfo = debugEmitter.getDebugInformation();
                    try (OutputStream debugInfoOut = new FileOutputStream(new File(targetDirectory,
                            targetFileName + ".teavmdbg"))) {
                        debugInfo.write(debugInfoOut);
                    }
                    log.info("Debug information successfully written");
                }
                if (sourceMapsFileGenerated) {
                    DebugInformation debugInfo = debugEmitter.getDebugInformation();
                    String sourceMapsFileName = targetFileName + ".map";
                    writer.append("\n//# sourceMappingURL=").append(sourceMapsFileName);
                    try (Writer sourceMapsOut = new OutputStreamWriter(new FileOutputStream(
                            new File(targetDirectory, sourceMapsFileName)), "UTF-8")) {
                        debugInfo.writeAsSourceMaps(sourceMapsOut, "src", targetFileName);
                    }
                    log.info("Source maps successfully written");
                }
                if (sourceFilesCopied) {
                    copySourceFiles();
                    log.info("Source files successfully written");
                }
                if (incremental) {
                    programCache.flush();
                    astCache.flush();
                    cachedClassSource.flush();
                    symbolTable.flush();
                    fileTable.flush();
                    log.info("Cache updated");
                }
            }
            if (runtime == RuntimeCopyOperation.SEPARATE) {
                resourceToFile("org/teavm/javascript/runtime.js", "runtime.js");
            }
            if (mainPageIncluded) {
                String text;
                try (Reader reader = new InputStreamReader(classLoader.getResourceAsStream(
                        "org/teavm/tooling/main.html"), "UTF-8")) {
                    text = IOUtils.toString(reader).replace("${classes.js}", targetFileName);
                }
                File mainPageFile = new File(targetDirectory, "main.html");
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(mainPageFile), "UTF-8")) {
                    writer.append(text);
                }
            }
        } catch (IOException e) {
            throw new TeaVMToolException("IO error occured", e);
        }
    }

    private void copySourceFiles() {
        if (vm.getWrittenClasses() == null) {
            return;
        }
        SourceFilesCopier copier = new SourceFilesCopier(sourceFileProviders);
        copier.addClasses(vm.getWrittenClasses());
        copier.setLog(log);
        copier.copy(new File(targetDirectory, "src"));
    }

    private AbstractRendererListener runtimeInjector = new AbstractRendererListener() {
        @Override
        public void begin(RenderingContext context, BuildTarget buildTarget) throws IOException {
            StringWriter writer = new StringWriter();
            resourceToWriter("org/teavm/javascript/runtime.js", writer);
            writer.close();
            context.getWriter().append(writer.toString()).newLine();
        }
    };

    private void resourceToFile(String resource, String fileName) throws IOException {
        try (InputStream input = TeaVMTool.class.getClassLoader().getResourceAsStream(resource)) {
            try (OutputStream output = new FileOutputStream(new File(targetDirectory, fileName))) {
                IOUtils.copy(input, output);
            }
        }
    }

    private void resourceToWriter(String resource, Writer writer) throws IOException {
        try (InputStream input = TeaVMTool.class.getClassLoader().getResourceAsStream(resource)) {
            IOUtils.copy(input, writer, "UTF-8");
        }
    }
}
