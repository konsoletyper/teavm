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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.help.TextHelpAppendable;

/**
 * Command line tool that compares results of benchmarks stored in JMH JSON format (produced by JMH with
 * {@code -rf json} or by TeaVM benchmark runner) and produces reports.
 */
public final class ReportCli {
    private ReportCli() {
    }

    public static void main(String[] args) {
        int exitCode;
        try {
            exitCode = run(args);
        } catch (Throwable e) {
            e.printStackTrace();
            exitCode = 1;
        }
        System.exit(exitCode);
    }

    public static int run(String[] args) throws IOException {
        var options = createOptions();
        CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            printUsage(options);
            return 2;
        }
        if (commandLine.hasOption("h") || commandLine.getArgList().isEmpty()) {
            printUsage(options);
            return commandLine.hasOption("h") ? 0 : 2;
        }
        Set<String> formats;
        try {
            formats = ReportFormat.parse(commandLine.getOptionValue("rf", "text,html,markdown"));
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        }

        var results = new ArrayList<BenchmarkResult>();
        for (var spec : commandLine.getArgList()) {
            try {
                results.addAll(JmhJsonFormat.read(spec));
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                return 2;
            }
        }

        var outputDir = new File(commandLine.getOptionValue("o", "."));
        var name = commandLine.getOptionValue("name", "report");
        var title = commandLine.getOptionValue("title", "Benchmarks");
        ReportFormat.write(results, formats, outputDir, name, title, null);
        return 0;
    }

    private static Options createOptions() {
        var options = new Options();
        options.addOption(Option.builder("h").longOpt("help").desc("Show this help").get());
        options.addOption(Option.builder("rf").argName("formats").hasArg()
                .desc("Comma-separated list of report formats to produce: text, html, markdown. "
                        + "Default is all of them. Text report is printed to stdout anyway").get());
        options.addOption(Option.builder("o").longOpt("output-dir").argName("dir").hasArg()
                .desc("Directory to write reports to. Default is current directory").get());
        options.addOption(Option.builder().longOpt("name").argName("name").hasArg()
                .desc("Name of report files without extension. Default is 'report'").get());
        options.addOption(Option.builder().longOpt("title").argName("title").hasArg()
                .desc("Title of report. Default is 'Benchmarks'").get());
        return options;
    }

    private static void printUsage(Options options) {
        var output = new TextHelpAppendable(System.out);
        output.setMaxWidth(120);
        var formatter = HelpFormatter.builder().setHelpAppendable(output).setShowSince(false).get();
        try {
            formatter.printHelp("java " + ReportCli.class.getName() + " [options] [label=]file.json...",
                    "Compares results of benchmarks. Results of the first file are used as a reference "
                    + "(baseline). Results labeled after TeaVM backends are compared with results of the same "
                    + "backend from preceding files.", options, "", false);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
