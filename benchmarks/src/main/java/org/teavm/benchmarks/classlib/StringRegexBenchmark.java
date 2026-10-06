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

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
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

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class StringRegexBenchmark {
    @Param({ "16", "1024" })
    public int length;

    private String text;
    private String identifier;

    @Setup
    public void setup() {
        var sb = new StringBuilder();
        var index = 0;
        while (sb.length() < length) {
            sb.append("word").append(index++).append(index % 3 == 0 ? "  " : " ");
        }
        sb.setLength(length);
        text = sb.toString();
        identifier = "x".repeat(length);
    }

    @Benchmark
    public String replaceAll() {
        return text.replaceAll("\\s+", "_");
    }

    @Benchmark
    public String replaceFirst() {
        return text.replaceFirst("\\d+", "#");
    }

    @Benchmark
    public boolean matches() {
        return identifier.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    @Benchmark
    public boolean patternMatches() {
        return Pattern.matches("[a-zA-Z_][a-zA-Z0-9_]*", identifier);
    }
}
