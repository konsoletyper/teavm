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
package org.teavm.compilerbenchmarks;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.teavm.backend.wasm.WasmDebugInfoLevel;
import org.teavm.backend.wasm.WasmDebugInfoLocation;
import org.teavm.backend.wasm.WasmGCTarget;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.parsing.ClasspathResourceProvider;
import org.teavm.vm.MemoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMOptimizationLevel;

/**
 * Measures time of compilation of a mid-sized real-life project: TeaVM compiler itself, as it's compiled
 * to Wasm GC by {@code :bootstrap-compiler:junit-compiler} module. Compiler settings are the same as in
 * {@code org.teavm.compiler.bootstrap.junit.tools.Compiler}.
 *
 * <p>Compiled program is taken from a separate class loader, which classpath is passed in
 * {@value #CLASSPATH_PROPERTY} system property (set by Gradle), so that classes and TeaVM plugins
 * of the compiled program don't affect other benchmarks.
 *
 * <p>When {@link #reuseParsedClasses} is true, classes parsed by the first build are reused by subsequent
 * builds, so steady state measures dependency analysis, optimizations and code generation.
 * Otherwise, every build parses classes from scratch, like a clean non-incremental build does.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 10)
@Measurement(iterations = 5, time = 10)
@Fork(value = 1, jvmArgsAppend = "-Xmx4g")
public class  BootstrapCompilerBenchmark {
    static final String CLASSPATH_PROPERTY = "teavm.compilerBenchmarks.bootstrapCompilerClasspath";
    private static final String MAIN_CLASS = "org.teavm.compiler.bootstrap.junit.TestCompiler";

    @Param("ADVANCED")
    public TeaVMOptimizationLevel optimization;

    @Param("false")
    public boolean reuseParsedClasses;

    private URLClassLoader classLoader;
    private ReferenceCache referenceCache;
    private ClasspathResourceProvider resourceProvider;
    private ClassHolderSource classSource;

    private final PhaseTimes phaseTimes = new PhaseTimes();

    @Setup(Level.Trial)
    public void setup() {
        classLoader = new URLClassLoader(readClasspath(), BootstrapCompilerBenchmark.class.getClassLoader());
        resourceProvider = new ClasspathResourceProvider(classLoader);
        var vm = compile();
        if (PhaseTimes.reportProblems(vm) > 0) {
            throw new IllegalStateException("Compiler reported severe problems, benchmark is meaningless");
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        classLoader.close();
    }

    @Setup(Level.Iteration)
    public void resetPhaseTimes() {
        phaseTimes.reset();
    }

    @TearDown(Level.Iteration)
    public void reportPhaseTimes() {
        phaseTimes.report();
    }

    @Benchmark
    public TeaVM bootstrapCompiler() {
        return compile();
    }

    private TeaVM compile() {
        if (classSource == null || !reuseParsedClasses) {
            referenceCache = new ReferenceCache();
            classSource = new PreOptimizingClassHolderSource(new ClasspathClassHolderSource(resourceProvider,
                    referenceCache, classLoader));
        }

        var target = new WasmGCTarget();
        target.setObfuscated(false);
        target.setStrict(true);
        target.setDebugInfo(true);
        target.setDebugInfoLevel(WasmDebugInfoLevel.FULL);
        target.setDebugInfoLocation(WasmDebugInfoLocation.EMBEDDED);
        var timingTarget = new EmitTimingTarget(target);
        var vm = new TeaVMBuilder(timingTarget)
                .setClassLoader(classLoader)
                .setClassSource(classSource)
                .setResourceProvider(resourceProvider)
                .setReferenceCache(referenceCache)
                .setObfuscated(false)
                .setStrict(true)
                .build();
        vm.setOptimizationLevel(optimization);
        vm.installPlugins();
        vm.setEntryPoint(MAIN_CLASS);
        phaseTimes.build(vm, timingTarget, new MemoryBuildTarget(), "compiler.wasm");
        return vm;
    }

    private static URL[] readClasspath() {
        var classpath = System.getProperty(CLASSPATH_PROPERTY);
        if (classpath == null || classpath.isEmpty()) {
            throw new IllegalStateException("System property " + CLASSPATH_PROPERTY + " is not set. "
                    + "Run benchmark via :compiler-benchmarks:jmh Gradle task");
        }
        var urls = new ArrayList<URL>();
        for (var entry : classpath.split(File.pathSeparator)) {
            if (entry.isEmpty()) {
                continue;
            }
            try {
                urls.add(new File(entry).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid classpath entry: " + entry, e);
            }
        }
        return urls.toArray(new URL[0]);
    }
}
