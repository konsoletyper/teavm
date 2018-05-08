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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
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
import org.teavm.backend.javascript.JavaScriptTarget;
import org.teavm.backend.wasm.WasmTarget;
import org.teavm.callgraph.CallGraph;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.diagnostics.Problem;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.ValueType;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.testing.JUnitTestAdapter;
import org.teavm.testing.TestAdapter;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMTarget;

public class TeaVMTestRunner extends Runner implements Filterable {
    private static final String PATH_PARAM = "teavm.junit.target";
    private static final String JS_RUNNER = "teavm.junit.js.runner";
    private static final String THREAD_COUNT = "teavm.junit.js.threads";
    private static final String SELENIUM_URL = "teavm.junit.js.selenium.url";
    private static final String JS_ENABLED = "teavm.junit.js";
    private static final String C_ENABLED = "teavm.junit.c";
    private static final String WASM_ENABLED = "teavm.junit.wasm";
    private static final String C_COMPILER = "teavm.junit.c.compiler";
    private static final String MINIFIED = "teavm.junit.minified";
    private static final String OPTIMIZED = "teavm.junit.optimized";

    private static final int stopTimeout = 15000;
    private Class<?> testClass;
    private ClassHolder classHolder;
    private ClassLoader classLoader;
    private Description suiteDescription;
    private static Map<ClassLoader, ClassHolderSource> classSources = new WeakHashMap<>();
    private File outputDir;
    private TestAdapter testAdapter = new JUnitTestAdapter();
    private Map<Method, Description> descriptions = new HashMap<>();
    private static Map<RunKind, RunnerKindInfo> runners = new HashMap<>();
    private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private CountDownLatch latch;
    private List<Method> filteredChildren;

    static class RunnerKindInfo {
        volatile TestRunner runner;
        volatile TestRunStrategy strategy;
        volatile ScheduledFuture<?> cleanupFuture;
    }

