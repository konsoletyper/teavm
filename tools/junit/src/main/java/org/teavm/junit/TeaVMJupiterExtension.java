/*
 *  Copyright 2026 Alexey Andreev.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Runs {@code @Test}-annotated methods on every enabled TeaVM backend (JS, Wasm GC, C), in addition
 * to the plain JVM.
 *
 * <p>By default, all test methods of a class are compiled together into one artifact per backend
 * (in {@link #beforeAll}), the same way {@code TeaVMTestRunner} does for JUnit 4/TestNG — compiling
 * once per class is far cheaper than compiling once per method. Annotate the class with
 * {@link EachTestCompiledSeparately} to opt out and compile each method on its own instead.
 *
 * <p>This only supports the subset of JUnit 5 that maps onto TeaVM's ahead-of-time compilation
 * model: plain {@code @Test} methods, {@code @ParameterizedTest} methods, instance-level
 * {@code @BeforeEach}/{@code @AfterEach} (recognized directly off the compiled class by
 * {@link TestEntryPointTransformer}, the same way JUnit 4's {@code @Before}/{@code @After} already
 * are), and TeaVM's own annotations ({@link SkipPlatform}, {@link OnlyPlatform}, {@link SkipJVM},
 * {@link JsModuleTest}, {@link TeaVMProperties}). There's no support for the general
 * {@code @ExtendWith} extension mechanism, {@code @Nested} classes, dynamic tests, or
 * {@code @BeforeAll}/{@code @AfterAll} — none of those have a sound ahead-of-time-compiled
 * equivalent (or, in the case of other extensions, would require re-executing arbitrary reflective
 * JVM code inside the compiled artifact).
 *
 * <p>Arguments of parameterized tests are produced inside the compiled artifact (see
 * {@link JupiterArgumentsPlan}). All invocations run at once, within a single run of the compiled
 * test, when Jupiter performs the first invocation on the JVM; each invocation then reports the
 * result of the invocation with the same index (see {@link InvocationResults}). Supported argument sources are
 * {@code @ValueSource}, {@code @CsvSource}, {@code @EnumSource}, {@code @NullSource},
 * {@code @EmptySource}, {@code @NullAndEmptySource}, {@code @MethodSource} and
 * {@code @FieldSource} (with parameterless factory methods). Explicit argument converters,
 * aggregators, {@code @CsvFileSource} and custom {@code @ArgumentsSource} providers are not
 * supported.
 */
