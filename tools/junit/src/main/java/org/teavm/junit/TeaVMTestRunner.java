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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

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
    static final String JUPITER_BEFORE_EACH = "org.junit.jupiter.api.BeforeEach";
    static final String JUPITER_AFTER_EACH = "org.junit.jupiter.api.AfterEach";

    private Class<?> testClass;
    private boolean isWholeClassCompilation;
    private static final ClassHolderSource classSource = TeaVMTestInfrastructure.classSource;
    private Description suiteDescription;
    private static final File outputDir = TeaVMTestInfrastructure.outputDir;
    private Map<Method, Description> descriptions = new HashMap<>();
    private static final Map<TestPlatform, TestRunStrategy> runners = TeaVMTestInfrastructure.runners;
    private List<Method> filteredChildren;
    private List<TestRun> runsInCurrentClass = new ArrayList<>();
    private static final List<TestPlatformSupport<?>> platforms = TeaVMTestInfrastructure.platforms;

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
        List<Method> children = getFilteredChildren();

        isWholeClassCompilation = !testClass.isAnnotationPresent(EachTestCompiledSeparately.class);
        if (isWholeClassCompilation) {
            runWithWholeClassCompilation(children, notifier);
        } else {
            for (Method child : children) {
                runChild(child, notifier);
            }
        }

        writeRunsDescriptor();
        runsInCurrentClass.clear();
    }

    private void runWithWholeClassCompilation(List<Method> children, RunNotifier notifier) {
        Map<TestPlatform, Map<Method, List<TestRun>>> tests;
        try {
            tests = TeaVMTestExecutionSupport.compileWholeClass(children, testClass, this::isIgnored,
                    Map.of());
        } catch (Throwable t) {
            notifier.fireTestFailure(new Failure(getDescription(), t));
            failAllClasses(children, notifier);
            return;
        }

        var skipJvmForClass = !testClass.isAnnotationPresent(SkipJVM.class);

        for (var child : children) {
            var description = describeChild(child);

            if (isIgnored(child)) {
                notifier.fireTestIgnored(description);
            } else {
                notifier.fireTestStarted(description);
                var success = true;
                if (skipJvmForClass && !child.isAnnotationPresent(SkipJVM.class)) {
                    ClassHolder classHolder = classSource.get(child.getDeclaringClass().getName());
                    MethodHolder methodHolder = classHolder.getMethod(TeaVMTestExecutionSupport.getDescriptor(child));
                    success = runInJvm(child, notifier, TestEntryPointTransformer.getExpectedExceptions(methodHolder));
                }

                if (success) {
                    for (var platform : platforms) {
                        var methodRuns = tests.get(platform.getPlatform());
                        var runs = methodRuns != null ? methodRuns.get(child) : null;
                        if (runs != null) {
                            for (var run : runs) {
                                try {
                                    submitRun(run);
                                } catch (Throwable e) {
                                    notifier.fireTestFailure(new Failure(description, e));
                                    break;
                                }
                            }
                        }
                    }
                }
                notifier.fireTestFinished(description);
            }
        }

        TeaVMTestExecutionSupport.cleanupWholeClass(tests);
    }

    private void failAllClasses(List<Method> children, RunNotifier notifier) {
        for (var child : children) {
            var description = describeChild(child);
            notifier.fireTestStarted(description);

            if (isIgnored(child)) {
                notifier.fireTestIgnored(description);
                return;
            } else {
                notifier.fireTestFailure(new Failure(description,
                        new AssertionError("Could not compile test class")));
            }

            notifier.fireTestFinished(description);
        }
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

    private void runChild(Method child, RunNotifier notifier) {
        Description description = describeChild(child);

        if (isIgnored(child)) {
            notifier.fireTestIgnored(description);
            return;
        }
        notifier.fireTestStarted(description);

        boolean ran = false;
        boolean success = true;

        if (!child.isAnnotationPresent(SkipJVM.class) && !testClass.isAnnotationPresent(SkipJVM.class)) {
            ran = true;
            ClassHolder classHolder = classSource.get(child.getDeclaringClass().getName());
            MethodHolder methodHolder = classHolder.getMethod(TeaVMTestExecutionSupport.getDescriptor(child));
            success = runInJvm(child, notifier, TestEntryPointTransformer.getExpectedExceptions(methodHolder));
        }

        if (success && outputDir != null) {
            List<TestRun> runs = new ArrayList<>();

            try {
                prepareCompiledTest(child, notifier, runs);

                for (var run : runs) {
                    try {
                        submitRun(run);
                    } catch (Throwable e) {
                        notifier.fireTestFailure(new Failure(description, e));
                        break;
                    }
                }

                for (var run : runs) {
                    var strategy = runners.get(run.getGroup().getKind());
                    strategy.cleanup();
                }
            } finally {
                notifier.fireTestFinished(description);
            }
        } else {
            if (!ran) {
                notifier.fireTestIgnored(description);
            }
            notifier.fireTestFinished(description);
        }
    }

    private void prepareCompiledTest(Method child, RunNotifier notifier, List<TestRun> runs) {
        try {
            var compiled = TeaVMTestExecutionSupport.compileSingleMethod(child, testClass, null);
            for (var platform : platforms) {
                var platformRuns = compiled.get(platform.getPlatform());
                if (platformRuns != null) {
                    runs.addAll(platformRuns);
                }
            }
        } catch (Throwable e) {
            notifier.fireTestFailure(new Failure(describeChild(child), e));
        }
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

        Map<String, Method> afterMethods = new LinkedHashMap<>();
        for (Class<?> c : classes) {
            for (Method method : c.getDeclaredMethods()) {
                if (getAnnotation(method, JUNIT4_AFTER) != null || getAnnotation(method, TESTNG_AFTER) != null) {
                    afterMethods.putIfAbsent(signatureOf(method), method);
                }
            }
        }

        Map<String, Method> beforeMethods = new LinkedHashMap<>();
        Collections.reverse(classes);
        for (Class<?> c : classes) {
            for (Method method : c.getDeclaredMethods()) {
                if (getAnnotation(method, JUNIT4_BEFORE) != null || getAnnotation(method, TESTNG_BEFORE) != null) {
                    beforeMethods.putIfAbsent(signatureOf(method), method);
                }
            }
        }

        if (beforeMethods.isEmpty() && afterMethods.isEmpty()) {
            return runner;
        }

        return new WithBeforeAndAfterRunner(runner, instance,
                beforeMethods.values().toArray(new Method[0]),
                afterMethods.values().toArray(new Method[0]));
    }

    private static String signatureOf(Method method) {
        StringBuilder signature = new StringBuilder(method.getName());
        for (Class<?> parameter : method.getParameterTypes()) {
            signature.append(':').append(parameter.getName());
        }
        return signature.toString();
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

    private void submitRun(TestRun run) throws IOException {
        runsInCurrentClass.add(run);
        var strategy = runners.get(run.getGroup().getKind());
        if (strategy == null) {
            return;
        }

        strategy.runTest(run);
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
        MethodDescriptor descriptor = TeaVMTestExecutionSupport.getDescriptor(method);
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
        TeaVMTestExecutionSupport.writeRunsDescriptor(testClass, runsInCurrentClass);
    }
}
