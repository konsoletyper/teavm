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
package org.teavm.tooling.testing;

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
import java.util.concurrent.atomic.AtomicInteger;
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
import org.teavm.tooling.BaseTeaVMTool;
import org.teavm.tooling.EmptyTeaVMToolLog;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMToolException;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.tooling.sources.SourceFileProvider;
import org.teavm.tooling.sources.SourceFilesCopier;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;

/**
 *
 * @author Alexey Andreev
 */
public class TeaVMTestTool implements BaseTeaVMTool {
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
    private boolean sourceMapsFileGenerated;
    private boolean sourceFilesCopied;
    private boolean incremental;
    private List<SourceFileProvider> sourceFileProviders = new ArrayList<>();
    private MethodNodeCache astCache;
    private ProgramCache programCache;
    private SourceFilesCopier sourceFilesCopier;
    private List<TestClassBuilder> testPlan = new ArrayList<>();
    private int fileIndexGenerator;
    private long startTime;
    private int testCount;
    private AtomicInteger testsBuilt = new AtomicInteger();

    public File getOutputDir() {
        return outputDir;
    }

    @Override
    public void setTargetDirectory(File outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isMinifying() {
        return minifying;
    }

    @Override
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

    @Override
    public List<ClassHolderTransformer> getTransformers() {
        return transformers;
    }

    public List<String> getAdditionalScripts() {
        return additionalScripts;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    public List<String> getTestClasses() {
        return testClasses;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public TeaVMToolLog getLog() {
        return log;
    }

    @Override
    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    public boolean isIncremental() {
        return incremental;
    }

    @Override
    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public boolean isDebugInformationGenerated() {
        return debugInformationGenerated;
    }

    @Override
    public void setDebugInformationGenerated(boolean debugInformationGenerated) {
        this.debugInformationGenerated = debugInformationGenerated;
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
    public void addSourceFileProvider(SourceFileProvider sourceFileProvider) {
        sourceFileProviders.add(sourceFileProvider);
    }

    public TestPlan generate() throws TeaVMToolException {
        testsBuilt.set(0);
        Runnable finalizer = null;
        try {
            new File(outputDir, "tests").mkdirs();
            new File(outputDir, "res").mkdirs();
            resourceToFile("org/teavm/javascript/runtime.js", "res/runtime.js");
            String prefix = "org/teavm/tooling/test";
            resourceToFile(prefix + "/res/junit-support.js", "res/junit-support.js");
            resourceToFile(prefix + "/res/junit-client.js", "res/junit-client.js");
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
            resourceToFile(prefix + "/junit-client.html", "junit-client.html");
            ClassHolderSource classSource = new ClasspathClassHolderSource(classLoader);
            if (incremental) {
                classSource = new PreOptimizingClassHolderSource(classSource);
            }

            List<TestGroup> groups = new ArrayList<>();
            for (String testClass : testClasses) {
                ClassHolder classHolder = classSource.get(testClass);
                if (classHolder == null) {
                    throw new TeaVMToolException("Could not find class " + testClass);
                }
                TestGroup group = findTests(classHolder);
                if (group != null) {
                    groups.add(group);
                }
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
                ThreadPoolFiniteExecutor threadedExecutor = new ThreadPoolFiniteExecutor(threads);
                finalizer = () -> threadedExecutor.stop();
                executor = threadedExecutor;
            }
            startTime = System.currentTimeMillis();
            int methodsGenerated = writeMethods(executor, classSource);

            if (sourceFilesCopied) {
                sourceFilesCopier.copy(new File(new File(outputDir, "tests"), "src"));
            }
            long timeSpent = System.currentTimeMillis() - startTime;
            log.info("Test files successfully generated for " + methodsGenerated + " method(s) in "
                    + (timeSpent / 1000.0) + " seconds.");

            return new TestPlan("res/runtime.js", groups);
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
            for (TestClassBuilder testClass : testPlan) {
                if (!first) {
                    allTestsWriter.append(",");
                }
                first = false;
                allTestsWriter.append("\n        { name : \"").append(testClass.getClassName())
                        .append("\", methods : [");
                boolean firstMethod = true;
                for (TestMethodBuilder testMethod : testClass.getMethods()) {
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
        for (TestClassBuilder testClass : testPlan) {
            for (TestMethodBuilder testMethod : testClass.getMethods()) {
                executor.execute(() ->  {
                    log.debug("Building test for " + testMethod.getMethod());
                    try {
                        decompileClassesForTest(classLoader, new CopyClassHolderSource(classSource),
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

    private TestGroup findTests(ClassHolder cls) {
        List<TestCase> cases = new ArrayList<>();
        TestClassBuilder testClass = new TestClassBuilder(cls.getName());
        for (MethodHolder method : cls.getMethods()) {
            if (adapter.acceptMethod(method)) {
                MethodReference ref = new MethodReference(cls.getName(), method.getDescriptor());
                String fileName = "tests/" + fileIndexGenerator++ + ".js";

                List<String> exceptions = new ArrayList<>();
                for (String exception : adapter.getExpectedExceptions(method)) {
                    exceptions.add(exception);
                }

                TestMethodBuilder testMethod = new TestMethodBuilder(ref, fileName, exceptions);
                testClass.getMethods().add(testMethod);

                String debugTable = debugInformationGenerated ? testMethod.getFileName() + ".teavmdbg" : null;
                cases.add(new TestCase(ref.toString(), testMethod.getFileName(), debugTable,
                        testMethod.getExpectedExceptions()));
                ++testCount;
            }
        }
        if (!testClass.getMethods().isEmpty()) {
            testPlan.add(testClass);
            return new TestGroup(cls.getName(), cases);
        } else {
            return null;
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
            TestMethodBuilder testMethod) throws IOException {
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
        DebugInformationBuilder debugInfoBuilder = sourceMapsFileGenerated || debugInformationGenerated
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
            if (sourceMapsFileGenerated) {
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

        if (debugInformationGenerated) {
            DebugInformation debugInfo = debugInfoBuilder.getDebugInformation();
            File debugTableFile = new File(outputDir, targetName + ".teavmdbg");
            try (OutputStream debugInfoOut = new FileOutputStream(debugTableFile)) {
                debugInfo.write(debugInfoOut);
            }
        }

        if (sourceMapsFileGenerated) {
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

        incrementCounter();
    }

    private void incrementCounter() {
        int count = testsBuilt.incrementAndGet();
        if (count % 10 != 0) {
            return;
        }

        long timeSpent = System.currentTimeMillis() - startTime;

        getLog().info(count + " of " + testCount + " tests built in " + (timeSpent / 1000.0) + " seconds ("
                + String.format("%.2f", (double) count / timeSpent * 1000.0) + " tests per second avg.)");
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
}
