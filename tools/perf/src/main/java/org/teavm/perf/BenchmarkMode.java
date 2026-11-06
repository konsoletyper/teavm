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

import java.util.concurrent.TimeUnit;

/**
 * Subset of JMH benchmark modes supported by TeaVM benchmark runner.
 */
public enum BenchmarkMode {
    THROUGHPUT("thrpt", "Throughput"),
    AVERAGE_TIME("avgt", "AverageTime"),
    SINGLE_SHOT_TIME("ss", "SingleShotTime");

    private final String shortLabel;
    private final String jmhName;

    BenchmarkMode(String shortLabel, String jmhName) {
        this.shortLabel = shortLabel;
        this.jmhName = jmhName;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public String getJmhName() {
        return jmhName;
    }

    public String unit(TimeUnit timeUnit) {
        var unitLabel = timeUnitLabel(timeUnit);
        return this == THROUGHPUT ? "ops/" + unitLabel : unitLabel + "/op";
    }

    public boolean isHigherBetter() {
        return this == THROUGHPUT;
    }

    /**
     * Parses mode the way JMH does, i.e. accepts both short label (e.g. {@code avgt}) and name of the constant of
     * JMH's {@code Mode} enum (e.g. {@code AverageTime}).
     *
     * @return parsed mode or {@code null}, if mode is not supported.
     */
    public static BenchmarkMode parse(String text) {
        for (var mode : values()) {
            if (mode.shortLabel.equalsIgnoreCase(text) || mode.jmhName.equalsIgnoreCase(text)) {
                return mode;
            }
        }
        return null;
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
}
