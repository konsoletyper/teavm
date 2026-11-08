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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Produces Markdown table with a column per source of results, convenient for pasting into issues,
 * pull requests and chats. Significant differences are highlighted with bold font.
 */
public final class MarkdownReport {
    private MarkdownReport() {
    }

    public static void write(List<BenchmarkResult> results, String title, PrintStream out) {
        var table = new ComparisonTable(results);
        out.println("## " + title);
        out.println();
        for (var source : table.getSources()) {
            out.println("* **" + escape(source) + "**: " + escape(table.getSourceDescription(source)));
        }
        out.println();

        var header = new ArrayList<String>();
        header.add("Benchmark");
        for (var param : table.getParamNames()) {
            header.add(param);
        }
        header.add("Mode");
        header.addAll(table.getSources());
        printRow(out, header);
        var separator = new ArrayList<String>();
        for (int i = 0; i < header.size(); ++i) {
            separator.add(i < header.size() - table.getSources().size() ? "---" : "---:");
        }
        printRow(out, separator);

        for (var row : table.getRows()) {
            var cells = new ArrayList<String>();
            cells.add(table.shortName(row.getBenchmark()));
            for (var param : table.getParamNames()) {
                cells.add(row.getParams().getOrDefault(param, ""));
            }
            cells.add(row.getMode());
            for (var source : table.getSources()) {
                var result = row.get(source);
                cells.add(result != null ? formatCell(table, row, result) : "—");
            }
            printRow(out, cells);
        }
        out.println();
        out.println("Differences that are not statistically significant (Welch's t-test, p ≥ "
                + ComparisonTable.SIGNIFICANCE_LEVEL + ") are marked as such.");
    }

    private static String formatCell(ComparisonTable table, ComparisonTable.Row row, BenchmarkResult result) {
        var display = ScoreUnits.display(result);
        var sb = new StringBuilder(display.score);
        if (display.error != null) {
            sb.append(" ± ").append(display.error);
        }
        sb.append(" ").append(display.unit);
        var referenceResult = table.referenceFor(row, result);
        if (referenceResult != null) {
            var description = ComparisonTable.describe(result, referenceResult);
            if (!description.isEmpty()) {
                sb.append("<br>");
                var significant = ComparisonTable.isSignificant(result, referenceResult);
                if (significant) {
                    sb.append("**");
                }
                sb.append(description);
                if (significant) {
                    sb.append("**");
                }
            }
        }
        return sb.toString();
    }

    private static void printRow(PrintStream out, List<String> cells) {
        var sb = new StringBuilder("|");
        for (var cell : cells) {
            sb.append(" ").append(cell.replace("|", "\\|")).append(" |");
        }
        out.println(sb);
    }

    private static String escape(String text) {
        return text.replace("*", "\\*").replace("_", "\\_");
    }
}
