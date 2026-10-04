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
 * Measures typical operations on {@code long} values.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class LongBenchmark {
    private static final int SIZE = 1024;

    private long[] longs = new long[SIZE];
    private int[] ints = new int[SIZE];
    private long seed;

    @Setup
    public void setup() {
        var random = new Random(123);
        for (var i = 0; i < SIZE; ++i) {
            longs[i] = random.nextLong();
            ints[i] = random.nextInt();
        }
        seed = random.nextLong() | 1;
    }

    /**
     * Xorshift random number generator: shifts by constant amounts and bitwise operations.
     */
    @Benchmark
    public long xorShift() {
        var x = seed;
        for (var i = 0; i < SIZE; ++i) {
            x ^= x << 13;
            x ^= x >>> 7;
            x ^= x << 17;
        }
        return x;
    }

    /**
     * SplitMix64 mixing function: multiplication by constants and shifts by constant amounts.
     */
    @Benchmark
    public long splitMix() {
        var result = 0L;
        var z = seed;
        for (var i = 0; i < SIZE; ++i) {
            z += 0x9E3779B97F4A7C15L;
            var v = z;
            v = (v ^ (v >>> 30)) * 0xBF58476D1CE4E5B9L;
            v = (v ^ (v >>> 27)) * 0x94D049BB133111EBL;
            result ^= v ^ (v >>> 31);
        }
        return result;
    }

    @Benchmark
    public int hashCodes() {
        var result = 0;
        for (var value : longs) {
            result = result * 31 + Long.hashCode(value);
        }
        return result;
    }

    @Benchmark
    public int longToInt() {
        var result = 0;
        for (var value : longs) {
            result ^= (int) value;
            result += (int) (value >> 32);
        }
        return result;
    }

    @Benchmark
    public double longToDouble() {
        var result = 0.0;
        for (var value : longs) {
            result += value;
        }
        return result;
    }

    @Benchmark
    public long intToLong() {
        var result = 0L;
        for (var value : ints) {
            result += value;
        }
        return result;
    }

    @Benchmark
    public long sum() {
        var result = 0L;
        for (var value : longs) {
            result += value;
        }
        return result;
    }

    @Benchmark
    public long variableShift() {
        var result = 0L;
        for (var i = 0; i < SIZE; ++i) {
            result ^= longs[i] << ints[i];
        }
        return result;
    }
}
