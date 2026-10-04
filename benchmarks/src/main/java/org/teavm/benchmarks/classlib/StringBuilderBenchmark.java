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
package org.teavm.benchmarks.classlib;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class StringBuilderBenchmark {
    private static final int COUNT = 256;

    /**
     * Kind of values to append:
     * <ul>
     *   <li>{@code short}: values with few significant digits, like 0.5 or 12.25;</li>
     *   <li>{@code random}: random values between 0 and 1000, requiring 15-17 significant digits;</li>
     *   <li>{@code exponent}: values that are formatted in scientific notation.</li>
     * </ul>
     */
    @Param({ "short", "random", "exponent" })
    public String kind;

    private double[] values;
    private StringBuilder sb = new StringBuilder();

    @Setup
    public void setup() {
        var random = new Random(42);
        values = new double[COUNT];
        for (var i = 0; i < COUNT; ++i) {
            switch (kind) {
                case "short":
                    values[i] = random.nextInt(10000) / 4.0;
                    break;
                case "random":
                    values[i] = random.nextDouble() * 1000;
                    break;
                case "exponent":
                    values[i] = random.nextDouble() * Math.pow(10, random.nextInt(600) - 300);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown kind: " + kind);
            }
        }
    }

    @Benchmark
    @OperationsPerInvocation(COUNT)
    public int appendDouble() {
        sb.setLength(0);
        for (var value : values) {
            sb.append(value);
        }
        return sb.length();
    }
}
