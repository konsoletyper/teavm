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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.teavm.perf.BenchmarkResult;
import org.teavm.perf.ScoreUnits;

/**
 * Groups results from different sources (backends, baseline files) by benchmark, mode and parameters.
 */
public final class ComparisonTable {
    private final List<String> sources;
    private final Map<String, String> sourceDescriptions;
    private final List<Row> rows = new ArrayList<>();
    private final String reference;
    private final List<String> paramNames;
    private final String commonPrefix;

    public ComparisonTable(List<BenchmarkResult> results) {
        sourceDescriptions = JmhJsonFormat.describeSources(results);
        sources = new ArrayList<>(sourceDescriptions.keySet());
        reference = sources.isEmpty() ? null : sources.get(0);

        var rowMap = new LinkedHashMap<String, Row>();
        var paramNameSet = new LinkedHashSet<String>();
        for (var result : results) {
            var row = rowMap.computeIfAbsent(result.getKey(), k -> new Row(result));
            row.results.putIfAbsent(result.getSource(), result);
            paramNameSet.addAll(result.getParams().keySet());
        }
        paramNames = new ArrayList<>(paramNameSet);
        paramNames.sort(Comparator.naturalOrder());
        rows.addAll(rowMap.values());
        rows.sort(Comparator.comparing((Row r) -> r.benchmark)
                .thenComparing(r -> r.mode)
                .thenComparing(this::compareParams));
        commonPrefix = computeCommonPrefix();
    }

    private int compareParams(Row a, Row b) {
        for (var name : paramNames) {
            var x = a.params.get(name);
            var y = b.params.get(name);
            if (x == null || y == null) {
                if (x != y) {
                    return x == null ? -1 : 1;
                }
                continue;
            }
            int cmp;
            try {
                cmp = Double.compare(Double.parseDouble(x), Double.parseDouble(y));
            } catch (NumberFormatException e) {
                cmp = x.compareTo(y);
            }
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private String computeCommonPrefix() {
        String prefix = null;
        for (var row : rows) {
            var name = row.benchmark;
            var packageEnd = name.lastIndexOf('.');
            packageEnd = packageEnd > 0 ? name.lastIndexOf('.', packageEnd - 1) : -1;
            var pkg = packageEnd >= 0 ? name.substring(0, packageEnd + 1) : "";
            if (prefix == null) {
                prefix = pkg;
            } else {
                int i = 0;
                while (i < prefix.length() && i < pkg.length() && prefix.charAt(i) == pkg.charAt(i)) {
                    i++;
                }
                prefix = prefix.substring(0, prefix.lastIndexOf('.', i - 1) + 1);
            }
        }
        return prefix != null ? prefix : "";
    }

    public List<String> getSources() {
        return sources;
    }

    public String getSourceDescription(String source) {
        return sourceDescriptions.get(source);
    }

    public String getReference() {
        return reference;
    }

    public List<String> getParamNames() {
        return paramNames;
    }

    public List<Row> getRows() {
        return rows;
    }

    public String shortName(String benchmark) {
        return benchmark.startsWith(commonPrefix) ? benchmark.substring(commonPrefix.length()) : benchmark;
    }

    /**
     * Finds result to compare given result with. For results produced by TeaVM, it's the result of the same
     * backend from one of the preceding sources (e.g. baseline file produced by previous version of TeaVM).
     * Otherwise, it's the result from the first source.
     *
     * @return reference result or {@code null} if there's nothing to compare with.
     */
    public BenchmarkResult referenceFor(Row row, BenchmarkResult result) {
        if (result.getBackend() != null) {
            for (var source : sources) {
                var candidate = row.get(source);
                if (candidate == result) {
                    break;
                }
                if (candidate != null && result.getBackend().equals(candidate.getBackend())) {
                    return candidate;
                }
            }
        }
        var referenceResult = row.get(reference);
        return referenceResult != result ? referenceResult : null;
    }

    /**
     * Computes how many times result is slower than reference result. Values below 1 mean that result is faster.
     *
     * @return slowdown factor or NaN if results can't be compared.
     */
    public static double slowdown(BenchmarkResult result, BenchmarkResult referenceResult) {
        var referenceScore = ScoreUnits.convert(referenceResult.getScore(), referenceResult.getUnit(),
                result.getUnit());
        if (Double.isNaN(referenceScore) || referenceScore == 0 || result.getScore() == 0) {
            return Double.NaN;
        }
        return ScoreUnits.isHigherBetter(result.getUnit())
                ? referenceScore / result.getScore()
                : result.getScore() / referenceScore;
    }

    public static String formatSlowdown(double slowdown) {
        if (Double.isNaN(slowdown)) {
            return "";
        }
        if (slowdown >= 1) {
            return String.format(Locale.ROOT, "%.2fx slower", slowdown);
        } else {
            return String.format(Locale.ROOT, "%.2fx faster", 1 / slowdown);
        }
    }

    public static final class Row {
        private final String benchmark;
        private final String mode;
        private final Map<String, String> params;
        private final Map<String, BenchmarkResult> results = new LinkedHashMap<>();

        Row(BenchmarkResult first) {
            benchmark = first.getBenchmark();
            mode = first.getMode();
            params = first.getParams();
        }

        public String getBenchmark() {
            return benchmark;
        }

        public String getMode() {
            return mode;
        }

        public Map<String, String> getParams() {
            return params;
        }

        public BenchmarkResult get(String source) {
            return results.get(source);
        }
    }
}
