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

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Measures uncontended {@code synchronized} blocks. Scores are per single lock acquisition.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class MonitorBenchmark {
    private static final int SIZE = 256;

    private Object lock;
    private Object[] locks;
    private int counter;

    @Setup
    public void setup() {
        lock = new Object();
        locks = new Object[SIZE];
        for (var i = 0; i < SIZE; ++i) {
            locks[i] = new Object();
        }
    }

    /**
     * Locks the same object repeatedly.
     */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int sameObject() {
        var lock = this.lock;
        for (var i = 0; i < SIZE; ++i) {
            synchronized (lock) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Locks a different object each time, one at a time.
     */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int distinctObjects() {
        var locks = this.locks;
        for (var i = 0; i < SIZE; ++i) {
            synchronized (locks[i]) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Locks two different objects, one inside another.
     */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int nested() {
        var lock = this.lock;
        var locks = this.locks;
        for (var i = 0; i < SIZE; i += 2) {
            synchronized (lock) {
                synchronized (locks[i]) {
                    counter++;
                }
            }
        }
        return counter;
    }

    /**
     * Reentrant locking of the same object.
     */
    @Benchmark
    @OperationsPerInvocation(SIZE)
    public int reentrant() {
        var lock = this.lock;
        for (var i = 0; i < SIZE; i += 2) {
            synchronized (lock) {
                synchronized (lock) {
                    counter++;
                }
            }
        }
        return counter;
    }
}
