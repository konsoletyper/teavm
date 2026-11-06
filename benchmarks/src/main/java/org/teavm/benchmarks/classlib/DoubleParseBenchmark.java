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
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class DoubleParseBenchmark {
    private static final int COUNT = 256;

    /**
     * Kind of strings to parse:
     * <ul>
     *   <li>{@code short}: numbers with few significant digits, like 0.5 or 12.25;</li>
     *   <li>{@code random}: numbers between 0 and 1000 with 17 significant digits;</li>
     *   <li>{@code exponent}: numbers with 17 significant digits in scientific notation.</li>
     * </ul>
     * Strings are generated from random digits rather than by {@code Double.toString}, so that
     * the input does not depend on implementation of double formatting.
     */
    @Param({ "short", "random", "exponent" })
    public String kind;

    private String[] values;

    @Setup
    public void setup() {
        var random = new Random(42);
        values = new String[COUNT];
        for (var i = 0; i < COUNT; ++i) {
            var sb = new StringBuilder();
            switch (kind) {
                case "short":
                    sb.append(random.nextInt(2500)).append('.').append(random.nextInt(4) * 25);
                    break;
                case "random": {
                    var intDigits = 1 + random.nextInt(3);
                    appendDigits(sb, random, intDigits);
                    sb.append('.');
                    appendDigits(sb, random, 17 - intDigits);
                    break;
                }
                case "exponent":
                    appendDigits(sb, random, 1);
                    sb.append('.');
                    appendDigits(sb, random, 16);
                    sb.append('E').append(random.nextInt(600) - 300);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown kind: " + kind);
            }
            values[i] = sb.toString();
        }
    }

    private static void appendDigits(StringBuilder sb, Random random, int count) {
        sb.append((char) ('1' + random.nextInt(9)));
        for (var i = 1; i < count; ++i) {
            sb.append((char) ('0' + random.nextInt(10)));
        }
    }

    @Benchmark
    @OperationsPerInvocation(COUNT)
    public double parseDouble() {
        var result = 0.0;
        for (var value : values) {
            result += Double.parseDouble(value);
        }
        return result;
    }
}
