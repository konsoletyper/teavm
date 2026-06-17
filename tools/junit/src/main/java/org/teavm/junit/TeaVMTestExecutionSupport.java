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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.teavm.model.ClassHolder;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMTarget;

/**
 * Compiles test methods against every enabled TeaVM backend, either one method at a time or as a
 * whole class sharing one compiled artifact (gated by {@link EachTestCompiledSeparately}), and runs
 * the resulting {@link TestRun}s. This is the single implementation of that compilation machinery,
 * shared by both {@link TeaVMTestRunner} (JUnit 4/TestNG) and {@link TeaVMJupiterExtension}
 * (JUnit 5); only the outer "how do I report a run's outcome" glue differs between them (a JUnit 4
 * {@code RunNotifier} vs. a thrown exception that JUnit 5 surfaces on its own), so that part stays
 * in each front end.
 */
final class TeaVMTestExecutionSupport {
    private TeaVMTestExecutionSupport() {
    }

    static boolean hasParticipatingPlatform(Method method, Class<?> testClass) {
        if (TeaVMTestInfrastructure.outputDir == null) {
            return false;
        }
        for (var platform : TeaVMTestInfrastructure.platforms) {
            if (platform.isEnabled() && !platform.getConfigurations().isEmpty()
                    && isPlatformPresent(testClass, platform.getPlatform())
                    && isPlatformPresent(method, platform.getPlatform())) {
                return true;
            }
        }
        return false;
    }

    static boolean isSkipJvm(Method method, Class<?> testClass) {
        return method.isAnnotationPresent(SkipJVM.class) || testClass.isAnnotationPresent(SkipJVM.class);
    }

    /** Compiles, runs, and cleans up one test method against every enabled backend. */
    static void runOnAllPlatforms(Method method, Class<?> testClass) throws Throwable {
        runOnAllPlatforms(method, testClass, run -> { });
    }

    /**
     * Same as {@link #runOnAllPlatforms(Method, Class)}, but also calls {@code onCompiled} for
     * every compiled run, before it's executed — so a caller can record it (e.g. for a
     * {@code tests.json} descriptor) without duplicating this compile/run/cleanup loop.
     */
    static void runOnAllPlatforms(Method method, Class<?> testClass, Consumer<TestRun> onCompiled) throws Throwable {
        var compiled = compileSingleMethod(method, testClass, null);
        for (var platform : TeaVMTestInfrastructure.platforms) {
            var runs = compiled.get(platform.getPlatform());
            if (runs == null) {
                continue;
            }
            var strategy = TeaVMTestInfrastructure.runners.get(platform.getPlatform());
            if (strategy == null) {
                continue;
            }
            for (var run : runs) {
                onCompiled.accept(run);
                strategy.runTest(run);
                strategy.cleanup();
            }
        }
    }

    /**
     * Compiles one test method against every enabled/applicable backend, mirroring
     * {@code TeaVMTestRunner.prepareCompiledTest}. Returns the compiled {@link TestRun}s (one per
     * configuration) grouped by platform, ready to be submitted by the caller — which also decides
     * how to report a compile failure (this method itself just throws).
     *
     * <p>{@code plan}, if not null, describes the arguments of a Jupiter parameterized test.
     */
    @SuppressWarnings("unchecked")
    static Map<TestPlatform, List<TestRun>> compileSingleMethod(Method method, Class<?> testClass,
            JupiterArgumentsPlan plan) throws Throwable {
        var result = new LinkedHashMap<TestPlatform, List<TestRun>>();
        if (TeaVMTestInfrastructure.outputDir == null) {
            return result;
        }

        var reference = new MethodReference(method.getDeclaringClass().getName(), getDescriptor(method));

        for (var platform : TeaVMTestInfrastructure.platforms) {
            if (!platform.isEnabled() || !isPlatformPresent(testClass, platform.getPlatform())
                    || !isPlatformPresent(method, platform.getPlatform())) {
                continue;
            }

            var outputPath = getOutputPath(method, testClass, platform);
            var runs = new ArrayList<TestRun>();
            for (var configuration : platform.getConfigurations()) {
                var castPlatform = (TestPlatformSupport<TeaVMTarget>) platform;
                var castConfig = (TeaVMTestConfiguration<TeaVMTarget>) configuration;

                var compileResult = castPlatform.compile(singleTestEntryPoint(method, testClass, plan), "test",
                        castConfig, outputPath, method);
                if (!compileResult.success) {
                    platform.additionalSingleTestOutput(outputPath, configuration, reference);
                    throw compileResult.throwable != null
                            ? compileResult.throwable
                            : new AssertionError(compileResult.errorMessage);
                }

                var run = new TestRun(generateName(method.getName(), configuration), method, null);
                run.group = new TestRunGroup(compileResult.file.getParentFile(), compileResult.file.getName(),
                        platform.getPlatform(), isModule(method));
                runs.add(run);

                platform.additionalSingleTestOutput(outputPath, configuration, reference);
            }
            platform.additionalOutputForAllConfigurations(outputPath, method);
            result.put(platform.getPlatform(), runs);
        }

        return result;
    }

