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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Result of running a single benchmark with particular mode and parameters, either obtained by running
 * benchmark or read from a JMH JSON file.
 */
public final class BenchmarkResult {
    private final String source;
    private final String sourceDescription;
    private final String benchmark;
    private final String mode;
    private final Map<String, String> params;
    private final String unit;
    private final List<List<Double>> rawData;
    private final double score;
    private final double error;
    private final Statistics statistics;
    private final Map<String, Object> metadata = new LinkedHashMap<>();
    private String backend;

    /**
     * Creates result and computes score and error from raw data.
     *
     * @param source identifier of the source of results, e.g. name of a backend.
     * @param mode JMH short label of the mode, e.g. {@code avgt}.
     * @param rawData scores of measurement iterations, grouped by forks.
     */
    public BenchmarkResult(String source, String sourceDescription, String benchmark, String mode,
            Map<String, String> params, String unit, List<List<Double>> rawData) {
        this(source, sourceDescription, benchmark, mode, params, unit, rawData, Double.NaN, Double.NaN);
    }

    /**
     * Creates result with explicitly given score and error. When score is NaN, it's computed from raw data.
     */
    public BenchmarkResult(String source, String sourceDescription, String benchmark, String mode,
            Map<String, String> params, String unit, List<List<Double>> rawData, double score, double error) {
        this.source = source;
        this.sourceDescription = sourceDescription;
        this.benchmark = benchmark;
        this.mode = mode;
        this.params = new TreeMap<>(params);
        this.unit = unit;
        var copy = new ArrayList<List<Double>>();
        for (var fork : rawData) {
            copy.add(List.copyOf(fork));
        }
        this.rawData = copy;
        var all = new ArrayList<Double>();
        for (var fork : rawData) {
            all.addAll(fork);
        }
        statistics = new Statistics(all);
        if (Double.isNaN(score)) {
            this.score = statistics.getMean();
            this.error = statistics.getError();
        } else {
            this.score = score;
            this.error = error;
        }
    }

    public String getSource() {
        return source;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public String getBenchmark() {
        return benchmark;
    }

    public String getMode() {
        return mode;
    }

    /**
     * Benchmark parameters, sorted by name.
     */
    public Map<String, String> getParams() {
        return params;
    }

    public String getUnit() {
        return unit;
    }

    public List<List<Double>> getRawData() {
        return rawData;
    }

    public double getScore() {
        return score;
    }

    public double getError() {
        return error;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    /**
     * Name of TeaVM backend that produced the result or {@code null} if result is not produced by TeaVM
     * (e.g. read from JSON produced by JMH).
     */
    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    /**
     * Additional fields of JMH JSON format, like number of iterations, iteration time, etc.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Key that identifies benchmark, mode and parameters, used to match results from different sources.
     */
    public String getKey() {
        var sb = new StringBuilder(benchmark).append(" ").append(mode);
        for (var entry : params.entrySet()) {
            sb.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}
