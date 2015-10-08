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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.teavm.common.FiniteExecutor;
import org.teavm.common.SimpleFiniteExecutor;
import org.teavm.common.ThreadPoolFiniteExecutor;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationBuilder;
import org.teavm.javascript.EmptyRegularMethodNodeCache;
import org.teavm.javascript.InMemoryRegularMethodNodeCache;
import org.teavm.javascript.MethodNodeCache;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.CopyClassHolderSource;
import org.teavm.model.InMemoryProgramCache;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.ProgramCache;
import org.teavm.model.ValueType;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.testing.JUnitTestAdapter;
import org.teavm.testing.TestAdapter;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;

/**
 *
 * @author Alexey Andreev
 */
public class TeaVMTestTool {
    private File outputDir = new File(".");
    private boolean minifying = true;
    private int numThreads = 1;
    private TestAdapter adapter = new JUnitTestAdapter();
    private List<ClassHolderTransformer> transformers = new ArrayList<>();
    private List<String> additionalScripts = new ArrayList<>();
    private List<String> additionalScriptLocalPaths = new ArrayList<>();
    private Properties properties = new Properties();
    private List<String> testClasses = new ArrayList<>();
    private ClassLoader classLoader = TeaVMTestTool.class.getClassLoader();
    private TeaVMToolLog log = new EmptyTeaVMToolLog();
    private boolean debugInformationGenerated;
    private boolean sourceMapsGenerated;
    private boolean sourceFilesCopied;
    private boolean incremental;
    private List<SourceFileProvider> sourceFileProviders = new ArrayList<>();
    private MethodNodeCache astCache;
    private ProgramCache programCache;
    private SourceFilesCopier sourceFilesCopier;
    private List<TeaVMTestToolListener> listeners = new ArrayList<>();
    private List<TeaVMTestClass> testPlan = new ArrayList<>();
    private int fileIndexGenerator;

    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isMinifying() {
        return minifying;
    }