    /**
     * Compiles every method in {@code methods} into a single artifact per platform/configuration,
     * mirroring {@code TeaVMTestRunner.compileClassForPlatform}/{@code wholeClass}. Returns, for
     * each platform that has at least one participating method, the compiled {@link TestRun}s
     * grouped by method — ready to be submitted one method at a time as each test method actually
     * executes, via {@link #runWholeClassMethod}.
     *
     * <p>{@code ignored} decides which methods to leave out of the compiled dispatch table (e.g.
     * JUnit 4's {@code @Ignore}/TestNG's {@code @Ignore} for {@link TeaVMTestRunner}, Jupiter's
     * {@code @Disabled} for {@link TeaVMJupiterExtension}) — it's a caller-supplied predicate
     * rather than a fixed annotation check here, since the two front ends recognize different,
     * framework-specific annotations, and this class must stay loadable on a classpath that has
     * only one of the two frameworks present.
     *
     * <p>{@code plans} describes arguments of Jupiter parameterized tests among {@code methods}.
     */
    @SuppressWarnings("unchecked")
    static Map<TestPlatform, Map<Method, List<TestRun>>> compileWholeClass(List<Method> methods, Class<?> testClass,
            Predicate<Method> ignored, Map<Method, JupiterArgumentsPlan> plans) throws Throwable {
        var result = new LinkedHashMap<TestPlatform, Map<Method, List<TestRun>>>();
        if (TeaVMTestInfrastructure.outputDir == null) {
            return result;
        }

        var isModule = testClass.isAnnotationPresent(JsModuleTest.class);
        for (var platform : TeaVMTestInfrastructure.platforms) {
            if (!platform.isEnabled() || !hasParticipatingMethod(methods, testClass, platform.getPlatform())) {
                continue;
            }

            var path = getOutputPathForClass(testClass, platform);
            var methodRuns = new LinkedHashMap<Method, List<TestRun>>();
            for (var configuration : platform.getConfigurations()) {
                var castPlatform = (TestPlatformSupport<TeaVMTarget>) platform;
                var castConfig = (TeaVMTestConfiguration<TeaVMTarget>) configuration;
                var runs = new ArrayList<TestRun>();

                var compileResult = castPlatform.compile(wholeClassEntryPoint(methods, platform.getPlatform(),
                        configuration, testClass, runs, ignored, plans), "classTest", castConfig, path, testClass);
                if (!compileResult.success) {
                    platform.additionalOutput(path, configuration);
                    throw compileResult.throwable != null
                            ? compileResult.throwable
                            : new AssertionError(compileResult.errorMessage);
                }

                var group = new TestRunGroup(path, compileResult.file.getName(), platform.getPlatform(), isModule);
                for (var run : runs) {
                    run.group = group;
                    methodRuns.computeIfAbsent(run.getMethod(), m -> new ArrayList<>()).add(run);
                    platform.additionalOutput(path, new File(path, run.getMethod().getName()), configuration,
                            MethodReference.parse(run.getArgument()));
                }
                platform.additionalOutput(path, configuration);
            }
            for (var method : methodRuns.keySet()) {
                platform.additionalOutputForAllConfigurations(path, method);
            }
            result.put(platform.getPlatform(), methodRuns);
        }

        return result;
    }

