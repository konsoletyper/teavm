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
import java.util.HashSet;
import java.util.List;
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
 * model: plain {@code @Test} methods, instance-level {@code @BeforeEach}/{@code @AfterEach}
 * (recognized directly off the compiled class by {@link TestEntryPointTransformer}, the same way
 * JUnit 4's {@code @Before}/{@code @After} already are), and TeaVM's own annotations
 * ({@link SkipPlatform}, {@link OnlyPlatform}, {@link SkipJVM}, {@link JsModuleTest},
 * {@link TeaVMProperties}). There's no support for the general {@code @ExtendWith} extension
 * mechanism, {@code @Nested} classes, dynamic tests, parameterized tests, or
 * {@code @BeforeAll}/{@code @AfterAll} — none of those have a sound ahead-of-time-compiled
 * equivalent (or, in the case of other extensions, would require re-executing arbitrary reflective
 * JVM code inside the compiled artifact).
 */
public final class TeaVMJupiterExtension implements InvocationInterceptor, ExecutionCondition, BeforeAllCallback {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TeaVMJupiterExtension.class);
    private static final ExtensionContext.Namespace RUN_RECORDER_NAMESPACE =
            ExtensionContext.Namespace.create(TeaVMJupiterExtension.class, "runs");

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
            var runsByPlatform = TeaVMTestExecutionSupport.compileWholeClass(methods, testClass,
                    TeaVMJupiterExtension::isDisabled);
            for (var methodRuns : runsByPlatform.values()) {
                for (var runs : methodRuns.values()) {
                    runs.forEach(recorder::record);
                }
            }
            context.getStore(NAMESPACE).put(testClass, new TeaVMWholeClassCompilation(runsByPlatform));
        } catch (Throwable t) {
            rethrow(t);
        }
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        var method = invocationContext.getExecutable();
        var testClass = extensionContext.getRequiredTestClass();

        if (TeaVMTestExecutionSupport.isSkipJvm(method, testClass)) {
            invocation.skip();
        } else {
            invocation.proceed();
        }

        if (testClass.isAnnotationPresent(EachTestCompiledSeparately.class)) {
            var recorder = extensionContext.getStore(RUN_RECORDER_NAMESPACE).get(testClass,
                    TeaVMJupiterRunRecorder.class);
            TeaVMTestExecutionSupport.runOnAllPlatforms(method, testClass, recorder::record);
        } else {
            var compilation = extensionContext.getStore(NAMESPACE).get(testClass, TeaVMWholeClassCompilation.class);
            compilation.runMethod(method);
        }
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
                && method.isAnnotationPresent(Test.class);
    }

    private static boolean isDisabled(Method method) {
        return method.isAnnotationPresent(Disabled.class)
                || method.getDeclaringClass().isAnnotationPresent(Disabled.class);
    }

    private static void rethrow(Throwable t) throws Exception {
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw (Exception) t;
    }
}
