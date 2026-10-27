/*
 *  Copyright 2026 Ethan Resnick.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Measures the hot operations of {@link HashMap}, {@link LinkedHashMap} and {@link HashSet}: insertion into a
 * growing map (which includes every resize), insertion into a pre-sized map, lookups of present and missing keys,
 * iteration, computeIfAbsent and merge. Keys are either strings or boxed integers.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class HashMapBenchmark {
    @Param({ "8", "1000" })
    public int size;

    @Param({ "string", "integer" })
    public String keyKind;

    private Object[] keys;
    private Object[] missingKeys;
    private HashMap<Object, Object> map;
    private LinkedHashMap<Object, Object> linkedMap;
    private HashSet<Object> set;

    @Setup
    public void setup() {
        keys = new Object[size];
        missingKeys = new Object[size];
        for (var i = 0; i < size; ++i) {
            keys[i] = key(i);
            missingKeys[i] = key(i + size);
        }
        map = new HashMap<>();
        linkedMap = new LinkedHashMap<>();
        set = new HashSet<>();
        for (var key : keys) {
            map.put(key, key);
            linkedMap.put(key, key);
            set.add(key);
        }
    }

    private Object key(int i) {
        // Integers are multiplied so that they are not consecutive.
        return keyKind.equals("string") ? "key-" + i : Integer.valueOf(i * 1031);
    }

    @Benchmark
    public Map<Object, Object> putGrowing() {
        var result = new HashMap<>();
        for (var key : keys) {
            result.put(key, key);
        }
        return result;
    }

    @Benchmark
    public Map<Object, Object> putPresized() {
        var result = new HashMap<>(size * 2);
        for (var key : keys) {
            result.put(key, key);
        }
        return result;
    }

    @Benchmark
    public Map<Object, Object> putLinkedGrowing() {
        var result = new LinkedHashMap<>();
        for (var key : keys) {
            result.put(key, key);
        }
        return result;
    }

    @Benchmark
    public Map<Object, Object> putAllCopy() {
        return new HashMap<>(map);
    }

    @Benchmark
    public Map<Object, Object> computeIfAbsentGrowing() {
        var result = new HashMap<>();
        for (var key : keys) {
            result.computeIfAbsent(key, k -> k);
        }
        return result;
    }

    @Benchmark
    public Map<Object, Object> computeIfAbsentPresent() {
        for (var key : keys) {
            map.computeIfAbsent(key, k -> k);
        }
        return map;
    }

    @Benchmark
    public Map<Object, Integer> mergeCount() {
        var result = new HashMap<Object, Integer>();
        for (var key : keys) {
            result.merge(key, 1, Integer::sum);
        }
        for (var key : keys) {
            result.merge(key, 1, Integer::sum);
        }
        return result;
    }

    @Benchmark
    public void getPresent(Blackhole blackhole) {
        for (var key : keys) {
            blackhole.consume(map.get(key));
        }
    }

    @Benchmark
    public void getMissing(Blackhole blackhole) {
        for (var key : missingKeys) {
            blackhole.consume(map.get(key));
        }
    }

    @Benchmark
    public void getLinkedPresent(Blackhole blackhole) {
        for (var key : keys) {
            blackhole.consume(linkedMap.get(key));
        }
    }

    @Benchmark
    public void setContains(Blackhole blackhole) {
        for (var key : keys) {
            blackhole.consume(set.contains(key));
        }
    }

    @Benchmark
    public void iterateEntries(Blackhole blackhole) {
        for (var entry : map.entrySet()) {
            blackhole.consume(entry.getValue());
        }
    }

    @Benchmark
    public void iterateSet(Blackhole blackhole) {
        for (var element : set) {
            blackhole.consume(element);
        }
    }

    @Benchmark
    public int removeAndReinsert() {
        for (var key : keys) {
            map.remove(key);
        }
        for (var key : keys) {
            map.put(key, key);
        }
        return map.size();
    }
}
