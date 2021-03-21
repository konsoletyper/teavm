/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.junit;

import static java.nio.charset.StandardCharsets.UTF_8;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.teavm.backend.c.CTarget;
import org.teavm.backend.c.generate.CNameProvider;
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.backend.wasm.WasmTarget;
import org.teavm.callgraph.CallGraph;
import org.teavm.debugging.information.DebugInformation;
import org.teavm.debugging.information.DebugInformationBuilder;
import org.teavm.dependency.DependencyAnalyzerFactory;
import org.teavm.dependency.FastDependencyAnalyzer;
import org.teavm.dependency.PreciseDependencyAnalyzer;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.diagnostics.Problem;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.ReferenceCache;
import org.teavm.model.ValueType;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.TeaVMTarget;

public class TeaVMTestRunner extends Runner implements Filterable {
    static final String JUNIT3_BASE_CLASS = "junit.framework.TestCase";
    static final MethodReference JUNIT3_BEFORE = new MethodReference(JUNIT3_BASE_CLASS, "setUp", ValueType.VOID);
    static final MethodReference JUNIT3_AFTER = new MethodReference(JUNIT3_BASE_CLASS, "tearDown", ValueType.VOID);
    static final String JUNIT4_TEST = "org.junit.Test";
    static final String JUNIT4_IGNORE = "org.junit.Ignore";
    static final String TESTNG_TEST = "org.testng.annotations.Test";
    static final String TESTNG_IGNORE = "org.testng.annotations.Ignore";
    static final String JUNIT4_BEFORE = "org.junit.Before";
    static final String TESTNG_BEFORE = "org.testng.annotations.BeforeMethod";
    static final String JUNIT4_AFTER = "org.junit.After";
    static final String TESTNG_AFTER = "org.testng.annotations.AfterMethod";
    static final String TESTNG_PROVIDER = "org.testng.annotations.DataProvider";
    private static final String PATH_PARAM = "teavm.junit.target";
    private static final String JS_RUNNER = "teavm.junit.js.runner";
    private static final String WASM_RUNNER = "teavm.junit.wasm.runner";
    private static final String THREAD_COUNT = "teavm.junit.threads";
    private static final String JS_ENABLED = "teavm.junit.js";
    static final String JS_DECODE_STACK = "teavm.junit.js.decodeStack";
    private static final String C_ENABLED = "teavm.junit.c";
    private static final String WASM_ENABLED = "teavm.junit.wasm";
    private static final String C_COMPILER = "teavm.junit.c.compiler";
    private static final String C_LINE_NUMBERS = "teavm.junit.c.lineNumbers";
    private static final String MINIFIED = "teavm.junit.minified";
    private static final String OPTIMIZED = "teavm.junit.optimized";
    private static final String FAST_ANALYSIS = "teavm.junit.fastAnalysis";

    private static final int stopTimeout = 15000;
    private Class<?> testClass;
    private boolean isWholeClassCompilation;
    private ClassHolderSource classSource;
    private ClassLoader classLoader;
    private Description suiteDescription;
    private static Map<ClassLoader, ClassHolderSource> classSources = new WeakHashMap<>();
    private File outputDir;
    private Map<Method, Description> descriptions = new HashMap<>();
    private static Map<RunKind, RunnerKindInfo> runners = new HashMap<>();
    private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private CountDownLatch latch;
    private List<Method> filteredChildren;
    private ReferenceCache referenceCache = new ReferenceCache();
    private boolean classCompilationOk;
    private List<TestRun> runsInCurrentClass = new ArrayList<>();

    static class RunnerKindInfo {
        volatile TestRunner runner;
        volatile TestRunStrategy strategy;
    }

