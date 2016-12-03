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
import org.teavm.backend.javascript.JavaScriptTarget;
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

public class TeaVMTestRunner extends Runner implements Filterable {
    private static final String PATH_PARAM = "teavm.junit.target";
    private static final String RUNNER = "teavm.junit.js.runner";
    private static final String THREAD_COUNT = "teavm.junit.js.threads";
    private static final String SELENIUM_URL = "teavm.junit.js.selenium.url";
    private static final int stopTimeout = 15000;
    private Class<?> testClass;
    private ClassHolder classHolder;
    private ClassLoader classLoader;
    private Description suiteDescription;
    private static Map<ClassLoader, ClassHolderSource> classSources = new WeakHashMap<>();
    private File outputDir;
    private TestAdapter testAdapter = new JUnitTestAdapter();
    private Map<Method, Description> descriptions = new HashMap<>();
    private TestRunStrategy runStrategy;
    private static volatile TestRunner runner;
    private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private static volatile ScheduledFuture<?> cleanupFuture;
    private CountDownLatch latch;
    private List<Method> filteredChildren;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (TeaVMTestRunner.class) {
                if (runner != null) {
                    cleanupFuture = null;
                    runner.stop();
                    runner.waitForCompletion();
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

        String runStrategyName = System.getProperty(RUNNER);
        if (runStrategyName != null) {
            switch (runStrategyName) {
                case "selenium":
                    try {
                        runStrategy = new SeleniumRunStrategy(new URL(System.getProperty(SELENIUM_URL)));
                    } catch (MalformedURLException e) {
                        throw new InitializationError(e);
                    }
                    break;
                case "htmlunit":
                    runStrategy = new HtmlUnitRunStrategy();
                    break;
                default:
                    throw new InitializationError("Unknown run strategy: " + runStrategyName);
            }
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
            List<TeaVMTestConfiguration> configurations = getConfigurations();
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

            for (TeaVMTestConfiguration configuration : configurations) {
                try {
                    TestRun run = compileByTeaVM(child, notifier, expectedExceptions, configuration, onSuccess.get(0));
                    if (run != null) {
                        runs.add(run);
                    } else {
                        notifier.fireTestFinished(description);
                        latch.countDown();
                        return;
                    }
                } catch (Throwable e) {
                    notifier.fireTestFailure(new Failure(description, e));
                    notifier.fireTestFinished(description);
                    latch.countDown();
                    return;
                }
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

    private TestRun compileByTeaVM(Method child, RunNotifier notifier, Set<Class<?>> expectedExceptions,
            TeaVMTestConfiguration configuration, Consumer<Boolean> onComplete) {
        Description description = describeChild(child);

        CompileResult compileResult;
        try {
            compileResult = compileTest(child, configuration);
        } catch (Exception e) {
            notifier.fireTestFailure(new Failure(description, e));
            return null;
        }

        if (!compileResult.success) {
            notifier.fireTestFailure(new Failure(description,
                    new AssertionError(compileResult.errorMessage)));
            return null;
        }

        if (runStrategy == null) {
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

        return new TestRun(compileResult.file.getParentFile(), child,
                new MethodReference(testClass.getName(), getDescriptor(child)),
                description, callback, expectedExceptions);
    }

    private void submitRun(TestRun run) {
        synchronized (TeaVMTestRunner.class) {
            if (runStrategy == null) {
                return;
            }

            if (runner == null) {
                runner = new TestRunner(runStrategy);
                try {
                    runner.setNumThreads(Integer.parseInt(System.getProperty(THREAD_COUNT, "1")));
                } catch (NumberFormatException e) {
                    runner.setNumThreads(1);
                }
                runner.init();
            }
            runner.run(run);

            if (cleanupFuture != null) {
                cleanupFuture.cancel(false);
                cleanupFuture = null;
            }
            cleanupFuture = executor.schedule(TeaVMTestRunner::cleanupRunner, stopTimeout, TimeUnit.MILLISECONDS);
        }
    }

    private static void cleanupRunner() {
        synchronized (TeaVMTestRunner.class) {
            cleanupFuture = null;
            runner.stop();
            runner = null;
        }
    }

    private CompileResult compileTest(Method method, TeaVMTestConfiguration configuration) throws IOException {
        CompileResult result = new CompileResult();

        File path = outputDir;
        path = new File(path, method.getDeclaringClass().getName().replace('.', '/'));
        path = new File(path, method.getName());
        path.mkdirs();

        StringBuilder simpleName = new StringBuilder();
        simpleName.append("test");
        String suffix = configuration.getSuffix();
        if (!suffix.isEmpty()) {
            simpleName.append('-').append(suffix);
        }
        simpleName.append(".js");
        File outputFile = new File(path, simpleName.toString());
        result.file = outputFile;

        resourceToFile("org/teavm/backend/javascript/runtime.js", new File(path, "runtime.js"));
        resourceToFile("teavm-run-test.html", new File(path, "run-test.html"));

        ClassLoader classLoader = TeaVMTestRunner.class.getClassLoader();
        ClassHolderSource classSource = getClassSource(classLoader);

        MethodHolder methodHolder = classHolder.getMethod(getDescriptor(method));
        Class<?> runnerType = testAdapter.getRunner(methodHolder);

        JavaScriptTarget jsTarget = new JavaScriptTarget();
        configuration.apply(jsTarget);

        TeaVM vm = new TeaVMBuilder(jsTarget)
                .setClassLoader(classLoader)
                .setClassSource(classSource)
                .build();
        vm.setIncremental(false);
        configuration.apply(vm);
        vm.installPlugins();

        new TestExceptionPlugin().install(vm);
        new TestEntryPointTransformer(runnerType.getName(), methodHolder.getReference()).install(vm);

        Properties properties = new Properties();
        applyProperties(method.getDeclaringClass(), properties);
        vm.setProperties(properties);

        MethodReference exceptionMsg = new MethodReference(ExceptionHelper.class, "showException",
                Throwable.class, String.class);
        vm.entryPoint("runTest", new MethodReference(TestEntryPoint.class, "run", void.class)).async();
        vm.entryPoint("extractException", exceptionMsg);
        vm.build(new DirectoryBuildTarget(outputFile.getParentFile()), outputFile.getName());
        if (!vm.getProblemProvider().getProblems().isEmpty()) {
            result.success = false;
            result.errorMessage = buildErrorMessage(vm);
        }

        return result;
    }

    private List<TeaVMTestConfiguration> getConfigurations() {
        List<TeaVMTestConfiguration> configurations = new ArrayList<>();
        configurations.add(TeaVMTestConfiguration.DEFAULT);
        if (Boolean.parseBoolean(System.getProperty("teavm.junit.minified", "false"))) {
            configurations.add(TeaVMTestConfiguration.MINIFIED);
        }
        if (Boolean.parseBoolean(System.getProperty("teavm.junit.optimized", "false"))) {
            configurations.add(TeaVMTestConfiguration.OPTIMIZED);
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
}