    /** Submits the pre-compiled runs (from {@link #compileWholeClass}) for one method. */
    static void runWholeClassMethod(Map<TestPlatform, Map<Method, List<TestRun>>> compilation, Method method)
            throws Throwable {
        for (var platform : TeaVMTestInfrastructure.platforms) {
            var methodRuns = compilation.get(platform.getPlatform());
            var runs = methodRuns != null ? methodRuns.get(method) : null;
            if (runs == null) {
                continue;
            }
            var strategy = TeaVMTestInfrastructure.runners.get(platform.getPlatform());
            if (strategy == null) {
                continue;
            }
            for (var run : runs) {
                strategy.runTest(run);
            }
        }
    }

    /** Called once, when the whole class is done, to release resources (e.g. close the browser tab). */
    static void cleanupWholeClass(Map<TestPlatform, Map<Method, List<TestRun>>> compilation) {
        for (var platformKind : compilation.keySet()) {
            var strategy = TeaVMTestInfrastructure.runners.get(platformKind);
            if (strategy != null) {
                strategy.cleanup();
            }
        }
    }

    /**
     * Writes one {@code tests.json} descriptor per platform present in {@code runs}, listing every
     * compiled run of {@code testClass} on that platform (consumed by external tooling that drives
     * the compiled artifacts outside this JVM). Also appends to the {@code teavm.junit.c.classList}
     * file, if configured, for {@link TestPlatform#C} runs.
     */
    static void writeRunsDescriptor(Class<?> testClass, List<TestRun> runs) {
        if (runs.isEmpty()) {
            return;
        }

        var runsByPlatform = new LinkedHashMap<TestPlatform, List<TestRun>>();
        for (var run : runs) {
            runsByPlatform.computeIfAbsent(run.getGroup().getKind(), k -> new ArrayList<>()).add(run);
        }

        for (var platform : TeaVMTestInfrastructure.platforms) {
            var platformRuns = runsByPlatform.get(platform.getPlatform());
            if (platformRuns != null) {
                writeRunsDescriptor(testClass, platform, platformRuns);
            }
        }
    }

