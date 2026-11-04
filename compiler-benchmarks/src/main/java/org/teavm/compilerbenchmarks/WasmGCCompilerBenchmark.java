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
import org.teavm.backend.wasm.WasmGCTarget;
import org.teavm.compilerbenchmarks.programs.BufferedInputStreamProgram;
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
 * Measures time of full compilation of a small program by TeaVM with Wasm GC backend.
 * Parsed classes are cached between invocations, so steady state measures dependency analysis,
 * optimizations and code generation. Breakdown by these phases is printed after each iteration.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 3)
@Measurement(iterations = 10, time = 3)
@Fork(value = 1, jvmArgsAppend = "-Xmx4g")
public class WasmGCCompilerBenchmark {
    @Param("ADVANCED")
    public TeaVMOptimizationLevel optimization;

    private ClassLoader classLoader;
    private ReferenceCache referenceCache;
    private ClasspathResourceProvider resourceProvider;
    private ClassHolderSource classSource;

    private final PhaseTimes phaseTimes = new PhaseTimes();

    @Setup(Level.Trial)
    public void setup() {
        classLoader = WasmGCCompilerBenchmark.class.getClassLoader();
        referenceCache = new ReferenceCache();
        resourceProvider = new ClasspathResourceProvider(classLoader);
        classSource = new PreOptimizingClassHolderSource(new ClasspathClassHolderSource(resourceProvider,
                referenceCache, classLoader));
        var vm = compile();
        PhaseTimes.reportProblems(vm);
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
    public TeaVM bufferedInputStream() {
        return compile();
    }

    private TeaVM compile() {
        var target = new WasmGCTarget();
        target.setObfuscated(true);
        var timingTarget = new EmitTimingTarget(target);
        var vm = new TeaVMBuilder(timingTarget)
                .setClassLoader(classLoader)
                .setClassSource(classSource)
                .setResourceProvider(resourceProvider)
                .setReferenceCache(referenceCache)
                .build();
        vm.setOptimizationLevel(optimization);
        vm.installPlugins();
        vm.setEntryPoint(BufferedInputStreamProgram.class.getName());
        phaseTimes.build(vm, timingTarget, new MemoryBuildTarget(), "classes.wasm");
        return vm;
    }
}