    static {
        for (RunKind kind : RunKind.values()) {
            runners.put(kind, new RunnerKindInfo());
            runners.put(kind, new RunnerKindInfo());
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (TeaVMTestRunner.class) {
                for (RunnerKindInfo info : runners.values()) {
                    if (info.runner != null) {
                        info.cleanupFuture = null;
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
        ClassHolderSource classSource = getClassSource(classLoader);
        classHolder = classSource.get(testClass.getName());
        String outputPath = System.getProperty(PATH_PARAM);
        if (outputPath != null) {
            outputDir = new File(outputPath);
        }

        String runStrategyName = System.getProperty(JS_RUNNER);
        if (runStrategyName != null) {
            TestRunStrategy jsRunStrategy;
            switch (runStrategyName) {
                case "selenium":
                    try {
                        jsRunStrategy = new SeleniumRunStrategy(new URL(System.getProperty(SELENIUM_URL)));
                    } catch (MalformedURLException e) {
                        throw new InitializationError(e);
                    }
                    break;
                case "htmlunit":
                    jsRunStrategy = new HtmlUnitRunStrategy();
                    break;
                case "":
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
    }

    @Override
    public Description getDescription() {
        if (suiteDescription == null) {
            suiteDescription = Description.createSuiteDescription(testClass);
            for (Method child : getFilteredChildren()) {
                suiteDescription.getChildren().add(describeChild(child));
            }
        }
        return suiteDescription;
    }

    @Override
    public void run(RunNotifier notifier) {
        List<Method> children = getFilteredChildren();
        latch = new CountDownLatch(children.size());

        notifier.fireTestStarted(getDescription());
        for (Method child : children) {
            runChild(child, notifier);
        }

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
        for (Method method : testClass.getDeclaredMethods()) {
            MethodHolder methodHolder = classHolder.getMethod(getDescriptor(method));
            if (testAdapter.acceptMethod(methodHolder)) {
                children.add(method);
            }
        }
        return children;
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

    private void runChild(Method child, RunNotifier notifier) {
        notifier.fireTestStarted(describeChild(child));

        boolean ran = false;
        boolean success = true;

        MethodHolder methodHolder = classHolder.getMethod(getDescriptor(child));
        Set<Class<?>> expectedExceptions = new HashSet<>();
        for (String exceptionName : testAdapter.getExpectedExceptions(methodHolder)) {
            try {
                expectedExceptions.add(Class.forName(exceptionName, false, classLoader));
            } catch (ClassNotFoundException e) {
                notifier.fireTestFailure(new Failure(describeChild(child), e));
                notifier.fireTestFinished(describeChild(child));
                latch.countDown();
                return;
            }
        }

        if (!child.isAnnotationPresent(SkipJVM.class)
                && !child.getDeclaringClass().isAnnotationPresent(SkipJVM.class)) {
            ran = true;
            success = runInJvm(child, notifier, expectedExceptions);
        }

        Description description = describeChild(child);
        if (success && outputDir != null) {
            int[] configurationIndex = new int[] { 0 };
            List<Consumer<Boolean>> onSuccess = new ArrayList<>();

            List<TestRun> runs = new ArrayList<>();
            onSuccess.add(runSuccess -> {
                if (runSuccess && configurationIndex[0] < runs.size()) {
                    submitRun(runs.get(configurationIndex[0]++));
                } else {
                    notifier.fireTestFinished(description);
                    latch.countDown();
                }
            });

            try {
                File outputPath = getOutputPath(child);
                copyJsFilesTo(outputPath);

                for (TeaVMTestConfiguration<JavaScriptTarget> configuration : getJavaScriptConfigurations()) {
                    TestRun run = compile(child, notifier, RunKind.JAVASCRIPT,
                            m -> compileToJs(m, configuration, outputPath), onSuccess.get(0));
                    if (run != null) {
                        runs.add(run);
                    }
                }

                for (TeaVMTestConfiguration<CTarget> configuration : getCConfigurations()) {
                    TestRun run = compile(child, notifier, RunKind.C,
                            m -> compileToC(m, configuration, outputPath), onSuccess.get(0));
                    if (run != null) {
                        runs.add(run);
                    }
                }

                for (TeaVMTestConfiguration<WasmTarget> configuration : getWasmConfigurations()) {
                    TestRun run = compile(child, notifier, RunKind.WASM,
                            m -> compileToWasm(m, configuration, outputPath), onSuccess.get(0));
                    if (run != null) {
                        runs.add(run);
                    }
                }

            } catch (Throwable e) {
                notifier.fireTestFailure(new Failure(description, e));
                notifier.fireTestFinished(description);
                latch.countDown();
                return;
            }

            onSuccess.get(0).accept(true);
        } else {
            if (!ran) {
                notifier.fireTestIgnored(description);
            }
            notifier.fireTestFinished(description);
            latch.countDown();
        }
    }

    private boolean runInJvm(Method child, RunNotifier notifier, Set<Class<?>> expectedExceptions) {
        Object instance;
        try {
            instance = testClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            notifier.fireTestFailure(new Failure(describeChild(child), e));
            return false;
        }

        boolean expectedCaught = false;
        try {
            child.invoke(instance);
        } catch (IllegalAccessException e) {
            notifier.fireTestFailure(new Failure(describeChild(child), e));
            return false;
        } catch (InvocationTargetException e) {
            boolean wasExpected = false;
            for (Class<?> expected : expectedExceptions) {
                if (expected.isInstance(e.getTargetException())) {
                    expectedCaught = true;
                    wasExpected = true;
                }
            }
            if (!wasExpected) {
                notifier.fireTestFailure(new Failure(describeChild(child), e.getTargetException()));
                return false;
            }
        }

        if (!expectedCaught && !expectedExceptions.isEmpty()) {
            notifier.fireTestAssumptionFailed(new Failure(describeChild(child),
                    new AssertionError("Expected exception was not thrown")));
            return false;
        }

        return true;
    }

    private TestRun compile(Method child, RunNotifier notifier, RunKind kind,
            CompileFunction compiler, Consumer<Boolean> onComplete) {
        Description description = describeChild(child);

        CompileResult compileResult;
        try {
            compileResult = compiler.compile(child);
        } catch (Exception e) {
            notifier.fireTestFailure(new Failure(description, e));
            notifier.fireTestFinished(description);
            latch.countDown();
            return null;
        }

        if (!compileResult.success) {
            notifier.fireTestFailure(new Failure(description, new AssertionError(compileResult.errorMessage)));
            return null;
        }

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

        return new TestRun(compileResult.file.getParentFile(), child, description, compileResult.file.getName(),
                kind, callback);
    }

    private void submitRun(TestRun run) {
        synchronized (TeaVMTestRunner.class) {
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

            if (info.cleanupFuture != null) {
                info.cleanupFuture.cancel(false);
                info.cleanupFuture = null;
            }
            RunKind kind = run.getKind();
            info.cleanupFuture = executor.schedule(() -> cleanupRunner(kind), stopTimeout, TimeUnit.MILLISECONDS);
        }
    }

    private static void cleanupRunner(RunKind kind) {
        synchronized (TeaVMTestRunner.class) {
            RunnerKindInfo info = runners.get(kind);
            info.cleanupFuture = null;
            info.runner.stop();
            info.runner = null;
        }
    }

    private File getOutputPath(Method method) {
        File path = outputDir;
        path = new File(path, method.getDeclaringClass().getName().replace('.', '/'));
        path = new File(path, method.getName());
        path.mkdirs();
        return path;
    }

    private void copyJsFilesTo(File path) throws IOException {
        resourceToFile("org/teavm/backend/javascript/runtime.js", new File(path, "runtime.js"));
        resourceToFile("org/teavm/backend/wasm/wasm-runtime.js", new File(path, "test.wasm-runtime.js"));
        resourceToFile("teavm-run-test.html", new File(path, "run-test.html"));
        resourceToFile("teavm-run-test-wasm.html", new File(path, "run-test-wasm.html"));
    }

    private CompileResult compileToJs(Method method, TeaVMTestConfiguration<JavaScriptTarget> configuration,
            File path) {
        return compileTest(method, configuration, JavaScriptTarget::new, vm -> {
            MethodReference exceptionMsg = new MethodReference(ExceptionHelper.class, "showException",
                    Throwable.class, String.class);
            vm.entryPoint("runTest", new MethodReference(TestEntryPoint.class, "run", void.class)).async();
            vm.entryPoint("extractException", exceptionMsg);
        }, path, ".js");
    }

    private CompileResult compileToC(Method method, TeaVMTestConfiguration<CTarget> configuration,
            File path) {
        return compileTest(method, configuration, CTarget::new, vm -> {
            vm.entryPoint("main", new MethodReference(TestEntryPoint.class, "main", String[].class, void.class));
        }, path, ".c");
    }

    private CompileResult compileToWasm(Method method, TeaVMTestConfiguration<WasmTarget> configuration,
            File path) {
        return compileTest(method, configuration, WasmTarget::new, vm -> {
            vm.entryPoint("main", new MethodReference(TestEntryPoint.class, "main", String[].class, void.class));
        }, path, ".wasm");
    }

    private <T extends TeaVMTarget> CompileResult compileTest(Method method, TeaVMTestConfiguration<T> configuration,
            Supplier<T> targetSupplier, Consumer<TeaVM> preBuild, File path, String extension) {
        CompileResult result = new CompileResult();

        StringBuilder simpleName = new StringBuilder();
        simpleName.append("test");
        String suffix = configuration.getSuffix();
        if (!suffix.isEmpty()) {
            simpleName.append('-').append(suffix);
        }
        simpleName.append(extension);
        File outputFile = new File(path, simpleName.toString());
        result.file = outputFile;

        ClassLoader classLoader = TeaVMTestRunner.class.getClassLoader();
        ClassHolderSource classSource = getClassSource(classLoader);

        MethodHolder methodHolder = classHolder.getMethod(getDescriptor(method));
        Class<?> runnerType = testAdapter.getRunner(methodHolder);

        T target = targetSupplier.get();
        configuration.apply(target);

        TeaVM vm = new TeaVMBuilder(target)
                .setClassLoader(classLoader)
                .setClassSource(classSource)
                .build();

        Properties properties = new Properties();
        applyProperties(method.getDeclaringClass(), properties);
        vm.setProperties(properties);

        vm.setIncremental(false);
        configuration.apply(vm);
        vm.installPlugins();

        new TestExceptionPlugin().install(vm);
        new TestEntryPointTransformer(runnerType.getName(), methodHolder.getReference()).install(vm);

        preBuild.accept(vm);
        vm.build(new DirectoryBuildTarget(outputFile.getParentFile()), outputFile.getName());
        if (!vm.getProblemProvider().getProblems().isEmpty()) {
            result.success = false;
            result.errorMessage = buildErrorMessage(vm);
        }

        return result;
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

    private void resourceToFile(String resource, File fileName) throws IOException {
        try (InputStream input = TeaVMTestRunner.class.getClassLoader().getResourceAsStream(resource);
                OutputStream output = new FileOutputStream(fileName)) {
            IOUtils.copy(input, output);
        }
    }

    private static ClassHolderSource getClassSource(ClassLoader classLoader) {
        return classSources.computeIfAbsent(classLoader, cl -> new PreOptimizingClassHolderSource(
                new ClasspathClassHolderSource(classLoader)));
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

    static class CompileResult {
        boolean success = true;
        String errorMessage;
        File file;
    }

    interface CompileFunction {
        CompileResult compile(Method method);
    }
}