    private static void writeRunsDescriptor(Class<?> testClass, TestPlatformSupport<?> platform,
            List<TestRun> runs) {
        var outputDir = getOutputPathForClass(testClass, platform);
        outputDir.mkdirs();
        var descriptorFile = new File(outputDir, "tests.json");
        try (var output = new FileOutputStream(descriptorFile);
                var bufferedOutput = new BufferedOutputStream(output);
                Writer writer = new OutputStreamWriter(bufferedOutput)) {
            writer.write("[\n");
            var first = true;
            for (var run : runs) {
                if (!first) {
                    writer.write(",\n");
                }
                first = false;
                writer.write("  {\n");
                writer.write("    \"baseDir\": ");
                writeJsonString(writer, run.getGroup().getBaseDirectory().getAbsolutePath().replace('\\', '/'));
                writer.write(",\n");
                writer.write("    \"fileName\": ");
                writeJsonString(writer, run.getGroup().getFileName());
                writer.write(",\n");
                writer.write("    \"kind\": \"" + run.getGroup().getKind().name() + "\"");
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

        var classListFilePath = System.getProperty(PropertyNames.C_CLASS_LIST_FILE);
        if (classListFilePath != null && platform.getPlatform() == TestPlatform.C) {
            try (var appender = new FileWriter(classListFilePath, StandardCharsets.UTF_8, true)) {
                appender.write(outputDir.getAbsolutePath() + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void writeJsonString(Writer writer, String s) throws IOException {
        writer.write('"');
        for (var i = 0; i < s.length(); ++i) {
            var c = s.charAt(i);
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

    private static boolean hasParticipatingMethod(List<Method> methods, Class<?> testClass, TestPlatform platform) {
        return isPlatformPresent(testClass, platform) && methods.stream().anyMatch(m -> isPlatformPresent(m, platform));
    }

    private static Consumer<TeaVM> wholeClassEntryPoint(List<Method> methods, TestPlatform platform,
            TeaVMTestConfiguration<?> configuration, Class<?> testClass, List<TestRun> runs,
            Predicate<Method> ignored, Map<Method, JupiterArgumentsPlan> plans) {
        return vm -> {
            var properties = new Properties();
            applyProperties(testClass, properties);
            vm.setProperties(properties);

            var methodReferences = new ArrayList<MethodReference>();
            var referencePlans = new HashMap<MethodReference, JupiterArgumentsPlan>();
            for (var method : methods) {
                if (!isPlatformPresent(method, platform) || ignored.test(method)) {
                    continue;
                }
                var classHolder = TeaVMTestInfrastructure.classSource.get(method.getDeclaringClass().getName());
                var methodHolder = classHolder.getMethod(getDescriptor(method));
                methodReferences.add(methodHolder.getReference());
                var plan = plans.get(method);
                if (plan != null) {
                    referencePlans.put(methodHolder.getReference(), plan);
                }
                var run = new TestRun(generateName(method.getName(), configuration), method,
                        methodHolder.getReference().toString());
                runs.add(run);
            }
            new TestEntryPointTransformerForWholeClass(methodReferences, testClass.getName(), referencePlans)
                    .install(vm);
        };
    }

    static File getOutputPathForClass(Class<?> testClass, TestPlatformSupport<?> platform) {
        var path = TeaVMTestInfrastructure.outputDir;
        path = new File(new File(path, platform.getPath()), testClass.getName().replace('.', '/'));
        path.mkdirs();
        return path;
    }

    private static Consumer<TeaVM> singleTestEntryPoint(Method method, Class<?> testClass,
            JupiterArgumentsPlan plan) {
        ClassHolder classHolder = TeaVMTestInfrastructure.classSource.get(method.getDeclaringClass().getName());
        MethodHolder methodHolder = classHolder.getMethod(getDescriptor(method));
        var plans = plan != null
                ? Map.of(methodHolder.getReference(), plan)
                : Map.<MethodReference, JupiterArgumentsPlan>of();

        return vm -> {
            var properties = new Properties();
            applyProperties(method.getDeclaringClass(), properties);
            vm.setProperties(properties);
            new TestEntryPointTransformerForSingleMethod(methodHolder.getReference(), testClass.getName(), plans)
                    .install(vm);
        };
    }

    static void applyProperties(Class<?> cls, Properties result) {
        if (cls.getSuperclass() != null) {
            applyProperties(cls.getSuperclass(), result);
        }
        var properties = cls.getAnnotation(TeaVMProperties.class);
        if (properties != null) {
            for (var property : properties.value()) {
                result.setProperty(property.key(), property.value());
            }
        }
    }

    static boolean isModule(Method method) {
        return method.isAnnotationPresent(JsModuleTest.class)
                || method.getDeclaringClass().isAnnotationPresent(JsModuleTest.class);
    }

    static String generateName(String baseName, TeaVMTestConfiguration<?> configuration) {
        var suffix = configuration.getSuffix();
        return suffix.isEmpty() ? baseName : baseName + " (" + suffix + ")";
    }

    static File getOutputPath(Method method, Class<?> testClass, TestPlatformSupport<?> platform) {
        var path = TeaVMTestInfrastructure.outputDir;
        path = new File(new File(path, platform.getPath()), testClass.getName().replace('.', '/'));
        path = new File(path, method.getName());
        path.mkdirs();
        return path;
    }

    static MethodDescriptor getDescriptor(Method method) {
        ValueType[] signature = Stream.concat(Arrays.stream(method.getParameterTypes()).map(ValueType::parse),
                Stream.of(ValueType.parse(method.getReturnType())))
                .toArray(ValueType[]::new);
        return new MethodDescriptor(method.getName(), signature);
    }

    static boolean isPlatformPresent(AnnotatedElement declaration, TestPlatform platform) {
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
}