    public void setMinifying(boolean minifying) {
        this.minifying = minifying;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public TestAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(TestAdapter adapter) {
        this.adapter = adapter;
    }

    public List<ClassHolderTransformer> getTransformers() {
        return transformers;
    }

    public List<String> getAdditionalScripts() {
        return additionalScripts;
    }

    public Properties getProperties() {
        return properties;
    }

    public List<String> getTestClasses() {
        return testClasses;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public TeaVMToolLog getLog() {
        return log;
    }

    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public boolean isDebugInformationGenerated() {
        return debugInformationGenerated;
    }

    public void setDebugInformationGenerated(boolean debugInformationGenerated) {
        this.debugInformationGenerated = debugInformationGenerated;
    }

    public boolean isSourceMapsGenerated() {
        return sourceMapsGenerated;
    }

    public void setSourceMapsGenerated(boolean sourceMapsGenerated) {
        this.sourceMapsGenerated = sourceMapsGenerated;
    }

    public boolean isSourceFilesCopied() {
        return sourceFilesCopied;
    }

    public void setSourceFilesCopied(boolean sourceFilesCopied) {
        this.sourceFilesCopied = sourceFilesCopied;
    }

    public void addSourceFileProvider(SourceFileProvider sourceFileProvider) {
        sourceFileProviders.add(sourceFileProvider);
    }

    public void generate() throws TeaVMToolException {
        Runnable finalizer = null;
        try {
            new File(outputDir, "tests").mkdirs();
            new File(outputDir, "res").mkdirs();
            resourceToFile("org/teavm/javascript/runtime.js", "res/runtime.js");
            String prefix = "org/teavm/tooling/test";
            resourceToFile(prefix + "/res/junit-support.js", "res/junit-support.js");
            resourceToFile(prefix + "/res/junit.css", "res/junit.css");
            resourceToFile(prefix + "/res/class_obj.png", "res/class_obj.png");
            resourceToFile(prefix + "/res/control-000-small.png", "res/control-000-small.png");
            resourceToFile(prefix + "/res/methpub_obj.png", "res/methpub_obj.png");
            resourceToFile(prefix + "/res/package_obj.png", "res/package_obj.png");
            resourceToFile(prefix + "/res/tick-small-red.png", "res/tick-small-red.png");
            resourceToFile(prefix + "/res/tick-small.png", "res/tick-small.png");
            resourceToFile(prefix + "/res/toggle-small-expand.png", "res/toggle-small-expand.png");
            resourceToFile(prefix + "/res/toggle-small.png", "res/toggle-small.png");
            resourceToFile(prefix + "/junit.html", "junit.html");
            ClassHolderSource classSource = new ClasspathClassHolderSource(classLoader);
            if (incremental) {
                classSource = new PreOptimizingClassHolderSource(classSource);
            }
            for (String testClass : testClasses) {
                ClassHolder classHolder = classSource.get(testClass);
                if (classHolder == null) {
                    throw new TeaVMToolException("Could not find class " + testClass);
                }
                findTests(classHolder);
            }

            includeAdditionalScripts(classLoader);
            astCache = new EmptyRegularMethodNodeCache();
            if (incremental) {
                astCache = new InMemoryRegularMethodNodeCache();
                programCache = new InMemoryProgramCache();
            }
            writeMetadata();

            FiniteExecutor executor = new SimpleFiniteExecutor();
            if (numThreads != 1) {
                int threads = numThreads != 0 ? numThreads : Runtime.getRuntime().availableProcessors();
                final ThreadPoolFiniteExecutor threadedExecutor = new ThreadPoolFiniteExecutor(threads);
                finalizer = () -> threadedExecutor.stop();
                executor = threadedExecutor;
            }
            int methodsGenerated = writeMethods(executor, classSource);

            if (sourceFilesCopied) {
                sourceFilesCopier.copy(new File(new File(outputDir, "tests"), "src"));
            }
            log.info("Test files successfully generated for " + methodsGenerated + " method(s).");
        } catch (IOException e) {
            throw new TeaVMToolException("IO error occured generating JavaScript files", e);
        } finally {
            if (finalizer != null) {
                finalizer.run();
            }
        }
    }

    private void writeMetadata() throws IOException {
        File allTestsFile = new File(outputDir, "tests/all.js");
        try (Writer allTestsWriter = new OutputStreamWriter(new FileOutputStream(allTestsFile), "UTF-8")) {
            allTestsWriter.write("prepare = function() {\n");
            allTestsWriter.write("    return new JUnitServer(document.body).readTests([");
            boolean first = true;
            for (TeaVMTestClass testClass : testPlan) {
                if (!first) {
                    allTestsWriter.append(",");
                }
                first = false;
                allTestsWriter.append("\n        { name : \"").append(testClass.getClassName())
                        .append("\", methods : [");
                boolean firstMethod = true;
                for (TeaVMTestMethod testMethod : testClass.getMethods()) {
                    String scriptName = testMethod.getFileName();
                    if (!firstMethod) {
                        allTestsWriter.append(",");
                    }
                    firstMethod = false;
                    allTestsWriter.append("\n            { name : \"" + testMethod.getMethod().getName()
                            + "\", script : \"" + scriptName + "\", expected : [");
                    boolean firstException = true;
                    for (String exception : testMethod.getExpectedExceptions()) {
                        if (!firstException) {
                            allTestsWriter.append(", ");
                        }
                        firstException = false;
                        allTestsWriter.append("\"" + exception + "\"");
                    }
                    allTestsWriter.append("], additionalScripts : [");
                    for (int i = 0; i < additionalScriptLocalPaths.size(); ++i) {
                        if (i > 0) {
                            allTestsWriter.append(", ");
                        }
                        escapeString(additionalScriptLocalPaths.get(i), allTestsWriter);
                    }
                    allTestsWriter.append("] }");
                }
                allTestsWriter.append("] }");
            }
            allTestsWriter.write("], function() {}); }");
        }
    }

    private int writeMethods(FiniteExecutor executor, ClassHolderSource classSource) {
        int methodsGenerated = 0;
        log.info("Generating test files");
        sourceFilesCopier = new SourceFilesCopier(sourceFileProviders);
        sourceFilesCopier.setLog(log);
        for (TeaVMTestClass testClass : testPlan) {
            for (TeaVMTestMethod testMethod : testClass.getMethods()) {
                final ClassHolderSource builderClassSource = classSource;
                executor.execute(() ->  {
                    log.debug("Building test for " + testMethod.getMethod());
                    try {
                        decompileClassesForTest(classLoader, new CopyClassHolderSource(builderClassSource),
                                testMethod);
                    } catch (IOException e) {
                        log.error("Error generating JavaScript", e);
                    }
                });
                ++methodsGenerated;
            }
        }
        executor.complete();
        return methodsGenerated;
    }

    private void resourceToFile(String resource, String fileName) throws IOException {
        try (InputStream input = TeaVMTestTool.class.getClassLoader().getResourceAsStream(resource)) {
            try (OutputStream output = new FileOutputStream(new File(outputDir, fileName))) {
                IOUtils.copy(input, output);
            }
        }
    }

    private void findTests(ClassHolder cls) {
        TeaVMTestClass testClass = new TeaVMTestClass(cls.getName());
        for (MethodHolder method : cls.getMethods()) {
            if (adapter.acceptMethod(method)) {
                MethodReference ref = new MethodReference(cls.getName(), method.getDescriptor());
                String fileName = "tests/" + fileIndexGenerator++ + ".js";

                List<String> exceptions = new ArrayList<>();
                for (String exception : adapter.getExpectedExceptions(method)) {
                    exceptions.add(exception);
                }

                TeaVMTestMethod testMethod = new TeaVMTestMethod(ref, fileName, exceptions);
                testClass.getMethods().add(testMethod);
            }
        }
        if (!testClass.getMethods().isEmpty()) {
            testPlan.add(testClass);
        }
    }

    private void includeAdditionalScripts(ClassLoader classLoader) throws TeaVMToolException {
        if (additionalScripts == null) {
            return;
        }
        for (String script : additionalScripts) {
            String simpleName = script.substring(script.lastIndexOf('/') + 1);
            additionalScriptLocalPaths.add("tests/" + simpleName);
            if (classLoader.getResource(script) == null) {
                throw new TeaVMToolException("Additional script " + script + " was not found");
            }
            File file = new File(outputDir, "tests/" + simpleName);
            try (InputStream in = classLoader.getResourceAsStream(script)) {
                if (!file.exists()) {
                    file.createNewFile();
                }
                try (OutputStream out = new FileOutputStream(file)) {
                    IOUtils.copy(in, out);
                }
            } catch (IOException e) {
                throw new TeaVMToolException("Error copying additional script " + script, e);
            }
        }
    }

    private void decompileClassesForTest(ClassLoader classLoader, ClassHolderSource classSource,
            TeaVMTestMethod testMethod) throws IOException {
        String targetName = testMethod.getFileName();
        TeaVM vm = new TeaVMBuilder()
                .setClassLoader(classLoader)
                .setClassSource(classSource)
                .build();
        vm.setIncremental(incremental);
        vm.setAstCache(astCache);
        vm.setProgramCache(programCache);
        vm.setProperties(properties);
        vm.setMinifying(minifying);
        vm.installPlugins();
        new TestExceptionPlugin().install(vm);
        for (ClassHolderTransformer transformer : transformers) {
            vm.add(transformer);
        }

        File file = new File(outputDir, testMethod.getFileName());
        DebugInformationBuilder debugInfoBuilder = sourceMapsGenerated || debugInformationGenerated
                ? new DebugInformationBuilder() : null;
        MethodReference methodRef = testMethod.getMethod();
        try (Writer innerWriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            MethodReference cons = new MethodReference(methodRef.getClassName(), "<init>", ValueType.VOID);
            MethodReference exceptionMsg = new MethodReference(ExceptionHelper.class, "showException",
                    Throwable.class, String.class);
            vm.entryPoint("initInstance", cons);
            vm.entryPoint("runTest", methodRef).withValue(0, cons.getClassName()).async();
            vm.entryPoint("extractException", exceptionMsg);
            vm.exportType("TestClass", cons.getClassName());
            vm.setDebugEmitter(debugInfoBuilder);
            vm.build(innerWriter, new DirectoryBuildTarget(outputDir));
            innerWriter.append("\n");
            innerWriter.append("\nJUnitClient.run();");
            if (sourceMapsGenerated) {
                String sourceMapsFileName = targetName.substring(targetName.lastIndexOf('/') + 1) + ".map";
                innerWriter.append("\n//# sourceMappingURL=").append(sourceMapsFileName);
            }
            if (!vm.getProblemProvider().getProblems().isEmpty()) {
                if (vm.getProblemProvider().getSevereProblems().isEmpty()) {
                    log.warning("Test built with warnings: " + methodRef);
                    TeaVMProblemRenderer.describeProblems(vm, log);
                } else {
                    log.warning("Test built with errors: " + methodRef);
                    TeaVMProblemRenderer.describeProblems(vm, log);
                }
            }
        }

        File debugTableFile = null;
        if (debugInformationGenerated) {
            DebugInformation debugInfo = debugInfoBuilder.getDebugInformation();
            debugTableFile = new File(outputDir, targetName + ".teavmdbg");
            try (OutputStream debugInfoOut = new FileOutputStream(debugTableFile)) {
                debugInfo.write(debugInfoOut);
            }
        }

        if (sourceMapsGenerated) {
            DebugInformation debugInfo = debugInfoBuilder.getDebugInformation();
            String sourceMapsFileName = targetName + ".map";
            try (Writer sourceMapsOut = new OutputStreamWriter(new FileOutputStream(
                    new File(outputDir, sourceMapsFileName)), "UTF-8")) {
                debugInfo.writeAsSourceMaps(sourceMapsOut, "src", targetName);
            }
        }
        if (sourceFilesCopied && vm.getWrittenClasses() != null) {
            sourceFilesCopier.addClasses(vm.getWrittenClasses());
        }

        TeaVMTestCase testCase = new TeaVMTestCase(methodRef, new File(outputDir, "res/runtime.js"),
                file, debugTableFile, testMethod.getExpectedExceptions());
        for (TeaVMTestToolListener listener : listeners) {
            listener.testGenerated(testCase);
        }
    }

    private void escapeString(String string, Writer writer) throws IOException {
        writer.append('\"');
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            switch (c) {
                case '"':
                    writer.append("\\\"");
                    break;
                case '\\':
                    writer.append("\\\\");
                    break;
                case '\n':
                    writer.append("\\n");
                    break;
                case '\r':
                    writer.append("\\r");
                    break;
                case '\t':
                    writer.append("\\t");
                    break;
                default:
                    writer.append(c);
                    break;
            }
        }
        writer.append('\"');
    }

    public void addListener(TeaVMTestToolListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TeaVMTestToolListener listener) {
        listeners.remove(listener);
    }
}
