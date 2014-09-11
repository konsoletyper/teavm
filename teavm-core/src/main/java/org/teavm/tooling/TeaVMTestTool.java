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
import org.teavm.common.FiniteExecutor;
import org.teavm.common.SimpleFiniteExecutor;
import org.teavm.common.ThreadPoolFiniteExecutor;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationBuilder;
import org.teavm.javascript.EmptyRegularMethodNodeCache;
import org.teavm.javascript.InMemoryRegularMethodNodeCache;
import org.teavm.javascript.RegularMethodNodeCache;
import org.teavm.model.*;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.testing.JUnitTestAdapter;
import org.teavm.testing.TestAdapter;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMTestTool {
    private Map<String, List<MethodReference>> groupedMethods = new HashMap<>();
    private Map<MethodReference, String> fileNames = new HashMap<>();
    private List<MethodReference> testMethods = new ArrayList<>();
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
    private boolean incremental;
    private RegularMethodNodeCache astCache;
    private ProgramCache programCache;

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
            File allTestsFile = new File(outputDir, "tests/all.js");
            try (Writer allTestsWriter = new OutputStreamWriter(new FileOutputStream(allTestsFile), "UTF-8")) {
                allTestsWriter.write("prepare = function() {\n");
                allTestsWriter.write("    return new JUnitServer(document.body).readTests([");
                boolean first = true;
                for (String testClass : testClasses) {
                    Collection<MethodReference> methods = groupedMethods.get(testClass);
                    if (methods == null) {
                        continue;
                    }
                    if (!first) {
                        allTestsWriter.append(",");
                    }
                    first = false;
                    allTestsWriter.append("\n        { name : \"").append(testClass).append("\", methods : [");
                    boolean firstMethod = true;
                    for (MethodReference methodRef : methods) {
                        String scriptName = "tests/" + fileNames.size() + ".js";
                        fileNames.put(methodRef, scriptName);
                        if (!firstMethod) {
                            allTestsWriter.append(",");
                        }
                        firstMethod = false;
                        allTestsWriter.append("\n            { name : \"" + methodRef.getName() + "\", script : \"" +
                                scriptName + "\", expected : [");
                        MethodHolder methodHolder = classSource.get(testClass).getMethod(
                                methodRef.getDescriptor());
                        boolean firstException = true;
                        for (String exception : adapter.getExpectedExceptions(methodHolder)) {
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
            int methodsGenerated = 0;
            log.info("Generating test files");
            FiniteExecutor executor = new SimpleFiniteExecutor();
            if (numThreads != 1) {
                int threads = numThreads != 0 ? numThreads : Runtime.getRuntime().availableProcessors();
                final ThreadPoolFiniteExecutor threadedExecutor = new ThreadPoolFiniteExecutor(threads);
                finalizer = new Runnable() {
                    @Override public void run() {
                        threadedExecutor.stop();
                    }
                };
                executor = threadedExecutor;
            }
            for (final MethodReference method : testMethods) {
                final ClassHolderSource builderClassSource = classSource;
                executor.execute(new Runnable() {
                    @Override public void run() {
                        log.debug("Building test for " + method);
                        try {
                            decompileClassesForTest(classLoader, new CopyClassHolderSource(builderClassSource), method,
                                    fileNames.get(method));
                        } catch (IOException e) {
                            log.error("Error generating JavaScript", e);
                        }
                    }
                });
                ++methodsGenerated;
            }
            executor.complete();
            log.info("Test files successfully generated for " + methodsGenerated + " method(s).");
        } catch (IOException e) {
            throw new TeaVMToolException("IO error occured generating JavaScript files", e);
        } finally {
            if (finalizer != null) {
                finalizer.run();
            }
        }
    }

    private void resourceToFile(String resource, String fileName) throws IOException {
        try (InputStream input = TeaVMTestTool.class.getClassLoader().getResourceAsStream(resource)) {
            try (OutputStream output = new FileOutputStream(new File(outputDir, fileName))) {
                IOUtils.copy(input, output);
            }
        }
    }

    private void findTests(ClassHolder cls) {
        for (MethodHolder method : cls.getMethods()) {
            if (adapter.acceptMethod(method)) {
                MethodReference ref = new MethodReference(cls.getName(), method.getDescriptor());
                testMethods.add(ref);
                List<MethodReference> group = groupedMethods.get(cls.getName());
                if (group == null) {
                    group = new ArrayList<>();
                    groupedMethods.put(cls.getName(), group);
                }
                group.add(ref);
            }
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
                try(OutputStream out = new FileOutputStream(file)) {
                    IOUtils.copy(in, out);
                }
            } catch (IOException e) {
                throw new TeaVMToolException("Error copying additional script " + script, e);
            }
        }
    }

    private void decompileClassesForTest(ClassLoader classLoader, ClassHolderSource classSource,
            MethodReference methodRef, String targetName) throws IOException {
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
        File file = new File(outputDir, targetName);
        DebugInformationBuilder debugInfoBuilder = sourceMapsGenerated || debugInformationGenerated ?
                new DebugInformationBuilder() : null;
        try (Writer innerWriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            MethodReference cons = new MethodReference(methodRef.getClassName(), "<init>", ValueType.VOID);
            MethodReference exceptionMsg = new MethodReference(ExceptionHelper.class, "showException",
                    Throwable.class, String.class);
            vm.entryPoint("initInstance", cons);
            vm.entryPoint("runTest", methodRef).withValue(0, cons.getClassName());
            vm.entryPoint("extractException", exceptionMsg);
            vm.exportType("TestClass", cons.getClassName());
            vm.setDebugEmitter(debugInfoBuilder);
            vm.build(innerWriter, new DirectoryBuildTarget(outputDir));
            if (!vm.hasMissingItems()) {
                innerWriter.append("\n");
                innerWriter.append("\nJUnitClient.run();");
                if (sourceMapsGenerated) {
                    String sourceMapsFileName = targetName + ".map";
                    innerWriter.append("\n//# sourceMappingURL=").append(sourceMapsFileName);
                }
            } else {
                innerWriter.append("JUnitClient.reportError(\n");
                StringBuilder sb = new StringBuilder();
                vm.showMissingItems(sb);
                escapeStringLiteral(sb.toString(), innerWriter);
                innerWriter.append(");");
                log.warning("Error building test " + methodRef);
                log.warning(sb.toString());
            }
        }
        if (sourceMapsGenerated) {
            DebugInformation debugInfo = debugInfoBuilder.getDebugInformation();
            try (OutputStream debugInfoOut = new FileOutputStream(new File(outputDir, targetName + ".teavmdbg"))) {
                debugInfo.write(debugInfoOut);
            }
            log.info("Debug information successfully written");
        }
        if (sourceMapsGenerated) {
            DebugInformation debugInfo = debugInfoBuilder.getDebugInformation();
            String sourceMapsFileName = targetName + ".map";
            try (Writer sourceMapsOut = new OutputStreamWriter(new FileOutputStream(
                    new File(outputDir, sourceMapsFileName)), "UTF-8")) {
                debugInfo.writeAsSourceMaps(sourceMapsOut, targetName);
            }
            log.info("Source maps successfully written");
        }
    }

    private void escapeStringLiteral(String text, Writer writer) throws IOException {
        int index = 0;
        while (true) {
            int next = text.indexOf('\n', index);
            if (next < 0) {
                break;
            }
            escapeString(text.substring(index, next + 1), writer);
            writer.append(" +\n");
            index = next + 1;
        }
        escapeString(text.substring(index), writer);
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
