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
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.ReferenceCache;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.parsing.ClasspathResourceProvider;
import org.teavm.vm.MemoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import org.teavm.vm.TeaVMOptimizationLevel;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

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

    private long builds;
    private long dependencyTime;
    private long optimizationTime;
    private long emitTime;

    @Setup(Level.Trial)
    public void setup() {
        classLoader = WasmGCCompilerBenchmark.class.getClassLoader();
        referenceCache = new ReferenceCache();
        resourceProvider = new ClasspathResourceProvider(classLoader);
        classSource = new PreOptimizingClassHolderSource(new ClasspathClassHolderSource(resourceProvider,
                referenceCache, classLoader));
        var vm = compile();
        reportProblems(vm);
    }

    @Setup(Level.Iteration)
    public void resetPhaseTimes() {
        builds = 0;
        dependencyTime = 0;
        optimizationTime = 0;
        emitTime = 0;
    }

    @TearDown(Level.Iteration)
    public void reportPhaseTimes() {
        if (builds == 0) {
            return;
        }
        System.out.printf(" [avg ms: dependencies %.2f, optimizations %.2f, emit %.2f]",
                dependencyTime / 1e6 / builds, optimizationTime / 1e6 / builds, emitTime / 1e6 / builds);
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
        var phaseListener = new PhaseListener();
        vm.setProgressListener(phaseListener);
        vm.installPlugins();
        vm.setEntryPoint(BufferedInputStreamProgram.class.getName());
        var start = System.nanoTime();
        vm.build(new MemoryBuildTarget(), "classes.wasm");

        if (phaseListener.compilingStart != 0 && timingTarget.emitEnd != 0) {
            ++builds;
            dependencyTime += phaseListener.compilingStart - start;
            optimizationTime += timingTarget.emitStart - phaseListener.compilingStart;
            emitTime += timingTarget.emitEnd - timingTarget.emitStart;
        }
        return vm;
    }

    private static void reportProblems(TeaVM vm) {
        var problems = vm.getProblemProvider().getProblems();
        if (problems.isEmpty()) {
            return;
        }
        var consumer = new DefaultProblemTextConsumer();
        System.err.println("WARNING: compiler reported " + problems.size() + " problem(s), "
                + vm.getProblemProvider().getSevereProblems().size() + " of them severe:");
        for (var problem : problems) {
            consumer.clear();
            problem.render(consumer);
            System.err.println("  " + problem.getSeverity() + ": " + consumer.getText());
        }
    }

    private static class PhaseListener implements TeaVMProgressListener {
        long compilingStart;

        @Override
        public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
            if (phase == TeaVMPhase.COMPILING) {
                compilingStart = System.nanoTime();
            }
            return TeaVMProgressFeedback.CONTINUE;
        }

        @Override
        public TeaVMProgressFeedback progressReached(int progress) {
            return TeaVMProgressFeedback.CONTINUE;
        }
    }
}
