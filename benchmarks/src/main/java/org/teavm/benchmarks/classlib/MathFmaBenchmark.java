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
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Measures {@link Math#fma}. Every benchmark performs {@link #SIZE} operations.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class MathFmaBenchmark {
    private static final int SIZE = 1024;

    private double[] a = new double[SIZE];
    private double[] b = new double[SIZE];
    private double[] c = new double[SIZE];
    private double[] cWide = new double[SIZE];
    private float[] af = new float[SIZE];
    private float[] bf = new float[SIZE];
    private float[] cf = new float[SIZE];

    @Setup
    public void setup() {
        var random = new Random(123);
        for (var i = 0; i < SIZE; ++i) {
            a[i] = random.nextDouble() * 2 - 1;
            b[i] = random.nextDouble() * 2 - 1;
            c[i] = random.nextDouble() * 2 - 1;
            // Addend with exponent far from the product's one
            cWide[i] = (random.nextDouble() * 2 - 1) * Math.scalb(1.0, random.nextInt(400) - 200);
            af[i] = (float) a[i];
            bf[i] = (float) b[i];
            cf[i] = (float) c[i];
        }
    }

    /**
     * Operands of similar magnitude, the most typical case.
     */
    @Benchmark
    public double fmaDouble() {
        var result = 0.0;
        for (var i = 0; i < SIZE; ++i) {
            result += Math.fma(a[i], b[i], c[i]);
        }
        return result;
    }

    /**
     * Addend is much smaller or much larger than the product.
     */
    @Benchmark
    public double fmaDoubleWideExponents() {
        var result = 0.0;
        for (var i = 0; i < SIZE; ++i) {
            result += Math.fma(a[i], b[i], cWide[i]);
        }
        return result;
    }

    /**
     * Cancellation: addend is close to negated product, so result keeps low bits of the product.
     */
    @Benchmark
    public double fmaDoubleCancellation() {
        var result = 0.0;
        for (var i = 0; i < SIZE; ++i) {
            var p = a[i] * b[i];
            result += Math.fma(a[i], b[i], -p);
        }
        return result;
    }

    /**
     * Dependent chain of operations, i.e. polynomial evaluation by Horner's method.
     */
    @Benchmark
    public double fmaDoubleHorner() {
        var x = 0.7;
        var result = 0.0;
        for (var i = 0; i < SIZE; ++i) {
            result = Math.fma(result, x, a[i]);
        }
        return result;
    }

    @Benchmark
    public float fmaFloat() {
        var result = 0f;
        for (var i = 0; i < SIZE; ++i) {
            result += Math.fma(af[i], bf[i], cf[i]);
        }
        return result;
    }

    /**
     * Baseline: non-fused operation.
     */
    @Benchmark
    public double mulAddDouble() {
        var result = 0.0;
        for (var i = 0; i < SIZE; ++i) {
            result += a[i] * b[i] + c[i];
        }
        return result;
    }
}
