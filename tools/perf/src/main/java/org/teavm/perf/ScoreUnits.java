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

import java.util.Locale;

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

    public static boolean isHigherBetter(String unit) {
        var parsed = parse(unit);
        return parsed != null && parsed.throughput;
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
                return new ParsedUnit(true, BenchmarkMode.parseTimeUnit(right).toNanos(1));
            } else if (right.equals("op")) {
                return new ParsedUnit(false, BenchmarkMode.parseTimeUnit(left).toNanos(1));
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
