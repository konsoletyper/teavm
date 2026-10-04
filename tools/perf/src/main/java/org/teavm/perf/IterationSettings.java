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
package org.teavm.perf;

/**
 * Settings of warmup or measurement phase. Negative values mean "not specified".
 */
public final class IterationSettings {
    public static final IterationSettings BLANK = new IterationSettings(-1, -1, -1);

    private final int iterations;
    private final long timeNanos;
    private final int batchSize;

    public IterationSettings(int iterations, long timeNanos, int batchSize) {
        this.iterations = iterations;
        this.timeNanos = timeNanos;
        this.batchSize = batchSize;
    }

    public int getIterations() {
        return iterations;
    }

    public long getTimeNanos() {
        return timeNanos;
    }

    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Takes values from this object, falling back to values of {@code other} object when value is not specified.
     */
    public IterationSettings orElse(IterationSettings other) {
        return new IterationSettings(
                iterations >= 0 ? iterations : other.iterations,
                timeNanos >= 0 ? timeNanos : other.timeNanos,
                batchSize >= 0 ? batchSize : other.batchSize
        );
    }

    public static String formatTime(long nanos) {
        if (nanos % 1_000_000_000L == 0) {
            return nanos / 1_000_000_000L + " s";
        } else if (nanos % 1_000_000L == 0) {
            return nanos / 1_000_000L + " ms";
        } else if (nanos % 1_000L == 0) {
            return nanos / 1_000L + " us";
        } else {
            return nanos + " ns";
        }
    }

    /**
     * Parses time in JMH format, e.g. {@code 1s}, {@code 500ms}, {@code 10 us}.
     */
    public static long parseTime(String text) {
        text = text.trim();
        int index = 0;
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            index++;
        }
        if (index == 0) {
            throw new IllegalArgumentException("Invalid time: " + text);
        }
        long value = Long.parseLong(text.substring(0, index));
        var unit = text.substring(index).trim();
        if (unit.isEmpty()) {
            unit = "s";
        }
        return BenchmarkMode.parseTimeUnit(unit).toNanos(value);
    }
}
