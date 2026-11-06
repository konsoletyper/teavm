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

import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.vm.BuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMPhase;
import org.teavm.vm.TeaVMProgressFeedback;
import org.teavm.vm.TeaVMProgressListener;

/**
 * Runs builds and accumulates time spent in dependency analysis, optimizations and code generation,
 * so that benchmarks can print breakdown by phases after each iteration.
 */
final class PhaseTimes {
    private long builds;
    private long dependencyTime;
    private long optimizationTime;
    private long emitTime;

    void reset() {
        builds = 0;
        dependencyTime = 0;
        optimizationTime = 0;
        emitTime = 0;
    }

    void report() {
        if (builds == 0) {
            return;
        }
        System.out.printf(" [avg ms: dependencies %.2f, optimizations %.2f, emit %.2f]",
                dependencyTime / 1e6 / builds, optimizationTime / 1e6 / builds, emitTime / 1e6 / builds);
    }

    /**
     * Builds the program, recording phase times.
     *
     * @param target the target that was passed to {@link org.teavm.vm.TeaVMBuilder}
     *               when {@code vm} was created.
     */
    void build(TeaVM vm, EmitTimingTarget target, BuildTarget buildTarget, String outputName) {
        var phaseListener = new PhaseListener();
        vm.setProgressListener(phaseListener);
        var start = System.nanoTime();
        vm.build(buildTarget, outputName);

        if (phaseListener.compilingStart != 0 && target.emitEnd != 0) {
            ++builds;
            dependencyTime += phaseListener.compilingStart - start;
            optimizationTime += target.emitStart - phaseListener.compilingStart;
            emitTime += target.emitEnd - target.emitStart;
        }
    }

    /**
     * Prints problems reported by compiler.
     *
     * @return number of severe problems
     */
    static int reportProblems(TeaVM vm) {
        var problems = vm.getProblemProvider().getProblems();
        var severeCount = vm.getProblemProvider().getSevereProblems().size();
        if (problems.isEmpty()) {
            return 0;
        }
        var consumer = new DefaultProblemTextConsumer();
        System.err.println("WARNING: compiler reported " + problems.size() + " problem(s), "
                + severeCount + " of them severe:");
        for (var problem : problems) {
            consumer.clear();
            problem.render(consumer);
            System.err.println("  " + problem.getSeverity() + ": " + consumer.getText());
        }
        return severeCount;
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
