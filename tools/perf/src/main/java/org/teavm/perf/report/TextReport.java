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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.teavm.perf.BenchmarkResult;
import org.teavm.perf.ScoreUnits;

/**
 * Prints results as a table, similar to the one JMH prints after running benchmarks.
 */
public final class TextReport {
    private TextReport() {
    }

    public static void write(List<BenchmarkResult> results, PrintStream out) {
        var table = new ComparisonTable(results);
        var compare = table.getSources().size() > 1;

        var header = new ArrayList<String>();
        header.add("Benchmark");
        for (var param : table.getParamNames()) {
            header.add("(" + param + ")");
        }
        header.add("Backend");
        header.add("Mode");
        header.add("Cnt");
        header.add("Score");
        header.add("");
        header.add("Error");
        header.add("Units");
        if (compare) {
            header.add("Compared to");
        }

        var lines = new ArrayList<List<String>>();
        for (var row : table.getRows()) {
            for (var source : table.getSources()) {
                var result = row.get(source);
                if (result == null) {
                    continue;
                }
                var line = new ArrayList<String>();
                line.add(table.shortName(row.getBenchmark()));
                for (var param : table.getParamNames()) {
                    line.add(row.getParams().getOrDefault(param, "N/A"));
                }
                line.add(source);
                line.add(row.getMode());
                line.add(String.valueOf(result.getStatistics().getCount()));
                line.add(ScoreUnits.format(result.getScore()));
                var hasError = !Double.isNaN(result.getError());
                line.add(hasError ? "±" : "");
                line.add(hasError ? ScoreUnits.format(result.getError()) : "");
                line.add(result.getUnit());
                if (compare) {
                    var referenceResult = table.referenceFor(row, result);
                    var slowdown = referenceResult != null
                            ? ComparisonTable.slowdown(result, referenceResult)
                            : Double.NaN;
                    line.add(Double.isNaN(slowdown) ? ""
                            : ComparisonTable.formatSlowdown(slowdown) + " than " + referenceResult.getSource());
                }
                lines.add(line);
            }
        }

        var widths = new int[header.size()];
        for (int i = 0; i < widths.length; ++i) {
            widths[i] = header.get(i).length();
        }
        for (var line : lines) {
            for (int i = 0; i < widths.length; ++i) {
                widths[i] = Math.max(widths[i], line.get(i).length());
            }
        }

        var leftAligned = new boolean[header.size()];
        leftAligned[0] = true;
        var unitsIndex = header.indexOf("Units");
        for (int i = unitsIndex; i < leftAligned.length; ++i) {
            leftAligned[i] = true;
        }
        printLine(out, header, widths, leftAligned);
        for (var line : lines) {
            printLine(out, line, widths, leftAligned);
        }

        if (compare) {
            out.println();
            for (var source : table.getSources()) {
                out.println(source + ": " + table.getSourceDescription(source));
            }
        }
    }

    private static void printLine(PrintStream out, List<String> cells, int[] widths, boolean[] leftAligned) {
        var sb = new StringBuilder();
        for (int i = 0; i < cells.size(); ++i) {
            var cell = cells.get(i);
            var padding = " ".repeat(widths[i] - cell.length());
            if (i > 0) {
                sb.append(cells.get(i - 1).equals("±") || cell.equals("±") ? " " : "  ");
            }
            if (leftAligned[i]) {
                sb.append(cell).append(padding);
            } else {
                sb.append(padding).append(cell);
            }
        }
        out.println(sb.toString().stripTrailing());
    }
}
