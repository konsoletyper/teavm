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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Writes comparison reports in formats chosen by user.
 */
public final class ReportFormat {
    public static final String TEXT = "text";
    public static final String HTML = "html";
    public static final String MARKDOWN = "markdown";
    public static final Set<String> ALL = Set.of(TEXT, HTML, MARKDOWN);

    private ReportFormat() {
    }

    /**
     * Parses comma-separated list of formats.
     *
     * @param extraFormats formats that are not handled by this class, but supported by caller.
     */
    public static Set<String> parse(String text, String... extraFormats) {
        var result = new LinkedHashSet<String>();
        for (var part : text.split(",")) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            if (!ALL.contains(part) && !List.of(extraFormats).contains(part)) {
                throw new IllegalArgumentException("Unknown report format: " + part);
            }
            result.add(part);
        }
        return result;
    }

    /**
     * Prints text report to stdout and writes reports in given formats to {@code <outputDir>/<name>.<ext>}.
     * Formats not supported by this class are ignored.
     *
     * @param version version of tool that produced results, mentioned in HTML report, or {@code null}.
     */
    public static void write(List<BenchmarkResult> results, Set<String> formats, File outputDir, String name,
            String title, String version) throws IOException {
        if (results.isEmpty()) {
            return;
        }
        outputDir.mkdirs();

        System.out.println();
        System.out.println();
        TextReport.write(results, System.out);
        System.out.println();

        if (formats.contains(TEXT)) {
            var file = new File(outputDir, name + ".txt");
            try (var out = new PrintStream(new FileOutputStream(file), false, StandardCharsets.UTF_8)) {
                TextReport.write(results, out);
            }
            System.out.println("Text report written to " + file.getAbsolutePath());
        }
        if (formats.contains(MARKDOWN)) {
            var file = new File(outputDir, name + ".md");
            try (var out = new PrintStream(new FileOutputStream(file), false, StandardCharsets.UTF_8)) {
                MarkdownReport.write(results, title, out);
            }
            System.out.println("Markdown report written to " + file.getAbsolutePath());
        }
        if (formats.contains(HTML)) {
            var file = new File(outputDir, name + ".html");
            HtmlReport.write(results, file, title, version);
            System.out.println("HTML report written to " + file.getAbsolutePath());
        }
    }
}