public final class TeaVMJupiterExtension implements InvocationInterceptor, ExecutionCondition, BeforeAllCallback {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TeaVMJupiterExtension.class);
    private static final ExtensionContext.Namespace RUN_RECORDER_NAMESPACE =
            ExtensionContext.Namespace.create(TeaVMJupiterExtension.class, "runs");
    private static final String PARAMETERIZED_TEST = "org.junit.jupiter.params.ParameterizedTest";
    private static final String INVOCATION_ID_PREFIX = "[test-template-invocation:#";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        var testMethod = context.getTestMethod();
        if (testMethod.isEmpty()) {
            return ConditionEvaluationResult.enabled("not a TeaVM test method");
        }

        var method = testMethod.get();
        var testClass = context.getRequiredTestClass();
        var skipJvm = TeaVMTestExecutionSupport.isSkipJvm(method, testClass);
        var hasBackend = TeaVMTestExecutionSupport.hasParticipatingPlatform(method, testClass);

        if (skipJvm && !hasBackend) {
            return ConditionEvaluationResult.disabled("Neither the JVM nor any TeaVM backend would run this "
                    + "test (teavm.junit.target is not set, @SkipJVM is present, or no backend is enabled "
                    + "for it)");
        }
        return ConditionEvaluationResult.enabled("TeaVM test");
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        var testClass = context.getRequiredTestClass();
        var recorder = new TeaVMJupiterRunRecorder(testClass);
        context.getStore(RUN_RECORDER_NAMESPACE).put(testClass, recorder);

        if (testClass.isAnnotationPresent(EachTestCompiledSeparately.class)) {
            return;
        }

        try {
            var methods = discoverTestMethods(testClass);
            var plans = new HashMap<Method, JupiterArgumentsPlan>();
            var errors = new HashMap<Method, Throwable>();
            for (var method : methods) {
                if (isParameterized(method) && !isDisabled(method)
                        && TeaVMTestExecutionSupport.hasParticipatingPlatform(method, testClass)) {
                    try {
                        plans.put(method, JupiterArgumentsAnalyzer.analyze(method, testClass));
                    } catch (RuntimeException e) {
                        errors.put(method, e);
                    }
                }
            }
            var runsByPlatform = TeaVMTestExecutionSupport.compileWholeClass(methods, testClass,
                    m -> isDisabled(m) || errors.containsKey(m), plans);
            for (var methodRuns : runsByPlatform.values()) {
                for (var runs : methodRuns.values()) {
                    runs.forEach(recorder::record);
                }
            }
            context.getStore(NAMESPACE).put(testClass, new TeaVMWholeClassCompilation(runsByPlatform, errors));
        } catch (Throwable t) {
            rethrow(t);
        }
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        intercept(invocation, invocationContext.getExecutable(), extensionContext, -1);
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
            throws Throwable {
        interceptLifecycleMethod(invocation, extensionContext);
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
            throws Throwable {
        interceptLifecycleMethod(invocation, extensionContext);
    }

    /**
     * When a test does not run on JVM, its {@code @BeforeEach}/{@code @AfterEach} methods should not run
     * on JVM either. In TeaVM backends they are called by the generated entry point.
     */
    private void interceptLifecycleMethod(Invocation<Void> invocation, ExtensionContext extensionContext)
            throws Throwable {
        var testMethod = extensionContext.getTestMethod();
        if (testMethod.isPresent() && TeaVMTestExecutionSupport.isSkipJvm(testMethod.get(),
                extensionContext.getRequiredTestClass())) {
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
            throws Throwable {
        var method = invocationContext.getExecutable();
        if (!isParameterized(method)) {
            // Other kinds of test templates (e.g. @RepeatedTest) are not supported by TeaVM, run them on JVM only
            invocation.proceed();
            return;
        }
        intercept(invocation, method, extensionContext, getInvocationIndex(extensionContext));
    }

    private void intercept(Invocation<Void> invocation, Method method, ExtensionContext extensionContext,
            int invocationIndex) throws Throwable {
        var testClass = extensionContext.getRequiredTestClass();

        if (TeaVMTestExecutionSupport.isSkipJvm(method, testClass)) {
            invocation.skip();
        } else {
            invocation.proceed();
        }

        if (testClass.isAnnotationPresent(EachTestCompiledSeparately.class)) {
            var recorder = extensionContext.getStore(RUN_RECORDER_NAMESPACE).get(testClass,
                    TeaVMJupiterRunRecorder.class);
            if (invocationIndex < 0) {
                TeaVMTestExecutionSupport.runOnAllPlatforms(method, testClass, recorder::record);
            } else {
                // All invocations of a parameterized test share one compilation and one run, stored in the
                // context of the test template, which is the parent of the invocation's context
                var templateContext = extensionContext.getParent().orElse(extensionContext);
                var compilation = templateContext.getStore(NAMESPACE).getOrComputeIfAbsent(method,
                        m -> new ParameterizedCompilation(method, testClass, recorder),
                        ParameterizedCompilation.class);
                compilation.run(invocationIndex);
            }
        } else {
            var compilation = extensionContext.getStore(NAMESPACE).get(testClass, TeaVMWholeClassCompilation.class);
            if (invocationIndex < 0) {
                compilation.runMethod(method);
            } else {
                compilation.runInvocation(method, invocationIndex);
            }
        }
    }

    /**
     * Extracts zero-based index of the invocation of a test template from the unique id of the invocation
     * (which ends with {@code [test-template-invocation:#N]}, where N is one-based).
     */
    private static int getInvocationIndex(ExtensionContext context) {
        var id = context.getUniqueId();
        var start = id.lastIndexOf(INVOCATION_ID_PREFIX);
        if (start >= 0) {
            start += INVOCATION_ID_PREFIX.length();
            var end = id.indexOf(']', start);
            if (end > start) {
                try {
                    return Integer.parseInt(id.substring(start, end)) - 1;
                } catch (NumberFormatException e) {
                    // fall through
                }
            }
        }
        throw new IllegalStateException("Could not determine invocation index from unique id " + id);
    }

    private static List<Method> discoverTestMethods(Class<?> testClass) {
        var result = new ArrayList<Method>();
        var seen = new HashSet<String>();
        var cls = testClass;
        while (cls != null && cls != Object.class) {
            for (var method : cls.getDeclaredMethods()) {
                if (seen.add(method.getName()) && isTestMethod(method)) {
                    result.add(method);
                }
            }
            cls = cls.getSuperclass();
        }
        return result;
    }

    private static boolean isTestMethod(Method method) {
        return !Modifier.isPrivate(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && (method.isAnnotationPresent(Test.class) || isParameterized(method));
    }

    /**
     * Checks annotation by name, so that the extension works without {@code junit-jupiter-params}
     * on the classpath.
     */
    private static boolean isParameterized(Method method) {
        for (var annot : method.getAnnotations()) {
            if (annot.annotationType().getName().equals(PARAMETERIZED_TEST)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDisabled(Method method) {
        return method.isAnnotationPresent(Disabled.class)
                || method.getDeclaringClass().isAnnotationPresent(Disabled.class);
    }

    /**
     * Compiles and runs all invocations of a parameterized test (in {@link EachTestCompiledSeparately} mode)
     * once, on the first invocation, and reports results of every invocation from the cache.
     */
    private static final class ParameterizedCompilation {
        private final Method method;
        private final Class<?> testClass;
        private final TeaVMJupiterRunRecorder recorder;
        private InvocationResults results;
        private Throwable error;

        ParameterizedCompilation(Method method, Class<?> testClass, TeaVMJupiterRunRecorder recorder) {
            this.method = method;
            this.testClass = testClass;
            this.recorder = recorder;
        }

        void run(int invocationIndex) throws Throwable {
            runAll().check(invocationIndex);
        }

        private synchronized InvocationResults runAll() throws Throwable {
            if (error != null) {
                throw error;
            }
            if (results == null) {
                Map<TestPlatform, List<TestRun>> compiled;
                try {
                    JupiterArgumentsPlan plan = null;
                    if (TeaVMTestExecutionSupport.hasParticipatingPlatform(method, testClass)) {
                        plan = JupiterArgumentsAnalyzer.analyze(method, testClass);
                    }
                    compiled = TeaVMTestExecutionSupport.compileSingleMethod(method, testClass, plan);
                } catch (Throwable e) {
                    error = e;
                    throw e;
                }
                var runs = new ArrayList<TestRun>();
                for (var platform : TeaVMTestInfrastructure.platforms) {
                    var platformRuns = compiled.get(platform.getPlatform());
                    if (platformRuns != null) {
                        runs.addAll(platformRuns);
                    }
                }
                runs.forEach(recorder::record);
                results = InvocationResults.collect(runs, true);
            }
            return results;
        }
    }

    private static void rethrow(Throwable t) throws Exception {
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw (Exception) t;
    }
}