    static {
        for (RunKind kind : RunKind.values()) {
            runners.put(kind, new RunnerKindInfo());
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (TeaVMTestRunner.class) {
                for (RunnerKindInfo info : runners.values()) {
                    if (info.runner != null) {
                        info.runner.stop();
                        info.runner.waitForCompletion();
                    }
                }
            }
        }));
    }

    public TeaVMTestRunner(Class<?> testClass) throws InitializationError {
        this.testClass = testClass;
        classLoader = TeaVMTestRunner.class.getClassLoader();
        classSource = getClassSource(classLoader);
        String outputPath = System.getProperty(PATH_PARAM);
        if (outputPath != null) {
            outputDir = new File(outputPath);
        }

        String runStrategyName = System.getProperty(JS_RUNNER);
        if (runStrategyName != null) {
            TestRunStrategy jsRunStrategy;
            switch (runStrategyName) {
                case "htmlunit":
                    jsRunStrategy = new HtmlUnitRunStrategy();
                    break;
                case "browser":
                    jsRunStrategy = new BrowserRunStrategy(outputDir, "JAVASCRIPT", this::customBrowser);
                    break;
                case "browser-chrome":
                    jsRunStrategy = new BrowserRunStrategy(outputDir, "JAVASCRIPT", this::chromeBrowser);
                    break;
                case "browser-firefox":
                    jsRunStrategy = new BrowserRunStrategy(outputDir, "JAVASCRIPT", this::firefoxBrowser);
                    break;
                case "none":
                    jsRunStrategy = null;
                    break;
                default:
                    throw new InitializationError("Unknown run strategy: " + runStrategyName);
            }
            runners.get(RunKind.JAVASCRIPT).strategy = jsRunStrategy;
        }

        String cCommand = System.getProperty(C_COMPILER);
        if (cCommand != null) {
            runners.get(RunKind.C).strategy = new CRunStrategy(cCommand);
        }

        runStrategyName = System.getProperty(WASM_RUNNER);
        if (runStrategyName != null) {
            TestRunStrategy wasmRunStrategy;
            switch (runStrategyName) {
                case "browser":
                    wasmRunStrategy = new BrowserRunStrategy(outputDir, "WASM", this::customBrowser);
                    break;
                case "browser-chrome":
                    wasmRunStrategy = new BrowserRunStrategy(outputDir, "WASM", this::chromeBrowser);
                    break;
                case "browser-firefox":
                    wasmRunStrategy = new BrowserRunStrategy(outputDir, "WASM", this::firefoxBrowser);
                    break;
                default:
                    throw new InitializationError("Unknown run strategy: " + runStrategyName);
            }
            runners.get(RunKind.WASM).strategy = wasmRunStrategy;
        }
    }

    private Process customBrowser(String url) {
        System.out.println("Open link to run tests: " + url + "?logging=true");
        return null;
    }

    private Process chromeBrowser(String url) {
        return browserTemplate("chrome", url, (profile, params) -> {
            params.addAll(Arrays.asList(
                    "google-chrome-stable",
                    "--headless",
                    "--disable-gpu",
                    "--remote-debugging-port=9222",
                    "--no-first-run",
                    "--user-data-dir=" + profile
            ));
        });
    }

    private Process firefoxBrowser(String url) {
        return browserTemplate("firefox", url, (profile, params) -> {
            params.addAll(Arrays.asList(
                    "firefox",
                    "--headless",
                    "--profile",
                    profile
            ));
        });
    }

    private Process browserTemplate(String name, String url, BiConsumer<String, List<String>> paramsBuilder) {
        File temp;
        try {
            temp = File.createTempFile("teavm", "teavm");
            temp.delete();
            temp.mkdirs();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteDir(temp)));
            System.out.println("Running " + name + " with user data dir: " + temp.getAbsolutePath());
            List<String> params = new ArrayList<>();
            paramsBuilder.accept(temp.getAbsolutePath(), params);
            int tabs = Integer.parseInt(System.getProperty(THREAD_COUNT, "1"));
            for (int i = 0; i < tabs; ++i) {
                params.add(url);
            }
            ProcessBuilder pb = new ProcessBuilder(params.toArray(new String[0]));
            Process process = pb.start();
            logStream(process.getInputStream(), name + " stdout");
            logStream(process.getErrorStream(), name + " stderr");
            new Thread(() -> {
                try {
                    System.out.println(name + " process terminated with code: " + process.waitFor());
                } catch (InterruptedException e) {
                    // ignore
                }
            });
            return process;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void logStream(InputStream stream, String name) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(name + ": " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteDir(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

    @Override
    public Description getDescription() {
        if (suiteDescription == null) {
            suiteDescription = Description.createSuiteDescription(testClass);
            for (Method child : getFilteredChildren()) {
                suiteDescription.addChild(describeChild(child));
            }
        }
        return suiteDescription;
    }

    @Override
    public void run(RunNotifier notifier) {
        List<Method> children = getFilteredChildren();
        latch = new CountDownLatch(children.size());

        notifier.fireTestStarted(getDescription());
        isWholeClassCompilation = testClass.isAnnotationPresent(WholeClassCompilation.class);
        if (isWholeClassCompilation) {
            classCompilationOk = compileWholeClass(children, notifier);
        }
        for (Method child : children) {
            runChild(child, notifier);
        }

        writeRunsDescriptor();
        runsInCurrentClass.clear();

        while (true) {
            try {
                if (latch.await(1000, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                break;
            }
        }
        notifier.fireTestFinished(getDescription());
    }

    private List<Method> getChildren() {
        List<Method> children = new ArrayList<>();
        Class<?> cls = testClass;
        Set<String> foundMethods = new HashSet<>();
        while (cls != Object.class && !cls.getName().equals(JUNIT3_BASE_CLASS)) {
            for (Method method : cls.getDeclaredMethods()) {
                if (foundMethods.add(method.getName()) && isTestMethod(method)) {
                    children.add(method);
                }
            }
            cls = cls.getSuperclass();
        }

        return children;
    }

    private boolean isTestMethod(Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }

        if (TestCase.class.isAssignableFrom(method.getDeclaringClass())) {
            return method.getName().startsWith("test") && method.getName().length() > 4
                    && Character.isUpperCase(method.getName().charAt(4));
        } else if (getClassAnnotation(method, TESTNG_TEST) != null) {
            return method.getName().startsWith("test_");
        } else {
            return getAnnotation(method, JUNIT4_TEST) != null || getAnnotation(method, TESTNG_TEST) != null;
        }
    }

    private List<Method> getFilteredChildren() {
        if (filteredChildren == null) {
            filteredChildren = getChildren();
        }
        return filteredChildren;
    }

    private Description describeChild(Method child) {
        return descriptions.computeIfAbsent(child, method -> Description.createTestDescription(testClass,
                method.getName()));
    }

    private boolean compileWholeClass(List<Method> children, RunNotifier notifier) {
        File outputPath = getOutputPathForClass();
        boolean hasErrors = false;
        Description description = getDescription();

        for (TeaVMTestConfiguration<JavaScriptTarget> configuration : getJavaScriptConfigurations()) {
            CompileResult result = compileToJs(wholeClass(children), "classTest", configuration, outputPath);
            if (!result.success) {
                hasErrors = true;
                notifier.fireTestFailure(createFailure(description, result));
            }
        }

        for (TeaVMTestConfiguration<CTarget> configuration : getCConfigurations()) {
            CompileResult result = compileToC(wholeClass(children), "classTest", configuration, outputPath);
            if (!result.success) {
                hasErrors = true;
                notifier.fireTestFailure(createFailure(description, result));
            }
        }

        for (TeaVMTestConfiguration<WasmTarget> configuration : getWasmConfigurations()) {
            CompileResult result = compileToWasm(wholeClass(children), "classTest", configuration, outputPath);
            if (!result.success) {
                hasErrors = true;
                notifier.fireTestFailure(createFailure(description, result));
            }
        }

        return !hasErrors;
    }

    private void runChild(Method child, RunNotifier notifier) {
        Description description = describeChild(child);
        notifier.fireTestStarted(description);

        if (isIgnored(child)) {
            notifier.fireTestIgnored(description);
            latch.countDown();
            return;
        }

        boolean ran = false;
        boolean success = true;

        if (!child.isAnnotationPresent(SkipJVM.class) && !testClass.isAnnotationPresent(SkipJVM.class)) {
            ran = true;
            ClassHolder classHolder = classSource.get(child.getDeclaringClass().getName());
            MethodHolder methodHolder = classHolder.getMethod(getDescriptor(child));
            success = runInJvm(child, notifier, getExpectedExceptions(methodHolder));
        }

        if (success && outputDir != null) {
            int[] configurationIndex = new int[] { 0 };

            List<TestRun> runs = new ArrayList<>();
            Consumer<Boolean> onSuccess = runSuccess -> {
                if (runSuccess && configurationIndex[0] < runs.size()) {
                    submitRun(runs.get(configurationIndex[0]++));
                } else {
                    notifier.fireTestFinished(description);
                    latch.countDown();
                }
            };

            if (isWholeClassCompilation) {
                if (!classCompilationOk) {
                    notifier.fireTestFinished(description);
                    notifier.fireTestFailure(new Failure(description,
                            new AssertionError("Could not compile test class")));
                    latch.countDown();
                } else {
                    runTestsFromWholeClass(child, notifier, runs, onSuccess);
                    onSuccess.accept(true);
                }
            } else {
                runCompiledTest(child, notifier, runs, onSuccess);
            }
        } else {
            if (!ran) {
                notifier.fireTestIgnored(description);
            }
            notifier.fireTestFinished(description);
            latch.countDown();
        }
    }

    private void runTestsFromWholeClass(Method child, RunNotifier notifier, List<TestRun> runs,
            Consumer<Boolean> onSuccess) {
        File outputPath = getOutputPathForClass();
        File outputPathForMethod = getOutputPath(child);
        MethodDescriptor descriptor = getDescriptor(child);
        MethodReference reference = new MethodReference(child.getDeclaringClass().getName(), descriptor);

        File testFilePath = getOutputPath(child);
        testFilePath.mkdirs();

        Map<String, String> properties = new HashMap<>();
        for (TeaVMTestConfiguration<JavaScriptTarget> configuration : getJavaScriptConfigurations()) {
            File testPath = getOutputFile(outputPath, "classTest", configuration.getSuffix(), false, ".js");
            runs.add(createTestRun(configuration, testPath, child, RunKind.JAVASCRIPT, reference.toString(),
                    notifier, onSuccess));
            File htmlPath = getOutputFile(outputPathForMethod, "test", configuration.getSuffix(), false, ".html");
            properties.put("SCRIPT", "../" + testPath.getName());
            properties.put("IDENTIFIER", reference.toString());
            try {
                resourceToFile("teavm-run-test.html", htmlPath, properties);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (TeaVMTestConfiguration<WasmTarget> configuration : getWasmConfigurations()) {
            File testPath = getOutputFile(outputPath, "classTest", configuration.getSuffix(), false, ".wasm");
            runs.add(createTestRun(configuration, testPath, child, RunKind.WASM, reference.toString(),
                    notifier, onSuccess));
            File htmlPath = getOutputFile(outputPathForMethod, "test-wasm", configuration.getSuffix(), false, ".html");
            properties.put("SCRIPT", "../" + testPath.getName());
            properties.put("IDENTIFIER", reference.toString());
            try {
                resourceToFile("teavm-run-test-wasm.html", htmlPath, properties);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (TeaVMTestConfiguration<CTarget> configuration : getCConfigurations()) {
            File testPath = getOutputFile(outputPath, "classTest", configuration.getSuffix(), true, ".c");
            runs.add(createTestRun(configuration, testPath, child, RunKind.C, reference.toString(),
                    notifier, onSuccess));
        }
    }

    private void runCompiledTest(Method child, RunNotifier notifier, List<TestRun> runs, Consumer<Boolean> onSuccess) {
        try {
            File outputPath = getOutputPath(child);

            Map<String, String> properties = new HashMap<>();
            for (TeaVMTestConfiguration<JavaScriptTarget> configuration : getJavaScriptConfigurations()) {
                CompileResult compileResult = compileToJs(singleTest(child), "test", configuration, outputPath);
                TestRun run = prepareRun(configuration, child, compileResult, notifier, RunKind.JAVASCRIPT, onSuccess);
                if (run != null) {
                    runs.add(run);

                    File testPath = getOutputFile(outputPath, "test", configuration.getSuffix(), false, ".js");
                    File htmlPath = getOutputFile(outputPath, "test", configuration.getSuffix(), false, ".html");
                    properties.put("SCRIPT", testPath.getName());
                    properties.put("IDENTIFIER", "");

                    try {
                        resourceToFile("teavm-run-test.html", htmlPath, properties);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            for (TeaVMTestConfiguration<CTarget> configuration : getCConfigurations()) {
                CompileResult compileResult = compileToC(singleTest(child), "test", configuration, outputPath);
                TestRun run = prepareRun(configuration, child, compileResult, notifier, RunKind.C, onSuccess);
                if (run != null) {
                    runs.add(run);
                }
            }

            for (TeaVMTestConfiguration<WasmTarget> configuration : getWasmConfigurations()) {
                CompileResult compileResult = compileToWasm(singleTest(child), "test", configuration,
                        outputPath);
                TestRun run = prepareRun(configuration, child, compileResult, notifier, RunKind.WASM, onSuccess);
                if (run != null) {
                    runs.add(run);

                    File testPath = getOutputFile(outputPath, "test", configuration.getSuffix(), false, ".wasm");
                    File htmlPath = getOutputFile(outputPath, "test", configuration.getSuffix(), false, ".html");
                    properties.put("SCRIPT", testPath.getName());
                    properties.put("IDENTIFIER", "");

                    try {
                        resourceToFile("teavm-run-test-wasm.html", htmlPath, properties);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(describeChild(child), e));
            notifier.fireTestFinished(describeChild(child));
            latch.countDown();
            return;
        }

        onSuccess.accept(true);
    }

    static String[] getExpectedExceptions(MethodReader method) {
        AnnotationReader annot = method.getAnnotations().get(JUNIT4_TEST);
        if (annot != null) {
            AnnotationValue expected = annot.getValue("expected");
            if (expected == null) {
                return new String[0];
            }

            ValueType result = expected.getJavaClass();
            return new String[] { ((ValueType.Object) result).getClassName() };
        }

        annot = method.getAnnotations().get(TESTNG_TEST);
        if (annot != null) {
            AnnotationValue expected = annot.getValue("expectedExceptions");
            if (expected == null) {
                return new String[0];
            }

            List<AnnotationValue> list = expected.getList();
            String[] result = new String[list.size()];
            for (int i = 0; i < list.size(); ++i) {
                result[i] = ((ValueType.Object) list.get(i).getJavaClass()).getClassName();
            }
            return result;
        }

        return new String[0];
    }

    private boolean runInJvm(Method testMethod, RunNotifier notifier, String[] expectedExceptions) {
        Description description = describeChild(testMethod);
        Object instance;
        try {
            instance = testClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            notifier.fireTestFailure(new Failure(description, e));
            return false;
        } catch (InvocationTargetException e) {
            notifier.fireTestFailure(new Failure(description, e.getTargetException()));
            return false;
        }

        Runner runner;
        try {
            runner = prepareJvmRunner(instance, testMethod, expectedExceptions);
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(description, e));
            return false;
        }

        try {
            runner.run(new Object[0]);
            return true;
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(description, e));
            return false;
        }
    }

    private Runner prepareJvmRunner(Object instance, Method testMethod, String[] expectedExceptions) throws Throwable {
        Runner runner;
        if (TestCase.class.isAssignableFrom(testClass)) {
            runner = new JUnit3Runner((TestCase) instance, testMethod);
        } else {
            runner = new SimpleMethodRunner(instance, testMethod);
        }

        if (expectedExceptions.length > 0) {
            runner = new WithExpectedExceptionRunner(runner, expectedExceptions);
        }

        runner = wrapWithBeforeAndAfter(runner, instance);
        runner = wrapWithDataProvider(runner, instance, testMethod);

        return runner;
    }

    private Runner wrapWithBeforeAndAfter(Runner runner, Object instance) {
        List<Class<?>> classes = new ArrayList<>();
        Class<?> cls = instance.getClass();
        while (cls != null) {
            classes.add(cls);
            cls = cls.getSuperclass();
        }

        List<Method> afterMethods = new ArrayList<>();
        for (Class<?> c : classes) {
            for (Method method : c.getMethods()) {
                if (getAnnotation(method, JUNIT4_AFTER) != null || getAnnotation(method, TESTNG_AFTER) != null) {
                    afterMethods.add(method);
                }
            }
        }

        List<Method> beforeMethods = new ArrayList<>();
        Collections.reverse(classes);
        for (Class<?> c : classes) {
            for (Method method : c.getMethods()) {
                if (getAnnotation(method, JUNIT4_BEFORE) != null || getAnnotation(method, TESTNG_BEFORE) != null) {
                    beforeMethods.add(method);
                }
            }
        }

        if (beforeMethods.isEmpty() && afterMethods.isEmpty()) {
            return runner;
        }

        return new WithBeforeAndAfterRunner(runner, instance, beforeMethods.toArray(new Method[0]),
                afterMethods.toArray(new Method[0]));
    }

    private Runner wrapWithDataProvider(Runner runner, Object instance, Method testMethod) throws Throwable {
        AnnotationHolder annot = getAnnotation(testMethod, TESTNG_TEST);
        if (annot == null) {
            return runner;
        }

        AnnotationValue dataProviderValue = annot.getValue("dataProvider");
        if (dataProviderValue == null) {
            return runner;
        }
        String providerName = dataProviderValue.getString();
        if (providerName.isEmpty()) {
            return runner;
        }

        Method provider = null;
        for (Method method : testMethod.getDeclaringClass().getDeclaredMethods()) {
            AnnotationHolder providerAnnot = getAnnotation(method, TESTNG_PROVIDER);
            if (providerAnnot != null && providerAnnot.getValue("name").getString().equals(providerName)) {
                provider = method;
                break;
            }
        }

        Object data;
        try {
            provider.setAccessible(true);
            data = provider.invoke(instance);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

        return new WithDataProviderRunner(runner, data, testMethod.getParameterTypes());
    }

    interface Runner {
        void run(Object[] arguments) throws Throwable;
    }

    static class SimpleMethodRunner implements Runner {
        Object instance;
        Method testMethod;

        SimpleMethodRunner(Object instance, Method testMethod) {
            this.instance = instance;
            this.testMethod = testMethod;
        }

        @Override
        public void run(Object[] arguments) throws Throwable {
            try {
                testMethod.invoke(instance, arguments);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }

    static class JUnit3Runner implements Runner {
        TestCase instance;
        Method testMethod;

        JUnit3Runner(TestCase instance, Method testMethod) {
            this.instance = instance;
            this.testMethod = testMethod;
        }

        @Override
        public void run(Object[] arguments) throws Throwable {
            instance.setName(testMethod.getName());
            instance.runBare();
        }
    }

    static class WithDataProviderRunner implements Runner {
        Runner underlyingRunner;
        Object data;
        Class<?>[] types;

        WithDataProviderRunner(Runner underlyingRunner, Object data, Class<?>[] types) {
            this.underlyingRunner = underlyingRunner;
            this.data = data;
            this.types = types;
        }

        @Override
        public void run(Object[] arguments) throws Throwable {
            if (arguments.length > 0) {
                throw new IllegalArgumentException("Expected 0 arguments");
            }
            if (data instanceof Iterator) {
                runWithIteratorData((Iterator<?>) data);
            } else {
                runWithArrayData((Object[][]) data);
            }
        }

        private void runWithArrayData(Object[][] data) throws Throwable {
            for (int i = 0; i < data.length; ++i) {
                runWithDataRow(data[i]);
            }
        }

        private void runWithIteratorData(Iterator<?> data) throws Throwable {
            while (data.hasNext()) {
                runWithDataRow((Object[]) data.next());
            }
        }

        private void runWithDataRow(Object[] dataRow) throws Throwable {
            Object[] args = dataRow.clone();
            for (int j = 0; j < args.length; ++j) {
                args[j] = convert(args[j], types[j]);
            }
            underlyingRunner.run(args);
        }

        private Object convert(Object value, Class<?> type) {
            if (type == byte.class) {
                value = ((Number) value).byteValue();
            } else if (type == short.class) {
                value = ((Number) value).shortValue();
            } else if (type == int.class) {
                value = ((Number) value).intValue();
            } else if (type == long.class) {
                value = ((Number) value).longValue();
            } else if (type == float.class) {
                value = ((Number) value).floatValue();
            } else if (type == double.class) {
                value = ((Number) value).doubleValue();
            }
            return value;
        }
    }

    static class WithExpectedExceptionRunner implements Runner {
        private Runner underlyingRunner;
        private String[] expectedExceptions;

        WithExpectedExceptionRunner(Runner underlyingRunner, String[] expectedExceptions) {
            this.underlyingRunner = underlyingRunner;
            this.expectedExceptions = expectedExceptions;
        }

        @Override
        public void run(Object[] arguments) throws Throwable {
            boolean caught = false;
            try {
                underlyingRunner.run(arguments);
            } catch (Exception e) {
                for (String expected : expectedExceptions) {
                    if (isSubtype(e.getClass(), expected)) {
                        caught = true;
                        break;
                    }
                }
                if (!caught) {
                    throw e;
                }
            }
            if (!caught) {
                throw new AssertionError("Expected exception not thrown");
            }
        }

        private boolean isSubtype(Class<?> cls, String superType) {
            while (cls != Throwable.class) {
                if (cls.getName().equals(superType)) {
                    return true;
                }
                cls = cls.getSuperclass();
            }
            return false;
        }
    }

    static class WithBeforeAndAfterRunner implements Runner {
        private Runner underlyingRunner;
        private Object instance;
        private Method[] beforeMethods;
        private Method[] afterMethods;

        WithBeforeAndAfterRunner(Runner underlyingRunner, Object instance, Method[] beforeMethods,
                Method[] afterMethods) {
            this.underlyingRunner = underlyingRunner;
            this.instance = instance;
            this.beforeMethods = beforeMethods;
            this.afterMethods = afterMethods;
        }

        @Override
        public void run(Object[] arguments) throws Throwable {
            for (Method method : beforeMethods) {
                try {
                    method.invoke(instance);
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            }
            try {
                underlyingRunner.run(arguments);
            } finally {
                for (Method method : afterMethods) {
                    method.invoke(instance);
                }
            }
        }
    }

    private TestRun prepareRun(TeaVMTestConfiguration<?> configuration, Method child, CompileResult result,
            RunNotifier notifier, RunKind kind, Consumer<Boolean> onComplete) {
        Description description = describeChild(child);

        if (!result.success) {
            notifier.fireTestFailure(createFailure(description, result));
            notifier.fireTestFinished(description);
            latch.countDown();
            return null;
        }

        return createTestRun(configuration, result.file, child, kind, null, notifier, onComplete);
    }

    private TestRun createTestRun(TeaVMTestConfiguration<?> configuration, File file, Method child, RunKind kind,
            String argument, RunNotifier notifier, Consumer<Boolean> onComplete) {
        Description description = describeChild(child);

        TestRunCallback callback = new TestRunCallback() {
            @Override
            public void complete() {
                onComplete.accept(true);
            }

            @Override
            public void error(Throwable e) {
                notifier.fireTestFailure(new Failure(description, e));
                onComplete.accept(false);
            }
        };

        return new TestRun(generateName(child.getName(), configuration), file.getParentFile(), child, description,
                file.getName(), kind, argument, callback);
    }

    private String generateName(String baseName, TeaVMTestConfiguration<?> configuration) {
        String suffix = configuration.getSuffix();
        if (!suffix.isEmpty()) {
            baseName = baseName + " (" + suffix + ")";
        }
        return baseName;
    }

    private Failure createFailure(Description description, CompileResult result) {
        Throwable throwable = result.throwable;
        if (throwable == null) {
            throwable = new AssertionError(result.errorMessage);
        }
        return new Failure(description, throwable);
    }

    private void submitRun(TestRun run) {
        synchronized (TeaVMTestRunner.class) {
            runsInCurrentClass.add(run);
            RunnerKindInfo info = runners.get(run.getKind());

            if (info.strategy == null) {
                run.getCallback().complete();
                return;
            }

            if (info.runner == null) {
                info.runner = new TestRunner(info.strategy);
                try {
                    info.runner.setNumThreads(Integer.parseInt(System.getProperty(THREAD_COUNT, "1")));
                } catch (NumberFormatException e) {
                    info.runner.setNumThreads(1);
                }
                info.runner.init();
            }
            info.runner.run(run);

            RunKind kind = run.getKind();
        }
    }

    private static void cleanupRunner(RunKind kind) {
        synchronized (TeaVMTestRunner.class) {
            RunnerKindInfo info = runners.get(kind);
            info.runner.stop();
            info.runner = null;
        }
    }

    private File getOutputPath(Method method) {
        File path = outputDir;
        path = new File(path, testClass.getName().replace('.', '/'));
        path = new File(path, method.getName());
        path.mkdirs();
        return path;
    }

    private File getOutputPathForClass() {
        File path = outputDir;
        path = new File(path, testClass.getName().replace('.', '/'));
        path.mkdirs();
        return path;
    }

    private CompileResult compileToJs(Consumer<TeaVM> additionalProcessing, String baseName,
            TeaVMTestConfiguration<JavaScriptTarget> configuration, File path) {
        boolean decodeStack = Boolean.parseBoolean(System.getProperty(JS_DECODE_STACK, "true"));
        DebugInformationBuilder debugEmitter = new DebugInformationBuilder(new ReferenceCache());
        Supplier<JavaScriptTarget> targetSupplier = () -> {
            JavaScriptTarget target = new JavaScriptTarget();
            target.setStrict(true);
            if (decodeStack) {
                target.setDebugEmitter(debugEmitter);
                target.setStackTraceIncluded(true);
            }
            return target;
        };
        CompilePostProcessor postBuild = null;
        if (decodeStack) {
            postBuild = (vm, file) -> {
                DebugInformation debugInfo = debugEmitter.getDebugInformation();
                File sourceMapsFile = new File(file.getPath() + ".map");
                File debugFile = new File(file.getPath() + ".teavmdbg");
                try {
                    try (Writer writer = new OutputStreamWriter(new FileOutputStream(file, true), UTF_8)) {
                        writer.write("\n//# sourceMappingURL=");
                        writer.write(sourceMapsFile.getName());
                    }

                    try (Writer sourceMapsOut = new OutputStreamWriter(new FileOutputStream(sourceMapsFile), UTF_8)) {
                        debugInfo.writeAsSourceMaps(sourceMapsOut, "", file.getPath());
                    }

                    try (OutputStream out = new FileOutputStream(debugFile)) {
                        debugInfo.write(out);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
        }
        return compile(configuration, targetSupplier, TestJsEntryPoint.class.getName(), path, ".js",
                postBuild, false, additionalProcessing, baseName);
    }

    private CompileResult compileToC(Consumer<TeaVM> additionalProcessing, String baseName,
            TeaVMTestConfiguration<CTarget> configuration, File path) {
        CompilePostProcessor postBuild = (vm, file) -> {
            try {
                resourceToFile("teavm-CMakeLists.txt", new File(file.getParent(), "CMakeLists.txt"),
                        Collections.emptyMap());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        return compile(configuration, this::createCTarget, TestNativeEntryPoint.class.getName(), path, ".c",
                postBuild, true, additionalProcessing, baseName);
    }

    private CTarget createCTarget() {
        CTarget cTarget = new CTarget(new CNameProvider());
        cTarget.setLineNumbersGenerated(Boolean.parseBoolean(System.getProperty(C_LINE_NUMBERS, "false")));
        return cTarget;
    }

    private CompileResult compileToWasm(Consumer<TeaVM> additionalProcessing, String baseName,
            TeaVMTestConfiguration<WasmTarget> configuration, File path) {
        return compile(configuration, WasmTarget::new, TestNativeEntryPoint.class.getName(), path,
                ".wasm", null, false, additionalProcessing, baseName);
    }

    private Consumer<TeaVM> singleTest(Method method) {
        ClassHolder classHolder = classSource.get(method.getDeclaringClass().getName());
        MethodHolder methodHolder = classHolder.getMethod(getDescriptor(method));

        return vm -> {
            Properties properties = new Properties();
            applyProperties(method.getDeclaringClass(), properties);
            vm.setProperties(properties);
            new TestEntryPointTransformerForSingleMethod(methodHolder.getReference(), testClass.getName()).install(vm);
        };
    }

    private Consumer<TeaVM> wholeClass(List<Method> methods) {
        return vm -> {
            Properties properties = new Properties();
            applyProperties(testClass, properties);
            vm.setProperties(properties);
            List<MethodReference> methodReferences = new ArrayList<>();
            for (Method method : methods) {
                if (isIgnored(method)) {
                    continue;
                }
                ClassHolder classHolder = classSource.get(method.getDeclaringClass().getName());
                MethodHolder methodHolder = classHolder.getMethod(getDescriptor(method));
                methodReferences.add(methodHolder.getReference());
            }
            new TestEntryPointTransformerForWholeClass(methodReferences, testClass.getName()).install(vm);
        };
    }

    private boolean isIgnored(Method method) {
        return getAnnotation(method, JUNIT4_IGNORE) != null
                || getAnnotation(method, TESTNG_IGNORE) != null
                || getClassAnnotation(method, JUNIT4_IGNORE) != null
                || getClassAnnotation(method, TESTNG_IGNORE) != null;
    }

    private AnnotationHolder getAnnotation(Method method, String name) {
        ClassHolder cls = classSource.get(method.getDeclaringClass().getName());
        if (cls == null) {
            return null;
        }
        MethodDescriptor descriptor = getDescriptor(method);
        MethodHolder methodHolder = cls.getMethod(descriptor);
        if (methodHolder == null) {
            return null;
        }
        return methodHolder.getAnnotations().get(name);
    }

    private AnnotationHolder getClassAnnotation(Method method, String name) {
        ClassHolder cls = classSource.get(method.getDeclaringClass().getName());
        if (cls == null) {
            return null;
        }
        return cls.getAnnotations().get(name);
    }


    private <T extends TeaVMTarget> CompileResult compile(TeaVMTestConfiguration<T> configuration,
            Supplier<T> targetSupplier, String entryPoint, File path, String extension,
            CompilePostProcessor postBuild, boolean separateDir,
            Consumer<TeaVM> additionalProcessing, String baseName) {
        CompileResult result = new CompileResult();

        File outputFile = getOutputFile(path, baseName, configuration.getSuffix(), separateDir, extension);
        result.file = outputFile;

        ClassLoader classLoader = TeaVMTestRunner.class.getClassLoader();

        T target = targetSupplier.get();
        configuration.apply(target);

        DependencyAnalyzerFactory dependencyAnalyzerFactory = PreciseDependencyAnalyzer::new;
        boolean fastAnalysis = Boolean.parseBoolean(System.getProperty(FAST_ANALYSIS));
        if (fastAnalysis) {
            dependencyAnalyzerFactory = FastDependencyAnalyzer::new;
        }

        try {
            TeaVM vm = new TeaVMBuilder(target)
                    .setClassLoader(classLoader)
                    .setClassSource(classSource)
                    .setReferenceCache(referenceCache)
                    .setDependencyAnalyzerFactory(dependencyAnalyzerFactory)
                    .build();

            configuration.apply(vm);
            additionalProcessing.accept(vm);
            vm.installPlugins();

            new TestExceptionPlugin().install(vm);

            vm.entryPoint(entryPoint);

            if (fastAnalysis) {
                vm.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE);
                vm.addVirtualMethods(m -> true);
            }
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            vm.build(new DirectoryBuildTarget(outputFile.getParentFile()), outputFile.getName());
            if (!vm.getProblemProvider().getProblems().isEmpty()) {
                result.success = false;
                result.errorMessage = buildErrorMessage(vm);
            } else {
                if (postBuild != null) {
                    postBuild.process(vm, outputFile);
                }
            }

            return result;
        } catch (Exception e) {
            result = new CompileResult();
            result.success = false;
            result.throwable = e;
            return result;
        }
    }

    private File getOutputFile(File path, String baseName, String suffix, boolean separateDir, String extension) {
        StringBuilder simpleName = new StringBuilder();
        simpleName.append(baseName);
        if (!suffix.isEmpty()) {
            if (!separateDir) {
                simpleName.append('-').append(suffix);
            }
        }
        File outputFile;
        if (separateDir) {
            outputFile = new File(new File(path, simpleName.toString()), "test" + extension);
        } else {
            simpleName.append(extension);
            outputFile = new File(path, simpleName.toString());
        }

        return outputFile;
    }

    interface CompilePostProcessor {
        void process(TeaVM vm, File targetFile);
    }

    private List<TeaVMTestConfiguration<JavaScriptTarget>> getJavaScriptConfigurations() {
        List<TeaVMTestConfiguration<JavaScriptTarget>> configurations = new ArrayList<>();
        if (Boolean.parseBoolean(System.getProperty(JS_ENABLED, "true"))) {
            configurations.add(TeaVMTestConfiguration.JS_DEFAULT);
            if (Boolean.getBoolean(MINIFIED)) {
                configurations.add(TeaVMTestConfiguration.JS_MINIFIED);
            }
            if (Boolean.getBoolean(OPTIMIZED)) {
                configurations.add(TeaVMTestConfiguration.JS_OPTIMIZED);
            }
        }
        return configurations;
    }

    private List<TeaVMTestConfiguration<WasmTarget>> getWasmConfigurations() {
        List<TeaVMTestConfiguration<WasmTarget>> configurations = new ArrayList<>();
        if (Boolean.getBoolean(WASM_ENABLED)) {
            configurations.add(TeaVMTestConfiguration.WASM_DEFAULT);
            if (Boolean.getBoolean(OPTIMIZED)) {
                configurations.add(TeaVMTestConfiguration.WASM_OPTIMIZED);
            }
        }
        return configurations;
    }

    private List<TeaVMTestConfiguration<CTarget>> getCConfigurations() {
        List<TeaVMTestConfiguration<CTarget>> configurations = new ArrayList<>();
        if (Boolean.getBoolean(C_ENABLED)) {
            configurations.add(TeaVMTestConfiguration.C_DEFAULT);
            if (Boolean.getBoolean(OPTIMIZED)) {
                configurations.add(TeaVMTestConfiguration.C_OPTIMIZED);
            }
        }
        return configurations;
    }

    private void applyProperties(Class<?> cls, Properties result) {
        if (cls.getSuperclass() != null) {
            applyProperties(cls.getSuperclass(), result);
        }
        TeaVMProperties properties = cls.getAnnotation(TeaVMProperties.class);
        if (properties != null) {
            for (TeaVMProperty property : properties.value()) {
                result.setProperty(property.key(), property.value());
            }
        }
    }

    private MethodDescriptor getDescriptor(Method method) {
        ValueType[] signature = Stream.concat(Arrays.stream(method.getParameterTypes()).map(ValueType::parse),
                Stream.of(ValueType.parse(method.getReturnType())))
                .toArray(ValueType[]::new);
        return new MethodDescriptor(method.getName(), signature);
    }

    private String buildErrorMessage(TeaVM vm) {
        CallGraph cg = vm.getDependencyInfo().getCallGraph();
        DefaultProblemTextConsumer consumer = new DefaultProblemTextConsumer();
        StringBuilder sb = new StringBuilder();
        for (Problem problem : vm.getProblemProvider().getProblems()) {
            consumer.clear();
            problem.render(consumer);
            sb.append(consumer.getText());
            TeaVMProblemRenderer.renderCallStack(cg, problem.getLocation(), sb);
            sb.append("\n");
        }
        return sb.toString();
    }

    private void resourceToFile(String resource, File file, Map<String, String> properties) throws IOException {
        if (properties.isEmpty()) {
            try (InputStream input = TeaVMTestRunner.class.getClassLoader().getResourceAsStream(resource);
                    OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
                IOUtils.copy(input, output);
            }
        } else {
            String content;
            try (InputStream input = TeaVMTestRunner.class.getClassLoader().getResourceAsStream(resource)) {
                content = IOUtils.toString(input, UTF_8);
            }
            content = replaceProperties(content, properties);
            try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file));
                    Writer writer = new OutputStreamWriter(output)) {
                 writer.write(content);
            }
        }
    }

    private static String replaceProperties(String s, Map<String, String> properties) {
        int i = 0;
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            int next = s.indexOf("${", i);
            if (next < 0) {
                break;
            }
            int end = s.indexOf('}', next + 2);
            if (end < 0) {
                break;
            }

            sb.append(s, i, next);
            String property = s.substring(next + 2, end);
            String value = properties.get(property);
            if (value == null) {
                sb.append(s, next, end + 1);
            } else {
                sb.append(value);
            }
            i = end + 1;
        }

        if (i == 0) {
            return s;
        }

        return sb.append(s.substring(i)).toString();
    }

    private ClassHolderSource getClassSource(ClassLoader classLoader) {
        return classSources.computeIfAbsent(classLoader, cl -> new PreOptimizingClassHolderSource(
                new ClasspathClassHolderSource(classLoader, referenceCache)));
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        for (Iterator<Method> iterator = getFilteredChildren().iterator(); iterator.hasNext();) {
            Method method = iterator.next();
            if (filter.shouldRun(describeChild(method))) {
                filter.apply(method);
            } else {
                iterator.remove();
            }
        }
    }

    private void writeRunsDescriptor() {
        if (runsInCurrentClass.isEmpty()) {
            return;
        }

        File outputDir = getOutputPathForClass();
        outputDir.mkdirs();
        File descriptorFile = new File(outputDir, "tests.json");
        try (OutputStream output = new FileOutputStream(descriptorFile);
                OutputStream bufferedOutput = new BufferedOutputStream(output);
                Writer writer = new OutputStreamWriter(bufferedOutput)) {
            writer.write("[\n");
            boolean first = true;
            for (TestRun run : runsInCurrentClass.toArray(new TestRun[0])) {
                if (!first) {
                    writer.write(",\n");
                }
                first = false;
                writer.write("  {\n");
                writer.write("    \"baseDir\": ");
                writeJsonString(writer, run.getBaseDirectory().getAbsolutePath().replace('\\', '/'));
                writer.write(",\n");
                writer.write("    \"fileName\": ");
                writeJsonString(writer, run.getFileName());
                writer.write(",\n");
                writer.write("    \"kind\": \"" + run.getKind().name() + "\"");
                if (run.getArgument() != null) {
                    writer.write(",\n");
                    writer.write("    \"argument\": ");
                    writeJsonString(writer, run.getArgument());
                }
                writer.write(",\n");
                writer.write("    \"name\": ");
                writeJsonString(writer, run.getName());
                writer.write("\n  }");
            }
            writer.write("\n]");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeJsonString(Writer writer, String s) throws IOException {
        writer.write('"');
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    writer.write("\\\"");
                    break;
                case '\\':
                    writer.write("\\\\");
                    break;
                case '\r':
                    writer.write("\\r");
                    break;
                case '\n':
                    writer.write("\\n");
                    break;
                case '\t':
                    writer.write("\\t");
                    break;
                case '\f':
                    writer.write("\\f");
                    break;
                case '\b':
                    writer.write("\\b");
                    break;
                default:
                    if (c < ' ') {
                        writer.write("\\u00");
                        writer.write(hex(c / 16));
                        writer.write(hex(c % 16));
                    } else {
                        writer.write(c);
                    }
                    break;
            }
        }
        writer.write('"');
    }

    private static char hex(int digit) {
        return (char) (digit < 10 ? '0' + digit : 'A' + digit - 10);
    }

    static class CompileResult {
        boolean success = true;
        String errorMessage;
        File file;
        Throwable throwable;
    }
}
