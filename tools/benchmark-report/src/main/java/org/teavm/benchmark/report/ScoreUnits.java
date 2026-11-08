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
package org.teavm.benchmark.report;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for formatting scores and converting them between units like {@code ns/op} or {@code ops/s}.
 */
public final class ScoreUnits {
    private ScoreUnits() {
    }

    public static String format(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "∞" : "-∞";
        }
        var abs = Math.abs(value);
        if (abs == 0 || abs >= 1) {
            return String.format(Locale.ROOT, "%.3f", value);
        }
        if (abs >= 1e-6) {
            var digits = Math.min(9, 2 - (int) Math.floor(Math.log10(abs)));
            return String.format(Locale.ROOT, "%." + digits + "f", value);
        }
        return String.format(Locale.ROOT, "%.3e", value);
    }

    /**
     * Scales score of result for display. Currently only scales units of bytes, so that, for example,
     * {@code 186346641 B/op} is displayed as {@code 177.715 MB/op}.
     */
    public static Display display(BenchmarkResult result) {
        var unit = result.getUnit();
        var scale = 1.0;
        if (unit.startsWith("B/")) {
            var suffix = unit.substring(1);
            var prefixes = new String[] { "", "K", "M", "G", "T" };
            var index = 0;
            var abs = Math.abs(result.getScore());
            while (index < prefixes.length - 1 && abs >= 1024 * scale) {
                scale *= 1024;
                index++;
            }
            unit = prefixes[index] + "B" + suffix;
        }
        return new Display(format(result.getScore() / scale),
                Double.isNaN(result.getError()) ? null : format(result.getError() / scale), unit);
    }

    public static final class Display {
        public final String score;
        public final String error;
        public final String unit;

        Display(String score, String error, String unit) {
            this.score = score;
            this.error = error;
            this.unit = unit;
        }
    }

    /**
     * Converts score between units. Returns NaN if units are incompatible.
     */
    public static double convert(double value, String fromUnit, String toUnit) {
        if (fromUnit.equals(toUnit)) {
            return value;
        }
        var from = parse(fromUnit);
        var to = parse(toUnit);
        if (from == null || to == null || from.throughput != to.throughput) {
            return Double.NaN;
        }
        // nanoseconds in unit of time
        var ratio = from.nanos / to.nanos;
        return from.throughput ? value / ratio : value * ratio;
    }

    /**
     * Tells whether unit is unit of time per operation or of throughput, as opposed to units of other
     * metrics, like bytes per operation.
     */
    public static boolean isTimeBased(String unit) {
        return parse(unit) != null;
    }

    public static boolean isHigherBetter(String unit) {
        var parsed = parse(unit);
        return parsed != null && parsed.throughput;
    }

    public static String timeUnitLabel(TimeUnit timeUnit) {
        switch (timeUnit) {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "us";
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "min";
            case HOURS:
                return "hr";
            case DAYS:
                return "day";
            default:
                throw new IllegalArgumentException();
        }
    }

    public static TimeUnit parseTimeUnit(String text) {
        for (var unit : TimeUnit.values()) {
            if (timeUnitLabel(unit).equalsIgnoreCase(text) || unit.name().equalsIgnoreCase(text)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unknown time unit: " + text);
    }

    private static ParsedUnit parse(String unit) {
        var slash = unit.indexOf('/');
        if (slash < 0) {
            return null;
        }
        var left = unit.substring(0, slash);
        var right = unit.substring(slash + 1);
        try {
            if (left.equals("ops")) {
                return new ParsedUnit(true, parseTimeUnit(right).toNanos(1));
            } else if (right.equals("op")) {
                return new ParsedUnit(false, parseTimeUnit(left).toNanos(1));
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    private static class ParsedUnit {
        final boolean throughput;
        final double nanos;

        ParsedUnit(boolean throughput, double nanos) {
            this.throughput = throughput;
            this.nanos = nanos;
        }
    }
}
