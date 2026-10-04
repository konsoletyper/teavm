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
package org.teavm.perf.report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.perf.BenchmarkResult;
import org.teavm.perf.Statistics;
import org.teavm.vm.TeaVM;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reads and writes results in the format that JMH produces with {@code -rf json} option.
 * This allows to compare results of TeaVM benchmarks with results of JMH on JVM and to use existing
 * tools that visualize JMH results.
 */
public final class JmhJsonFormat {
    public static final String GENERATOR = "teavm-perf";
    private static final String BACKEND_FIELD = "teavmBackend";

    private JmhJsonFormat() {
    }

    public static void write(List<BenchmarkResult> results, File file) throws IOException {
        var mapper = new ObjectMapper();
        var root = mapper.createArrayNode();
        var version = teavmVersion();
        for (var result : results) {
            var node = root.addObject();
            node.put("jmhVersion", GENERATOR);
            node.put("benchmark", result.getBenchmark());
            node.put("mode", result.getMode());
            node.put("threads", 1);
            node.put("forks", ((Number) result.getMetadata().getOrDefault("forks", result.getRawData().size()))
                    .intValue());
            node.put("jvm", "teavm-" + result.getSource());
            node.putArray("jvmArgs");
            node.put("jdkVersion", version);
            node.put("vmName", result.getSourceDescription());
            node.put("vmVersion", version);
            node.put(BACKEND_FIELD, result.getBackend() != null ? result.getBackend() : result.getSource());
            for (var key : List.of("warmupIterations", "warmupTime", "warmupBatchSize", "measurementIterations",
                    "measurementTime", "measurementBatchSize")) {
                var value = result.getMetadata().get(key);
                if (value instanceof Number) {
                    node.put(key, ((Number) value).intValue());
                } else if (value != null) {
                    node.put(key, value.toString());
                }
            }
            if (!result.getParams().isEmpty()) {
                var params = node.putObject("params");
                for (var entry : result.getParams().entrySet()) {
                    params.put(entry.getKey(), entry.getValue());
                }
            }
            writePrimaryMetric(node.putObject("primaryMetric"), result);
            node.putObject("secondaryMetrics");
        }
        file.getAbsoluteFile().getParentFile().mkdirs();
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
    }

    private static void writePrimaryMetric(ObjectNode metric, BenchmarkResult result) {
        putDouble(metric, "score", result.getScore());
        putDouble(metric, "scoreError", result.getError());
        var confidence = metric.putArray("scoreConfidence");
        addDouble(confidence, result.getScore() - result.getError());
        addDouble(confidence, result.getScore() + result.getError());
        var percentiles = metric.putObject("scorePercentiles");
        var statistics = result.getStatistics();
        for (var percentile : Statistics.PERCENTILES) {
            putDouble(percentiles, formatPercentile(percentile), statistics.getPercentile(percentile));
        }
        metric.put("scoreUnit", result.getUnit());
        var rawData = metric.putArray("rawData");
        for (var fork : result.getRawData()) {
            var forkNode = rawData.addArray();
            for (var value : fork) {
                addDouble(forkNode, value);
            }
        }
    }

    private static String formatPercentile(double percentile) {
        return Double.toString(percentile);
    }

    private static void putDouble(ObjectNode node, String name, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            node.put(name, Double.toString(value));
        } else {
            node.put(name, value);
        }
    }

    private static void addDouble(ArrayNode node, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            node.add(Double.toString(value));
        } else {
            node.add(value);
        }
    }

    /**
     * Reads results from JSON file produced either by JMH or by TeaVM benchmark runner.
     *
     * @param label name of the source of results. If {@code null}, results produced by TeaVM get name of
     *              the backend, other results get name of the file without extension.
     */
    public static List<BenchmarkResult> read(File file, String label) throws IOException {
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(file);
        } catch (JacksonException e) {
            throw new IOException("Error reading " + file + ": " + e.getMessage(), e);
        }
        if (!root.isArray()) {
            throw new IOException("File " + file + " does not contain JMH results");
        }
        var defaultLabel = file.getName();
        if (defaultLabel.endsWith(".json")) {
            defaultLabel = defaultLabel.substring(0, defaultLabel.length() - 5);
        }
        var results = new ArrayList<BenchmarkResult>();
        for (var node : root.values()) {
            var source = label;
            if (source == null) {
                source = node.has(BACKEND_FIELD) ? node.get(BACKEND_FIELD).asString() : defaultLabel;
            }
            var description = node.has("vmName") ? node.get("vmName").asString() : source;
            if (node.has("vmVersion") && !node.get("vmVersion").asString().equals("unknown")) {
                description += " " + node.get("vmVersion").asString();
            }
            var params = new LinkedHashMap<String, String>();
            var paramsNode = node.get("params");
            if (paramsNode != null) {
                for (var entry : paramsNode.properties()) {
                    params.put(entry.getKey(), entry.getValue().asString());
                }
            }
            var metric = node.get("primaryMetric");
            var rawData = new ArrayList<List<Double>>();
            var rawDataNode = metric.get("rawData");
            if (rawDataNode != null) {
                for (var forkNode : rawDataNode.values()) {
                    var fork = new ArrayList<Double>();
                    for (var valueNode : forkNode.values()) {
                        fork.add(readDouble(valueNode));
                    }
                    rawData.add(fork);
                }
            }
            var result = new BenchmarkResult(source, description, node.get("benchmark").asString(),
                    node.get("mode").asString(), params, metric.get("scoreUnit").asString(), rawData,
                    readDouble(metric.get("score")), readDouble(metric.get("scoreError")));
            for (var key : List.of("forks", "warmupIterations", "warmupTime", "warmupBatchSize",
                    "measurementIterations", "measurementTime", "measurementBatchSize")) {
                var value = node.get(key);
                if (value != null) {
                    result.getMetadata().put(key, value.isNumber() ? (Object) value.asInt() : value.asString());
                }
            }
            if (node.has(BACKEND_FIELD)) {
                result.setBackend(node.get(BACKEND_FIELD).asString());
            }
            results.add(result);
        }
        return results;
    }

    private static double readDouble(JsonNode node) {
        if (node == null) {
            return Double.NaN;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        try {
            return Double.parseDouble(node.asString());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    static String teavmVersion() {
        var version = JmhJsonFormat.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = TeaVM.class.getPackage().getImplementationVersion();
        }
        return version != null ? version : "unknown";
    }

    static Map<String, String> describeSources(List<BenchmarkResult> results) {
        var result = new LinkedHashMap<String, String>();
        for (var r : results) {
            result.putIfAbsent(r.getSource(), r.getSourceDescription());
        }
        return result;
    }
}
