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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.teavm.perf.report.HtmlReport;
import org.teavm.perf.report.JmhJsonFormat;
import org.teavm.perf.report.TextReport;
import org.teavm.vm.TeaVMOptimizationLevel;

/**
 * Command line entry point of the benchmark runner. Options that control benchmark execution
 * mimic JMH command line options, so the same set of options can be passed to both JMH and this runner.
 */
public final class PerfCli {
    private static final Set<String> FORMATS = Set.of("text", "json", "html");

    private PerfCli() {
    }

    public static void main(String[] args) {
        int exitCode;
        try {
            exitCode = run(args);
        } catch (Throwable e) {
            e.printStackTrace();
            exitCode = 1;
        }
        // Browser runner leaves non-daemon threads, so exit explicitly
        System.exit(exitCode);
    }

    public static int run(String[] args) {
        var options = createOptions();
        CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            printUsage(options);
            return 2;
        }
        if (commandLine.hasOption("h")) {
            printUsage(options);
            return 0;
        }
        try {
            return new PerfCli.Execution(commandLine).run();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
    }

    private static Options createOptions() {
        var options = new Options();
        options.addOption(Option.builder("h").longOpt("help").desc("Show this help").build());
        options.addOption(Option.builder("l").desc("List matching benchmarks and exit").build());
        options.addOption(Option.builder("e").argName("regexp").hasArg()
                .desc("Exclude benchmarks matching regular expression. Can be specified multiple times").build());
        options.addOption(Option.builder("wi").argName("int").hasArg()
                .desc("Number of warmup iterations").build());
        options.addOption(Option.builder("w").argName("time").hasArg()
                .desc("Time of each warmup iteration, e.g. 1s, 500ms").build());
        options.addOption(Option.builder("wbs").argName("int").hasArg()
                .desc("Warmup batch size, i.e. number of benchmark calls per op in single shot mode").build());
        options.addOption(Option.builder("i").argName("int").hasArg()
                .desc("Number of measurement iterations").build());
        options.addOption(Option.builder("r").argName("time").hasArg()
                .desc("Time of each measurement iteration, e.g. 1s, 500ms").build());
        options.addOption(Option.builder("bs").argName("int").hasArg()
                .desc("Measurement batch size, i.e. number of benchmark calls per op in single shot mode").build());
        options.addOption(Option.builder("f").argName("int").hasArg()
                .desc("How many times to fork a single benchmark. In browser every fork runs in a new frame, "
                        + "in native code every fork runs in a new process").build());
        options.addOption(Option.builder("bm").argName("mode").hasArg()
                .desc("Benchmark mode, comma-separated list of: thrpt, avgt, ss, all").build());
        options.addOption(Option.builder("tu").argName("unit").hasArg()
                .desc("Output time unit: ns, us, ms, s, min").build());
        options.addOption(Option.builder("opi").argName("int").hasArg()
                .desc("Operations per invocation").build());
        options.addOption(Option.builder("p").argName("param=v1,v2").hasArg()
                .desc("Override values of benchmark parameters. Can be specified multiple times").build());
        options.addOption(Option.builder("rf").argName("formats").hasArg()
                .desc("Comma-separated list of report formats to produce: text, json, html. "
                        + "Default is text,json,html").build());
        options.addOption(Option.builder("o").longOpt("output-dir").argName("dir").hasArg()
                .desc("Directory to write reports and compiled benchmarks to. Default is teavm-perf").build());
        options.addOption(Option.builder("b").longOpt("backends").argName("list").hasArg()
                .desc("Comma-separated list of backends: js, wasm-gc, c. Default is js").build());
        options.addOption(Option.builder().longOpt("browser").argName("browser").hasArg()
                .desc("Browser to run JS and Wasm GC benchmarks: browser-chrome, browser-firefox or browser "
                        + "(print URL to open manually). Default is browser-chrome").build());
        options.addOption(Option.builder().longOpt("optimization").argName("level").hasArg()
                .desc("TeaVM optimization level: simple, advanced, full. Default is full").build());
        options.addOption(Option.builder().longOpt("cc").argName("command").hasArg()
                .desc("C compiler for native backend. Default is cc").build());
        options.addOption(Option.builder().longOpt("cflags").argName("flags").hasArg()
                .desc("Flags for C compiler, separated by spaces. Default is -O2").build());
        options.addOption(Option.builder().longOpt("c-build-script").argName("file").hasArg()
                .desc("Script that builds C code, runs in directory with generated code and must produce "
                        + "'benchmark' executable. Overrides --cc and --cflags").build());
        options.addOption(Option.builder().longOpt("scan").argName("path").hasArg()
                .desc("Directory or JAR file to search benchmarks in. Can be specified multiple times. "
                        + "By default, all directories on classpath are searched").build());
        options.addOption(Option.builder().longOpt("baseline").argName("[label=]file").hasArg()
                .desc("JMH JSON result file (for example, produced by running JMH with '-rf json') "
                        + "to compare with. Can be specified multiple times").build());
        options.addOption(Option.builder().longOpt("compare-only")
                .desc("Don't run benchmarks, only produce reports from baseline files").build());
        return options;
    }

