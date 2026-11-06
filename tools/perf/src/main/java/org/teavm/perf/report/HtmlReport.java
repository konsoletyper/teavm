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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.teavm.perf.BenchmarkResult;
import org.teavm.perf.ScoreUnits;

/**
 * Produces self-contained human-readable HTML report that compares results from different sources.
 */
public final class HtmlReport {
    private HtmlReport() {
    }

    public static void write(List<BenchmarkResult> results, File file) throws IOException {
        file.getAbsoluteFile().getParentFile().mkdirs();
        try (var writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()),
                StandardCharsets.UTF_8))) {
            write(results, writer);
        }
    }

    private static void write(List<BenchmarkResult> results, PrintWriter out) {
        var table = new ComparisonTable(results);
        var version = JmhJsonFormat.teavmVersion();
        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("<meta charset=\"utf-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("<title>TeaVM benchmarks</title>");
        out.println("<style>");
        out.println(STYLE);
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<main>");
        out.println("<h1>TeaVM benchmarks</h1>");
        out.println("<p class=\"meta\">Generated " + escape(ZonedDateTime.now().format(
                DateTimeFormatter.RFC_1123_DATE_TIME))
                + (version.equals("unknown") ? "" : " &middot; TeaVM " + escape(version)) + "</p>");

        out.println("<dl class=\"sources\">");
        for (var source : table.getSources()) {
            out.println("<dt>" + escape(source) + "</dt>");
            out.println("<dd>" + escape(table.getSourceDescription(source)) + "</dd>");
        }
        out.println("</dl>");

        var groups = new ArrayList<List<ComparisonTable.Row>>();
        for (var row : table.getRows()) {
            var last = groups.isEmpty() ? null : groups.get(groups.size() - 1);
            if (last != null && last.get(0).getBenchmark().equals(row.getBenchmark())
                    && last.get(0).getMode().equals(row.getMode())) {
                last.add(row);
            } else {
                var group = new ArrayList<ComparisonTable.Row>();
                group.add(row);
                groups.add(group);
            }
        }

        for (var group : groups) {
            writeGroup(table, group, out);
        }

        out.println("</main>");
        out.println("</body>");
        out.println("</html>");
    }

    private static void writeGroup(ComparisonTable table, List<ComparisonTable.Row> group, PrintWriter out) {
        var first = group.get(0);
        var paramNames = group.stream()
                .flatMap(r -> r.getParams().keySet().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        String unit = null;
        double max = 0;
        for (var row : group) {
            for (var source : table.getSources()) {
                var result = row.get(source);
                if (result == null) {
                    continue;
                }
                if (unit == null) {
                    unit = result.getUnit();
                }
                var value = ScoreUnits.convert(result.getScore(), result.getUnit(), unit);
                if (!Double.isNaN(value)) {
                    max = Math.max(max, value);
                }
            }
        }
        var higherBetter = unit != null && ScoreUnits.isHigherBetter(unit);

        out.println("<section>");
        out.println("<h2><span class=\"name\">" + escape(table.shortName(first.getBenchmark())) + "</span>"
                + " <span class=\"mode\">" + escape(first.getMode()) + ", " + escape(unit != null ? unit : "")
                + ", " + (higherBetter ? "higher is better" : "lower is better") + "</span></h2>");
        out.println("<div class=\"scroll\"><table>");
        out.println("<thead><tr>");
        for (var param : paramNames) {
            out.println("<th class=\"param\">" + escape(param) + "</th>");
        }
        for (var source : table.getSources()) {
            out.println("<th>" + escape(source) + "</th>");
        }
        out.println("</tr></thead>");
        out.println("<tbody>");
        for (var row : group) {
            out.println("<tr>");
            for (var param : paramNames) {
                out.println("<td class=\"param\">" + escape(row.getParams().getOrDefault(param, "")) + "</td>");
            }
            for (var source : table.getSources()) {
                var result = row.get(source);
                if (result == null) {
                    out.println("<td class=\"missing\">&mdash;</td>");
                    continue;
                }
                var value = ScoreUnits.convert(result.getScore(), result.getUnit(), unit);
                var width = max > 0 && !Double.isNaN(value) ? Math.max(0.5, value / max * 100) : 0;
                var sb = new StringBuilder();
                sb.append("<td title=\"").append(escape(rawDataTitle(result))).append("\">");
                sb.append("<div class=\"score\"><span class=\"value\">")
                        .append(escape(ScoreUnits.format(result.getScore()))).append("</span>");
                if (!Double.isNaN(result.getError())) {
                    sb.append(" <span class=\"error\">&plusmn; ").append(escape(ScoreUnits.format(result.getError())))
                            .append("</span>");
                }
                sb.append(" <span class=\"unit\">").append(escape(result.getUnit())).append("</span></div>");
                sb.append("<div class=\"bar\"><div style=\"width: ")
                        .append(String.format(Locale.ROOT, "%.1f", width)).append("%\"></div></div>");
                var referenceResult = table.referenceFor(row, result);
                if (referenceResult != null) {
                    var slowdown = ComparisonTable.slowdown(result, referenceResult);
                    if (!Double.isNaN(slowdown)) {
                        sb.append("<div class=\"ratio ").append(slowdown >= 1 ? "slower" : "faster").append("\">")
                                .append(escape(ComparisonTable.formatSlowdown(slowdown) + " than "
                                        + referenceResult.getSource())).append("</div>");
                    }
                }
                sb.append("</td>");
                out.println(sb);
            }
            out.println("</tr>");
        }
        out.println("</tbody>");
        out.println("</table></div>");
        out.println("</section>");
    }

    private static String rawDataTitle(BenchmarkResult result) {
        var sb = new StringBuilder();
        int forkIndex = 1;
        for (var fork : result.getRawData()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append("Fork ").append(forkIndex++).append(": ");
            sb.append(fork.stream().map(ScoreUnits::format).collect(Collectors.joining(", ")));
        }
        return sb.toString();
    }

    private static String escape(String text) {
        var sb = new StringBuilder();
        for (int i = 0; i < text.length(); ++i) {
            var c = text.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    private static final String STYLE = String.join("\n",
            ":root {",
            "  --bg: #ffffff; --fg: #1d1f23; --muted: #5f6672; --border: #dfe2e7; --surface: #f6f7f9;",
            "  --bar: #4f7cc9; --slower: #b3412d; --faster: #2e7d4f; --tag-bg: #e8eefa; --tag-fg: #2a4f91;",
            "}",
            "@media (prefers-color-scheme: dark) {",
            "  :root {",
            "    --bg: #15171b; --fg: #e3e5e8; --muted: #9aa1ac; --border: #2d3138; --surface: #1d2025;",
            "    --bar: #6d97e0; --slower: #e5806b; --faster: #6cc28f; --tag-bg: #233350; --tag-fg: #a9c3f2;",
            "  }",
            "}",
            "body { margin: 0; background: var(--bg); color: var(--fg);",
            "  font: 14px/1.45 system-ui, -apple-system, 'Segoe UI', sans-serif; }",
            "main { max-width: 1200px; margin: 0 auto; padding: 24px 16px 48px; }",
            "h1 { font-size: 24px; margin: 0 0 4px; }",
            ".meta { color: var(--muted); margin: 0 0 16px; }",
            ".sources { display: grid; grid-template-columns: max-content 1fr; gap: 4px 16px; margin: 0 0 24px; }",
            ".sources dt { font-weight: 600; }",
            ".sources dd { margin: 0; color: var(--muted); }",
            ".tag { font-size: 11px; font-weight: 500; padding: 1px 6px; border-radius: 8px;",
            "  background: var(--tag-bg); color: var(--tag-fg); }",
            "section { margin: 0 0 28px; }",
            "h2 { font-size: 16px; margin: 0 0 8px; overflow-wrap: anywhere; }",
            "h2 .mode { font-weight: 400; color: var(--muted); font-size: 13px; }",
            ".scroll { overflow-x: auto; }",
            "table { border-collapse: collapse; min-width: 100%; }",
            "th, td { border-bottom: 1px solid var(--border); padding: 6px 10px; text-align: left;",
            "  vertical-align: top; }",
            "th { background: var(--surface); font-weight: 600; white-space: nowrap; }",
            "td.param { font-variant-numeric: tabular-nums; white-space: nowrap; }",
            "td { min-width: 160px; }",
            "td.param { min-width: 0; }",
            ".score { font-variant-numeric: tabular-nums; white-space: nowrap; }",
            ".score .value { font-weight: 600; }",
            ".score .error, .score .unit { color: var(--muted); }",
            ".bar { height: 4px; background: var(--surface); border-radius: 2px; margin: 4px 0 2px; }",
            ".bar div { height: 100%; background: var(--bar); border-radius: 2px; }",
            ".ratio { font-size: 12px; }",
            ".ratio.slower { color: var(--slower); }",
            ".ratio.faster { color: var(--faster); }",
            ".missing { color: var(--muted); }"
    );
}
