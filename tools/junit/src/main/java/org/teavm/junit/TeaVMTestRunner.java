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

import static org.teavm.junit.PropertyNames.PATH_PARAM;
import static org.teavm.junit.TestUtil.getOutputFile;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.AnnotatedElement;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import junit.framework.TestCase;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
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
import org.teavm.vm.TeaVM;
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

    private Class<?> testClass;
    private boolean isWholeClassCompilation;
    private static ClassHolderSource classSource;
    private static ClassLoader classLoader;
    private Description suiteDescription;
    private static File outputDir;
    private Map<Method, Description> descriptions = new HashMap<>();
    private static Map<TestPlatform, TestRunStrategy> runners = new HashMap<>();
    private List<Method> filteredChildren;
    private static ReferenceCache referenceCache = new ReferenceCache();
    private boolean classCompilationOk;
    private List<TestRun> runsInCurrentClass = new ArrayList<>();
    private static List<TestPlatformSupport<?>> platforms = new ArrayList<>();
    private List<TestPlatformSupport<?>> participatingPlatforms = new ArrayList<>();

    static {
        classLoader = TeaVMTestRunner.class.getClassLoader();
        classSource = getClassSource(classLoader);

        String outputPath = System.getProperty(PATH_PARAM);
        if (outputPath != null) {
            outputDir = new File(outputPath);
        }

        platforms.add(new JSPlatformSupport(classSource, referenceCache));
        platforms.add(new WebAssemblyPlatformSupport(classSource, referenceCache));
        platforms.add(new WasiPlatformSupport(classSource, referenceCache));
        platforms.add(new CPlatformSupport(classSource, referenceCache));

        for (var platform : platforms) {
            if (platform.isEnabled() && !platform.getConfigurations().isEmpty()) {
                var runStrategy = platform.createRunStrategy(outputDir);
                if (runStrategy != null) {
                    runners.put(platform.getPlatform(), runStrategy);
                }
            }
        }

        for (var strategy : runners.values()) {
            strategy.beforeAll();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (var strategy : runners.values()) {
                strategy.afterAll();
            }
        }));
    }

    public TeaVMTestRunner(Class<?> testClass) throws InitializationError {
        this.testClass = testClass;
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
        for (var platform : platforms) {
            if (platform.isEnabled() && !platform.getConfigurations().isEmpty()) {
                participatingPlatforms.add(platform);
            }
        }

        List<Method> children = getFilteredChildren();
        var description = getDescription();

        notifier.fireTestStarted(description);
        isWholeClassCompilation = !testClass.isAnnotationPresent(EachTestCompiledSeparately.class);
        if (isWholeClassCompilation) {
            classCompilationOk = compileWholeClass(children, notifier);
        }
        for (Method child : children) {
            runChild(child, notifier);
        }

        writeRunsDescriptor();
        runsInCurrentClass.clear();

        notifier.fireTestFinished(description);
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
        Description description = getDescription();

        for (var platformSupport : participatingPlatforms) {
            if (!compileClassForPlatform(platformSupport, children, description, notifier)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean compileClassForPlatform(TestPlatformSupport<?> platform, List<Method> children,
            Description description, RunNotifier notifier) {
        if (platform.isEnabled() && hasChildrenToRun(children, platform.getPlatform())) {
            for (var configuration : platform.getConfigurations()) {
                var path = getOutputPathForClass(platform);
                var castPlatform = (TestPlatformSupport<TeaVMTarget>) platform;
                var castConfiguration = (TeaVMTestConfiguration<TeaVMTarget>) configuration;
                var result = castPlatform.compile(wholeClass(children, platform.getPlatform()), "classTest",
                        castConfiguration, path);
                if (!result.success) {
                    notifier.fireTestFailure(createFailure(description, result));
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPlatformPresent(AnnotatedElement declaration, TestPlatform platform) {
        var skipPlatform = declaration.getAnnotation(SkipPlatform.class);
        if (skipPlatform != null) {
            for (var toSkip : skipPlatform.value()) {
                if (toSkip == platform) {
                    return false;
                }
            }
        }

        var onlyPlatform = declaration.getAnnotation(OnlyPlatform.class);
        if (onlyPlatform != null) {
            for (var allowedPlatform : onlyPlatform.value()) {
                if (allowedPlatform == platform) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    private boolean hasChildrenToRun(List<Method> children, TestPlatform platform) {
        return isPlatformPresent(testClass, platform)
                && children.stream().anyMatch(child -> isPlatformPresent(child, platform));
    }

    private List<Method> filterChildren(List<Method> children, TestPlatform platform) {
        return children.stream().filter(child -> isPlatformPresent(child, platform)).collect(Collectors.toList());
    }

    private boolean shouldRunChild(Method child, TestPlatform platform) {
        return isPlatformPresent(testClass, platform) && isPlatformPresent(child, platform);
    }

    private void runChild(Method child, RunNotifier notifier) {
        Description description = describeChild(child);
        notifier.fireTestStarted(description);

        if (isIgnored(child)) {
            notifier.fireTestIgnored(description);
            notifier.fireTestFinished(description);
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
            List<TestRun> runs = new ArrayList<>();

            if (isWholeClassCompilation) {
                if (!classCompilationOk) {
                    notifier.fireTestFinished(description);
                    notifier.fireTestFailure(new Failure(description,
                            new AssertionError("Could not compile test class")));
                } else {
                    prepareTestsFromWholeClass(child, runs);
                }
            } else {
                prepareCompiledTest(child, notifier, runs);
            }

            for (var run : runs) {
                try {
                    submitRun(run);
                } catch (Throwable e) {
                    notifier.fireTestFailure(new Failure(description, e));
                    break;
                }
            }
            notifier.fireTestFinished(description);
        } else {
            if (!ran) {
                notifier.fireTestIgnored(description);
            }
            notifier.fireTestFinished(description);
        }
    }

    private void prepareTestsFromWholeClass(Method child, List<TestRun> runs) {
        MethodDescriptor descriptor = getDescriptor(child);
        MethodReference reference = new MethodReference(child.getDeclaringClass().getName(), descriptor);

        for (var platform : participatingPlatforms) {
            if (platform.isEnabled() && shouldRunChild(child, platform.getPlatform())) {
                var outputPath = getOutputPathForClass(platform);
                var outputPathForMethod = getOutputPath(child, platform);
                for (var configuration : platform.getConfigurations()) {
                    var testPath = getOutputFile(outputPath, "classTest", configuration.getSuffix(), false,
                            platform.getExtension());
                    runs.add(createTestRun(configuration, testPath, child, platform.getPlatform(),
                            reference.toString()));
                    platform.additionalOutput(outputPath, outputPathForMethod, configuration, reference);
                }
            }
        }
    }

    private void prepareCompiledTest(Method child, RunNotifier notifier, List<TestRun> runs) {
        MethodDescriptor descriptor = getDescriptor(child);
        MethodReference reference = new MethodReference(child.getDeclaringClass().getName(), descriptor);

        try {
            for (var platform : participatingPlatforms) {
                if (platform.isEnabled() && shouldRunChild(child, platform.getPlatform())) {
                    File outputPath = getOutputPath(child, platform);
                    for (var configuration : platform.getConfigurations()) {
                        @SuppressWarnings("unchecked")
                        var castPlatform = (TestPlatformSupport<TeaVMTarget>) platform;
                        @SuppressWarnings("unchecked")
                        var castConfig = (TeaVMTestConfiguration<TeaVMTarget>) configuration;
                        var compileResult = castPlatform.compile(singleTest(child), "test", castConfig, outputPath);
                        var run = prepareRun(configuration, child, compileResult, notifier, platform.getPlatform());
                        if (run != null) {
                            runs.add(run);
                            platform.additionalSingleTestOutput(outputPath, configuration, reference);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(describeChild(child), e));
            notifier.fireTestFinished(describeChild(child));
        }
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
            RunNotifier notifier, TestPlatform kind) {
        Description description = describeChild(child);

        if (!result.success) {
            notifier.fireTestFailure(createFailure(description, result));
            notifier.fireTestFinished(description);
            return null;
        }

        return createTestRun(configuration, result.file, child, kind, null);
    }

    private TestRun createTestRun(TeaVMTestConfiguration<?> configuration, File file, Method child, TestPlatform kind,
            String argument) {
        return new TestRun(generateName(child.getName(), configuration), file.getParentFile(), child,
                file.getName(), kind, argument);
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

    private void submitRun(TestRun run) throws IOException {
        runsInCurrentClass.add(run);
        var strategy = runners.get(run.getKind());
        if (strategy == null) {
            return;
        }

        strategy.runTest(run);
    }

    private File getOutputPath(Method method, TestPlatformSupport<?> platform) {
        File path = outputDir;
        path = new File(new File(path, platform.getPath()), testClass.getName().replace('.', '/'));
        path = new File(path, method.getName());
        path.mkdirs();
        return path;
    }

    private File getOutputPathForClass(TestPlatformSupport<?> platform) {
        File path = outputDir;
        path = new File(new File(path, platform.getPath()), testClass.getName().replace('.', '/'));
        path.mkdirs();
        return path;
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

    private Consumer<TeaVM> wholeClass(List<Method> methods, TestPlatform platform) {
        return vm -> {
            Properties properties = new Properties();
            applyProperties(testClass, properties);
            vm.setProperties(properties);
            List<MethodReference> methodReferences = new ArrayList<>();
            for (Method method : filterChildren(methods, platform)) {
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

    private static ClassHolderSource getClassSource(ClassLoader classLoader) {
        return new PreOptimizingClassHolderSource(new ClasspathClassHolderSource(classLoader, referenceCache));
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

        for (var platform : participatingPlatforms) {
            writeRunsDescriptor(platform);
        }
    }

    private void writeRunsDescriptor(TestPlatformSupport<?> platform) {
        var runs = runsInCurrentClass.stream().filter(run -> run.getKind() == platform.getPlatform())
                .collect(Collectors.toList());
        if (runs.isEmpty()) {
            return;
        }

        File outputDir = getOutputPathForClass(platform);
        outputDir.mkdirs();
        File descriptorFile = new File(outputDir, "tests.json");
        try (OutputStream output = new FileOutputStream(descriptorFile);
                OutputStream bufferedOutput = new BufferedOutputStream(output);
                Writer writer = new OutputStreamWriter(bufferedOutput)) {
            writer.write("[\n");
            boolean first = true;
            for (TestRun run : runs) {
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
}
