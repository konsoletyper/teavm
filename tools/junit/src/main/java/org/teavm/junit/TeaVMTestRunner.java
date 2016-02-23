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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
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
import org.teavm.tooling.testing.TeaVMTestTool;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;

public class TeaVMTestRunner extends ParentRunner<Method> {
    private static final String PATH_PARAM = "teavm.junit.target";
    private ClassHolder classHolder;
    private ClassLoader classLoader;
    private ClassHolderSource classSource;
    private static Map<ClassLoader, ClassHolderSource> classSources = new WeakHashMap<>();
    private File outputDir;
    private TestAdapter testAdapter = new JUnitTestAdapter();
    private Map<Method, Description> descriptions = new HashMap<>();

    public TeaVMTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        classLoader = TeaVMTestRunner.class.getClassLoader();
        classSource = getClassSource(classLoader);
        classHolder = classSource.get(testClass.getName());
        String outputPath = System.getProperty(PATH_PARAM);
        if (outputPath != null) {
            outputDir = new File(outputPath);
        }
    }

    @Override
    protected List<Method> getChildren() {
        List<Method> children = new ArrayList<>();
        for (Method method : getTestClass().getJavaClass().getDeclaredMethods()) {
            MethodHolder methodHolder = classHolder.getMethod(getDescriptor(method));
            if (testAdapter.acceptMethod(methodHolder)) {
                children.add(method);
            }
        }
        return children;
    }

    @Override
    protected Description describeChild(Method child) {
        return descriptions.computeIfAbsent(child, method -> Description.createTestDescription(
                getTestClass().getJavaClass(), method.getName()));
    }

    @Override
    protected void runChild(Method child, RunNotifier notifier) {
        notifier.fireTestStarted(describeChild(child));

        boolean run = false;
        boolean success = true;
        if (outputDir != null) {
            run = true;
            success &= runInTeaVM(child, notifier);
        }

        if (success && !child.isAnnotationPresent(SkipJVM.class)
                && !child.getDeclaringClass().isAnnotationPresent(SkipJVM.class)) {
            run = true;
            success &= runInJvm(child, notifier);
        }

        if (!run) {
            notifier.fireTestIgnored(describeChild(child));
        }
        notifier.fireTestFinished(describeChild(child));
    }

    private boolean runInJvm(Method child, RunNotifier notifier) {
        MethodHolder methodHolder = classHolder.getMethod(getDescriptor(child));
        Set<Class<?>> expectedExceptions = new HashSet<>();
        for (String exceptionName : testAdapter.getExpectedExceptions(methodHolder)) {
            try {
                expectedExceptions.add(Class.forName(exceptionName, false, classLoader));
            } catch (ClassNotFoundException e) {
                notifier.fireTestFailure(new Failure(describeChild(child), e));
                return false;
            }
        }

        Object instance;
        try {
            instance = getTestClass().getJavaClass().newInstance();
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

    private boolean runInTeaVM(Method child, RunNotifier notifier) {
        CompileResult compileResult;
        try {
            compileResult = compileTest(child);
        } catch (IOException e) {
            notifier.fireTestFailure(new Failure(describeChild(child), e));
            return false;
        }

        if (!compileResult.success) {
            notifier.fireTestFailure(new Failure(describeChild(child),
                    new AssertionError(compileResult.errorMessage)));
            return false;
        }

        return true;
    }

    private CompileResult compileTest(Method method) throws IOException {
        CompileResult result = new CompileResult();

        File path = outputDir;
        path = new File(path, method.getDeclaringClass().getName().replace('.', '/'));
        path = new File(path, method.getName());
        path.mkdirs();
        File outputFile = new File(path, "test.js");
        result.file = outputFile;

        resourceToFile("org/teavm/javascript/runtime.js", new File(path, "runtime.js"));
        resourceToFile("teavm-run-test.html", new File(path, "run-test.html"));

        ClassLoader classLoader = TeaVMTestRunner.class.getClassLoader();
        ClassHolderSource classSource = getClassSource(classLoader);

        MethodHolder methodHolder = classHolder.getMethod(getDescriptor(method));
        Class<?> runnerType = testAdapter.getRunner(methodHolder);

        TeaVM vm = new TeaVMBuilder()
                .setClassLoader(classLoader)
                .setClassSource(classSource)
                .build();
        vm.setIncremental(false);
        vm.setMinifying(false);
        vm.installPlugins();

        new TestExceptionPlugin().install(vm);
        new TestEntryPointTransformer(runnerType.getName(), methodHolder.getReference()).install(vm);

        try (Writer innerWriter = new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")) {
            MethodReference exceptionMsg = new MethodReference(ExceptionHelper.class, "showException",
                    Throwable.class, String.class);
            vm.entryPoint("runTest", new MethodReference(TestEntryPoint.class, "run", void.class)).async();
            vm.entryPoint("extractException", exceptionMsg);
            vm.build(innerWriter, new DirectoryBuildTarget(outputDir));
            if (!vm.getProblemProvider().getProblems().isEmpty()) {
                result.success = false;
                result.errorMessage = buildErrorMessage(vm);
            }
        }

        return result;
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
        try (InputStream input = TeaVMTestTool.class.getClassLoader().getResourceAsStream(resource);
                OutputStream output = new FileOutputStream(fileName)) {
            IOUtils.copy(input, output);
        }
    }

    private static ClassHolderSource getClassSource(ClassLoader classLoader) {
        return classSources.computeIfAbsent(classLoader, cl -> new PreOptimizingClassHolderSource(
                new ClasspathClassHolderSource(classLoader)));
    }

    static class CompileResult {
        boolean success = true;
        String errorMessage;
        File file;
    }
}