    private static void printUsage(Options options) {
        var formatter = new HelpFormatter();
        formatter.setWidth(120);
        formatter.printHelp("java " + PerfCli.class.getName() + " [options] [benchmark regexp...]", options);
    }

    private static final class Execution {
        private final CommandLine commandLine;
        private final BenchmarkOptions benchmarkOptions = new BenchmarkOptions();
        private File outputDir;
        private Set<String> formats;

        Execution(CommandLine commandLine) {
            this.commandLine = commandLine;
        }

        int run() throws IOException {
            parseBenchmarkOptions();
            outputDir = new File(commandLine.getOptionValue("o", "teavm-perf"));
            formats = new LinkedHashSet<>(splitList(commandLine.getOptionValue("rf", "text,json,html")));
            for (var format : formats) {
                if (!FORMATS.contains(format)) {
                    throw new IllegalArgumentException("Unknown report format: " + format);
                }
            }

            var baselines = new ArrayList<BenchmarkResult>();
            for (var baseline : nullToEmpty(commandLine.getOptionValues("baseline"))) {
                baselines.addAll(readBaseline(baseline));
            }

            if (commandLine.hasOption("compare-only")) {
                if (baselines.isEmpty()) {
                    throw new IllegalArgumentException("--compare-only requires at least one --baseline");
                }
                writeReports(baselines, List.of());
                return 0;
            }

            var environment = new BenchmarkEnvironment(PerfCli.class.getClassLoader(), outputDir);
            environment.setOptimizationLevel(parseOptimizationLevel(
                    commandLine.getOptionValue("optimization", "full")));
            var benchmarks = findBenchmarks(environment);
            if (benchmarks == null) {
                return 1;
            }
            if (commandLine.hasOption("l")) {
                System.out.println("Benchmarks:");
                for (var benchmark : benchmarks) {
                    System.out.println(benchmark.getName());
                }
                return 0;
            }
            if (benchmarks.isEmpty()) {
                System.err.println("No matching benchmarks found");
                return 1;
            }

            var backends = createBackends(environment);
            var runner = new BenchmarkRunner(environment, benchmarkOptions, backends, System.out);
            var results = runner.run(benchmarks);
            writeReports(baselines, results);

            if (!runner.getFailures().isEmpty()) {
                System.out.println();
                System.out.println(runner.getFailures().size() + " failure(s):");
                for (var failure : runner.getFailures()) {
                    System.out.println("  " + failure.lines().findFirst().orElse(""));
                }
                return 1;
            }
            return 0;
        }

        private void parseBenchmarkOptions() {
            for (var arg : commandLine.getArgList()) {
                benchmarkOptions.getIncludes().add(compilePattern(arg));
            }
            for (var arg : nullToEmpty(commandLine.getOptionValues("e"))) {
                benchmarkOptions.getExcludes().add(compilePattern(arg));
            }
            benchmarkOptions.setWarmup(new IterationSettings(
                    intOption("wi"),
                    commandLine.hasOption("w") ? IterationSettings.parseTime(commandLine.getOptionValue("w")) : -1,
                    intOption("wbs")));
            benchmarkOptions.setMeasurement(new IterationSettings(
                    intOption("i"),
                    commandLine.hasOption("r") ? IterationSettings.parseTime(commandLine.getOptionValue("r")) : -1,
                    intOption("bs")));
            benchmarkOptions.setForks(intOption("f"));
            benchmarkOptions.setOperationsPerInvocation(intOption("opi"));
            if (commandLine.hasOption("bm")) {
                var modes = EnumSet.noneOf(BenchmarkMode.class);
                for (var name : splitList(commandLine.getOptionValue("bm"))) {
                    if (name.equalsIgnoreCase("all")) {
                        modes.addAll(EnumSet.allOf(BenchmarkMode.class));
                        continue;
                    }
                    var mode = BenchmarkMode.parse(name);
                    if (mode == null) {
                        throw new IllegalArgumentException("Unsupported benchmark mode: " + name);
                    }
                    modes.add(mode);
                }
                benchmarkOptions.setModes(modes);
            }
            if (commandLine.hasOption("tu")) {
                benchmarkOptions.setTimeUnit(BenchmarkMode.parseTimeUnit(commandLine.getOptionValue("tu")));
            }
            for (var param : nullToEmpty(commandLine.getOptionValues("p"))) {
                var eq = param.indexOf('=');
                if (eq <= 0) {
                    throw new IllegalArgumentException("Invalid parameter specification: " + param);
                }
                benchmarkOptions.getParams().put(param.substring(0, eq), splitList(param.substring(eq + 1)));
            }
        }

        private int intOption(String name) {
            var value = commandLine.getOptionValue(name);
            if (value == null) {
                return -1;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Option -" + name + " requires integer value: " + value);
            }
        }

        private static Pattern compilePattern(String regex) {
            try {
                return Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid regular expression: " + regex);
            }
        }

