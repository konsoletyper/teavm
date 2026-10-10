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
import java.util.regex.Matcher;
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

/**
 * Patterns that consist of several char classes, quantifiers and alternatives, which can be matched
 * by a single state machine.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class RegexStateMachineBenchmark {
    private static final Pattern NUMBER = Pattern.compile("\\d+\\.\\d+");
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern REQUEST = Pattern.compile("(?:GET|POST|PUT|DELETE) [^ ]+");
    private static final Pattern TAG = Pattern.compile("<[a-z]+?>");
    private static final String[] METHODS = { "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD" };

    @Param({ "16", "1024" })
    public int lines;

    private String log;

    @Setup
    public void setup() {
        var sb = new StringBuilder();
        for (var i = 0; i < lines; ++i) {
            // Every pattern also meets text that matches its beginning, but not the whole pattern
            var day = 10 + i % 19;
            sb.append(i % 2 == 0 ? "2026-10-" + day : "2026-10 " + day).append(' ');
            sb.append(i).append(i % 3 == 0 ? "" : "." + i % 97).append(" ms ");
            sb.append(i % 5 == 0 ? "<b1>" : "<span>").append(' ');
            sb.append(METHODS[i % METHODS.length]).append(" /api/item/").append(i).append(" done\n");
        }
        log = sb.toString();
    }

    private static int count(Matcher matcher) {
        var result = 0;
        while (matcher.find()) {
            result += matcher.end() - matcher.start();
        }
        return result;
    }

    /**
     * Sequence of loops and a literal, many digits that don't start a match.
     */
    @Benchmark
    public int findNumbers() {
        return count(NUMBER.matcher(log));
    }

    /**
     * Sequence of fixed-count quantifiers and literals.
     */
    @Benchmark
    public int findDates() {
        return count(DATE.matcher(log));
    }

    /**
     * Alternatives that share prefixes, followed by a sequence.
     */
    @Benchmark
    public int findRequests() {
        return count(REQUEST.matcher(log));
    }

    /**
     * Reluctant loop followed by a literal.
     */
    @Benchmark
    public int findTags() {
        return count(TAG.matcher(log));
    }
}
