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
package org.teavm.benchmarks.vm;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Measures conversions from floating point numbers to integers. All values fit into the target type,
 * so these benchmarks show the cost of saturation checks in the common case.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class NumericConversionBenchmark {
    private static final int SIZE = 1024;

    private double[] doubles = new double[SIZE];
    private float[] floats = new float[SIZE];
    private double[] largeDoubles = new double[SIZE];

    @Setup
    public void setup() {
        var random = new Random(123);
        for (var i = 0; i < SIZE; ++i) {
            doubles[i] = (random.nextDouble() - 0.5) * 2_000_000_000.0;
            floats[i] = (float) ((random.nextDouble() - 0.5) * 2_000_000.0);
            largeDoubles[i] = (random.nextDouble() - 0.5) * 1e18;
        }
    }

    @Benchmark
    public int doubleToInt() {
        var result = 0;
        for (var value : doubles) {
            result ^= (int) value;
        }
        return result;
    }

    @Benchmark
    public int floatToInt() {
        var result = 0;
        for (var value : floats) {
            result ^= (int) value;
        }
        return result;
    }

    /**
     * Clamps value with {@link Math#min} and {@link Math#max} before conversion, so that JS engine
     * may infer that value fits into int.
     */
    @Benchmark
    public int doubleToIntClamped() {
        var result = 0;
        for (var value : doubles) {
            result ^= (int) Math.max(-2_000_000_000.0, Math.min(2_000_000_000.0, value));
        }
        return result;
    }

    /**
     * Checks that value fits into range before conversion, so that JS engine may infer that value fits into int.
     */
    @Benchmark
    public int doubleToIntChecked() {
        var result = 0;
        for (var value : doubles) {
            if (value > -2_000_000_000.0 && value < 2_000_000_000.0) {
                result ^= (int) value;
            }
        }
        return result;
    }

    /**
     * Typical pattern: scaling a value and converting it to an index.
     */
    @Benchmark
    public int scaleToIndex() {
        var result = 0;
        for (var i = 0; i < SIZE; ++i) {
            result += (int) (i * 0.75 + 0.5);
        }
        return result;
    }

    @Benchmark
    public long doubleToLong() {
        var result = 0L;
        for (var value : largeDoubles) {
            result ^= (long) value;
        }
        return result;
    }

    @Benchmark
    public long doubleToLongClamped() {
        var result = 0L;
        for (var value : largeDoubles) {
            result ^= (long) Math.max(-1e18, Math.min(1e18, value));
        }
        return result;
    }

    @Benchmark
    public long doubleToLongChecked() {
        var result = 0L;
        for (var value : largeDoubles) {
            if (value > -1e18 && value < 1e18) {
                result ^= (long) value;
            }
        }
        return result;
    }

    @Benchmark
    public long floatToLong() {
        var result = 0L;
        for (var value : floats) {
            result ^= (long) value;
        }
        return result;
    }
}
