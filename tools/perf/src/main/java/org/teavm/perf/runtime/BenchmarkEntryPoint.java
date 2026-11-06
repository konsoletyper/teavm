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
package org.teavm.perf.runtime;

/**
 * Template of a benchmark program. Native methods are replaced by
 * {@link org.teavm.perf.BenchmarkEntryPointTransformer} with code that calls methods of particular benchmark.
 * Host passes settings as a single argument (see {@link #run(String)}) and reads results from stdout
 * (lines starting with {@link #OUTPUT_PREFIX}).
 */
public final class BenchmarkEntryPoint {
    public static final String OUTPUT_PREFIX = "@@teavm-perf@@ ";
    public static final String MODE_TIMED = "timed";
    public static final String MODE_SINGLE_SHOT = "ss";

    private static final long MIN_BATCH_TIME = 1_000_000L;

    private static String mode = MODE_TIMED;
    private static int warmupIterations;
    private static long warmupTime;
    private static int warmupBatchSize = 1;
    private static int measurementIterations;
    private static long measurementTime;
    private static int measurementBatchSize = 1;
    private static int batch = 1;
    private static String profileTitle;

    private BenchmarkEntryPoint() {
    }

    /**
     * Runs benchmark.
     *
     * @param argument list of {@code key=value} pairs, separated by {@code ;}. Values of parameters are
     *                 encoded, so that they can't contain any characters except for ASCII letters and digits.
     *                 Characters are encoded as {@code %XXXX}, where {@code XXXX} is a hex code of a
     *                 UTF-16 code unit.
     */
    public static void run(String argument) {
        run(argument, null);
    }

    /**
     * Runs benchmark. When argument contains {@code prof} key and profiler is given, measurement iterations
     * are recorded by profiler with title, specified by value of {@code prof} key.
     */
    public static void run(String argument, BenchmarkProfiler profiler) {
        createStates();
        parseArgument(argument);
        setupTrial();
        for (int i = 0; i < warmupIterations; ++i) {
            iteration("W", warmupTime, warmupBatchSize);
        }
        var profile = profiler != null && profileTitle != null;
        if (profile) {
            profiler.start(profileTitle);
        }
        for (int i = 0; i < measurementIterations; ++i) {
            iteration("M", measurementTime, measurementBatchSize);
        }
        if (profile) {
            profiler.stop(profileTitle);
        }
        tearDownTrial();
        System.out.println(OUTPUT_PREFIX + "END " + BlackholeSink.hash());
        System.out.flush();
    }

    private static void parseArgument(String argument) {
        int index = 0;
        while (index < argument.length()) {
            int next = argument.indexOf(';', index);
            if (next < 0) {
                next = argument.length();
            }
            int eq = argument.indexOf('=', index);
            if (eq < 0 || eq > next) {
                throw new IllegalArgumentException("Invalid argument: " + argument);
            }
            String key = argument.substring(index, eq);
            String value = argument.substring(eq + 1, next);
            index = next + 1;
            switch (key) {
                case "mode":
                    mode = value;
                    break;
                case "wi":
                    warmupIterations = Integer.parseInt(value);
                    break;
                case "wt":
                    warmupTime = Long.parseLong(value);
                    break;
                case "wb":
                    warmupBatchSize = Integer.parseInt(value);
                    break;
                case "mi":
                    measurementIterations = Integer.parseInt(value);
                    break;
                case "mt":
                    measurementTime = Long.parseLong(value);
                    break;
                case "mb":
                    measurementBatchSize = Integer.parseInt(value);
                    break;
                case "prof":
                    profileTitle = decode(value);
                    break;
                default:
                    if (key.startsWith("p")) {
                        setParam(Integer.parseInt(key.substring(1)), decode(value));
                    } else {
                        throw new IllegalArgumentException("Unknown key: " + key);
                    }
                    break;
            }
        }
    }

    private static String decode(String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '%') {
                sb.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                i += 5;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static void iteration(String kind, long time, int batchSize) {
        setupIteration();
        long ops;
        long elapsed;
        if (mode.equals(MODE_SINGLE_SHOT)) {
            long start = System.nanoTime();
            runBatch(batchSize);
            elapsed = System.nanoTime() - start;
            ops = 1;
        } else {
            ops = 0;
            long minBatchTime = Math.max(MIN_BATCH_TIME, time / 100);
            long start = System.nanoTime();
            long deadline = start + time;
            long now;
            do {
                long batchStart = System.nanoTime();
                runBatch(batch);
                now = System.nanoTime();
                ops += batch;
                if (now - batchStart < minBatchTime && batch < (1 << 30)) {
                    batch *= 2;
                }
            } while (now < deadline);
            elapsed = now - start;
        }
        tearDownIteration();
        System.out.println(OUTPUT_PREFIX + kind + " " + ops + " " + elapsed);
    }

    private static native void createStates();

    private static native void setParam(int index, String value);

    private static native void setupTrial();

    private static native void setupIteration();

    private static native void runBatch(int count);

    private static native void tearDownIteration();

    private static native void tearDownTrial();
}
