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
package org.teavm.benchmarks;

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

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class StringSplitBenchmark {
    @Param({ "4", "64" })
    public int fields;

    private String commaSeparated;
    private String commaSpaceSeparated;

    @Setup
    public void setup() {
        var comma = new StringBuilder();
        var commaSpace = new StringBuilder();
        for (var i = 0; i < fields; ++i) {
            if (i > 0) {
                comma.append(",");
                commaSpace.append(", ");
            }
            var field = "field" + i;
            comma.append(field);
            commaSpace.append(field);
        }
        commaSeparated = comma.toString();
        commaSpaceSeparated = commaSpace.toString();
    }

    /**
     * Splits by a single character, which JDK handles without involving regular expression engine.
     */
    @Benchmark
    public void splitSingleChar(Blackhole blackhole) {
        blackhole.consume(commaSeparated.split(","));
    }

    /**
     * Splits by a multi-character literal string, which requires regular expression engine.
     */
    @Benchmark
    public void splitMultiChar(Blackhole blackhole) {
        blackhole.consume(commaSpaceSeparated.split(", "));
    }

    /**
     * Splits by a real regular expression.
     */
    @Benchmark
    public void splitRegex(Blackhole blackhole) {
        blackhole.consume(commaSpaceSeparated.split("\\s*,\\s*"));
    }
}