        private static TeaVMOptimizationLevel parseOptimizationLevel(String text) {
            switch (text.toLowerCase()) {
                case "simple":
                    return TeaVMOptimizationLevel.SIMPLE;
                case "advanced":
                    return TeaVMOptimizationLevel.ADVANCED;
                case "full":
                    return TeaVMOptimizationLevel.FULL;
                default:
                    throw new IllegalArgumentException("Unknown optimization level: " + text);
            }
        }

        private List<BenchmarkInfo> findBenchmarks(BenchmarkEnvironment environment) throws IOException {
            var scanPaths = new ArrayList<File>();
            for (var path : nullToEmpty(commandLine.getOptionValues("scan"))) {
                scanPaths.add(new File(path));
            }
            if (scanPaths.isEmpty()) {
                for (var entry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
                    var file = new File(entry);
                    if (file.isDirectory()) {
                        scanPaths.add(file);
                    }
                }
            }
            var classNames = new LinkedHashSet<String>();
            for (var path : scanPaths) {
                classNames.addAll(ClassNameCollector.collect(path));
            }

            var scanner = new BenchmarkScanner(environment.getClassSource());
            var benchmarks = scanner.scan(classNames);
            for (var warning : scanner.getWarnings()) {
                System.err.println("WARNING: " + warning);
            }
            if (!scanner.getProblems().isEmpty()) {
                for (var problem : scanner.getProblems()) {
                    System.err.println("ERROR: " + problem);
                }
                return null;
            }
            benchmarks.removeIf(b -> !benchmarkOptions.matches(b));
            return benchmarks;
        }

        private List<BenchmarkBackend> createBackends(BenchmarkEnvironment environment) {
            var browser = commandLine.getOptionValue("browser", "browser-chrome");
            var backends = new ArrayList<BenchmarkBackend>();
            for (var name : splitList(commandLine.getOptionValue("b", "js"))) {
                switch (name) {
                    case "js":
                        backends.add(new JsBackend(environment, browser));
                        break;
                    case "wasm-gc":
                        backends.add(new WasmGCBackend(environment, browser));
                        break;
                    case "c": {
                        var backend = new CBackend(environment);
                        if (commandLine.hasOption("cc")) {
                            backend.setCompiler(commandLine.getOptionValue("cc"));
                        }
                        if (commandLine.hasOption("cflags")) {
                            backend.setCompilerFlags(Arrays.asList(commandLine.getOptionValue("cflags").trim()
                                    .split("\\s+")));
                        }
                        if (commandLine.hasOption("c-build-script")) {
                            backend.setBuildScript(new File(commandLine.getOptionValue("c-build-script")));
                        }
                        backends.add(backend);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unknown backend: " + name);
                }
            }
            return backends;
        }

        private List<BenchmarkResult> readBaseline(String spec) throws IOException {
            String label = null;
            var path = spec;
            var eq = spec.indexOf('=');
            if (eq > 0) {
                var prefix = spec.substring(0, eq);
                if (prefix.indexOf('/') < 0 && prefix.indexOf('\\') < 0) {
                    label = prefix;
                    path = spec.substring(eq + 1);
                }
            }
            var file = new File(path);
            if (!file.isFile()) {
                throw new IllegalArgumentException("Baseline file not found: " + file);
            }
            return JmhJsonFormat.read(file, label);
        }

        private void writeReports(List<BenchmarkResult> baselines, List<BenchmarkResult> results)
                throws IOException {
            if (results.isEmpty() && baselines.isEmpty()) {
                return;
            }
            var all = new ArrayList<BenchmarkResult>(baselines);
            all.addAll(results);
            outputDir.mkdirs();

            System.out.println();
            System.out.println();
            TextReport.write(all, System.out);

            if (formats.contains("text")) {
                var file = new File(outputDir, "results.txt");
                try (var out = new PrintStream(new FileOutputStream(file), false, StandardCharsets.UTF_8)) {
                    TextReport.write(all, out);
                }
                System.out.println();
                System.out.println("Text report written to " + file.getAbsolutePath());
            }
            if (formats.contains("json") && !results.isEmpty()) {
                var sources = new LinkedHashSet<String>();
                for (var result : results) {
                    sources.add(result.getSource());
                }
                for (var source : sources) {
                    var file = new File(outputDir, "results-" + source + ".json");
                    var sourceResults = new ArrayList<BenchmarkResult>();
                    for (var result : results) {
                        if (result.getSource().equals(source)) {
                            sourceResults.add(result);
                        }
                    }
                    JmhJsonFormat.write(sourceResults, file);
                    System.out.println("JMH-compatible JSON results written to " + file.getAbsolutePath());
                }
            }
            if (formats.contains("html")) {
                var file = new File(outputDir, "report.html");
                HtmlReport.write(all, file);
                System.out.println("HTML report written to " + file.getAbsolutePath());
            }
        }

        private static List<String> splitList(String text) {
            var result = new ArrayList<String>();
            for (var part : text.split(",")) {
                part = part.trim();
                if (!part.isEmpty()) {
                    result.add(part);
                }
            }
            return result;
        }

        private static List<String> nullToEmpty(String[] values) {
            return values != null ? Arrays.asList(values) : List.of();
        }
    }
}
