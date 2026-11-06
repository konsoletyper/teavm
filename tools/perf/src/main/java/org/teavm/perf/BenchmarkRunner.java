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
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.teavm.perf.runtime.BenchmarkEntryPoint;

/**
 * Compiles benchmarks for each of the given backends, runs them and collects results.
 */
public class BenchmarkRunner {
    private final BenchmarkEnvironment environment;
    private final BenchmarkOptions options;
    private final List<BenchmarkBackend> backends;
    private final PrintStream log;
    private final List<String> failures = new ArrayList<>();
    private int profileCounter;
    private static final int PROFILE_SUMMARY_SIZE = 15;

    public BenchmarkRunner(BenchmarkEnvironment environment, BenchmarkOptions options,
            List<BenchmarkBackend> backends, PrintStream log) {
        this.environment = environment;
        this.options = options;
        this.backends = List.copyOf(backends);
        this.log = log;
    }

    public List<String> getFailures() {
        return failures;
    }

    public List<BenchmarkResult> run(List<BenchmarkInfo> benchmarks) {
        var results = new ArrayList<BenchmarkResult>();
        for (var backend : backends) {
            var compiled = compileAll(backend, benchmarks);
            if (compiled.isEmpty()) {
                continue;
            }
            if (environment.isCpuProfiling() && !backend.supportsCpuProfiling()) {
                log.println("WARNING: CPU profiling is not supported by " + backend.getName()
                        + " backend with current settings, benchmarks will run without profiling");
            }
            try {
                backend.start();
            } catch (BenchmarkException e) {
                fail("Could not start " + backend.getName() + ": " + e.getMessage());
                continue;
            }
            try {
                for (var benchmark : compiled) {
                    runBenchmark(backend, benchmark, results);
                }
            } finally {
                backend.stop();
            }
        }
        return results;
    }

    private List<CompiledBenchmark> compileAll(BenchmarkBackend backend, List<BenchmarkInfo> benchmarks) {
        var result = new ArrayList<CompiledBenchmark>();
        var baseDir = new File(new File(environment.getOutputDir(), "build"), backend.getName());
        for (var benchmark : benchmarks) {
            log.println("# Compiling " + benchmark.getName() + " for " + backend.getName());
            var start = System.currentTimeMillis();
            try {
                result.add(backend.compile(benchmark, new File(baseDir, benchmark.getName())));
                log.println("#   done in " + (System.currentTimeMillis() - start) + " ms");
            } catch (BenchmarkException e) {
                fail(e.getMessage());
                if (e.getCause() != null) {
                    e.getCause().printStackTrace(log);
                }
            }
        }
        return result;
    }

    private void runBenchmark(BenchmarkBackend backend, CompiledBenchmark compiled, List<BenchmarkResult> results) {
        var benchmark = compiled.getBenchmark();
        for (var mode : options.resolveModes(benchmark)) {
            for (var paramValues : paramCombinations(benchmark)) {
                try {
                    results.add(runBenchmark(backend, compiled, mode, paramValues));
                } catch (BenchmarkException e) {
                    fail(e.getMessage());
                }
            }
        }
    }

    private BenchmarkResult runBenchmark(BenchmarkBackend backend, CompiledBenchmark compiled, BenchmarkMode mode,
            Map<ParamInfo, String> paramValues) throws BenchmarkException {
        var benchmark = compiled.getBenchmark();
        var warmup = options.resolveWarmup(benchmark);
        var measurement = options.resolveMeasurement(benchmark);
        var forks = options.resolveForks(benchmark);
        var timeUnit = options.resolveTimeUnit(benchmark);
        var opi = options.resolveOperationsPerInvocation(benchmark);
        var unit = mode.unit(timeUnit);

        log.println();
        log.println("# Backend: " + backend.getName() + " (" + backend.getDescription() + ")");
        log.println("# Warmup: " + describe(warmup, mode));
        log.println("# Measurement: " + describe(measurement, mode));
        log.println("# Benchmark mode: " + mode.getJmhName() + ", " + unit);
        log.println("# Benchmark: " + benchmark.getName());
        var params = new LinkedHashMap<String, String>();
        for (var entry : paramValues.entrySet()) {
            params.put(entry.getKey().getName(), entry.getValue());
        }
        if (!params.isEmpty()) {
            var sb = new StringBuilder();
            for (var entry : params.entrySet()) {
                if (!sb.isEmpty()) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append(" = ").append(entry.getValue());
            }
            log.println("# Parameters: (" + sb + ")");
        }

        var argument = buildArgument(mode, warmup, measurement, paramValues);
        var rawData = new ArrayList<List<Double>>();
        for (int fork = 1; fork <= forks; ++fork) {
            log.println();
            log.println("# Fork: " + fork + " of " + forks);
            var forkData = new ArrayList<Double>();
            var counters = new int[2];
            var finished = new boolean[1];
            var profileTitle = environment.isCpuProfiling() && backend.supportsCpuProfiling()
                    ? "teavm-perf-" + ++profileCounter
                    : null;
            var forkArgument = profileTitle != null ? argument + ";prof=" + encode(profileTitle) : argument;
            backend.run(compiled, forkArgument, line -> {
                if (!line.startsWith(BenchmarkEntryPoint.OUTPUT_PREFIX)) {
                    log.println(line);
                    return;
                }
                var parts = line.substring(BenchmarkEntryPoint.OUTPUT_PREFIX.length()).split(" ");
                switch (parts[0]) {
                    case "W":
                    case "M": {
                        var score = score(mode, Long.parseLong(parts[1]), Long.parseLong(parts[2]), opi, timeUnit);
                        var formatted = ScoreUnits.format(score) + " " + unit;
                        if (parts[0].equals("W")) {
                            log.printf("# Warmup Iteration %3d: %s%n", ++counters[0], formatted);
                        } else {
                            log.printf("Iteration %3d: %s%n", ++counters[1], formatted);
                            forkData.add(score);
                        }
                        break;
                    }
                    case "END":
                        finished[0] = true;
                        break;
                }
            });
            if (!finished[0]) {
                throw new BenchmarkException("Benchmark " + benchmark.getName() + " on " + backend.getName()
                        + " finished without reporting results");
            }
            rawData.add(forkData);
            if (profileTitle != null) {
                saveProfile(backend, benchmark, params, forks > 1 ? fork : 0,
                        backend.takeCpuProfile(profileTitle));
            }
        }

        var result = new BenchmarkResult(backend.getName(), backend.getDescription(), benchmark.getName(),
                mode.getShortLabel(), params, unit, rawData);
        result.setBackend(backend.getName());
        var metadata = result.getMetadata();
        metadata.put("warmupIterations", warmup.getIterations());
        metadata.put("warmupTime", IterationSettings.formatTime(warmup.getTimeNanos()));
        metadata.put("warmupBatchSize", warmup.getBatchSize());
        metadata.put("measurementIterations", measurement.getIterations());
        metadata.put("measurementTime", IterationSettings.formatTime(measurement.getTimeNanos()));
        metadata.put("measurementBatchSize", measurement.getBatchSize());
        metadata.put("forks", forks);

        log.println();
        log.println("Result \"" + benchmark.getName() + "\":");
        log.println("  " + ScoreUnits.format(result.getScore()) + " ±(99.9%) " + ScoreUnits.format(result.getError())
                + " " + unit);
        return result;
    }

    private void saveProfile(BenchmarkBackend backend, BenchmarkInfo benchmark, Map<String, String> params,
            int fork, String profile) throws BenchmarkException {
        var sb = new StringBuilder(benchmark.getName());
        for (var entry : params.entrySet()) {
            sb.append('-').append(entry.getKey()).append('_').append(entry.getValue());
        }
        if (fork > 0) {
            sb.append("-fork").append(fork);
        }
        var fileName = sb.toString().replaceAll("[^A-Za-z0-9._-]", "_") + ".cpuprofile";
        var file = new File(new File(new File(environment.getOutputDir(), "profiles"), backend.getName()), fileName);
        file.getParentFile().mkdirs();
        try {
            Files.writeString(file.toPath(), profile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BenchmarkException("Error writing profile to " + file, e);
        }

        var summary = CpuProfileSummary.parse(profile);
        log.println();
        log.println("CPU profile of measurement iterations (self time, " + summary.getTotalSamples()
                + " samples), full profile written to " + file);
        var entries = summary.getEntries();
        for (var i = 0; i < Math.min(PROFILE_SUMMARY_SIZE, entries.size()); ++i) {
            var entry = entries.get(i);
            log.printf(Locale.ROOT, "  %5.1f%%  %s%n", summary.fraction(entry) * 100, entry.function);
        }
    }

    private static String describe(IterationSettings settings, BenchmarkMode mode) {
        if (mode == BenchmarkMode.SINGLE_SHOT_TIME) {
            return settings.getIterations() + " iterations, single-shot each"
                    + (settings.getBatchSize() > 1 ? ", " + settings.getBatchSize() + " calls per op" : "");
        }
        return settings.getIterations() + " iterations, " + IterationSettings.formatTime(settings.getTimeNanos())
                + " each";
    }

    static double score(BenchmarkMode mode, long ops, long nanos, int opi, TimeUnit timeUnit) {
        double unitNanos = timeUnit.toNanos(1);
        double operations = (double) ops * opi;
        switch (mode) {
            case THROUGHPUT:
                return operations / nanos * unitNanos;
            case AVERAGE_TIME:
                return nanos / operations / unitNanos;
            case SINGLE_SHOT_TIME:
                return nanos / (double) opi / unitNanos;
            default:
                throw new IllegalArgumentException();
        }
    }

    private String buildArgument(BenchmarkMode mode, IterationSettings warmup, IterationSettings measurement,
            Map<ParamInfo, String> paramValues) {
        var sb = new StringBuilder();
        sb.append("mode=").append(mode == BenchmarkMode.SINGLE_SHOT_TIME
                ? BenchmarkEntryPoint.MODE_SINGLE_SHOT
                : BenchmarkEntryPoint.MODE_TIMED);
        sb.append(";wi=").append(warmup.getIterations());
        sb.append(";wt=").append(warmup.getTimeNanos());
        sb.append(";wb=").append(Math.max(1, warmup.getBatchSize()));
        sb.append(";mi=").append(measurement.getIterations());
        sb.append(";mt=").append(measurement.getTimeNanos());
        sb.append(";mb=").append(Math.max(1, measurement.getBatchSize()));
        for (var entry : paramValues.entrySet()) {
            sb.append(";p").append(entry.getKey().getIndex()).append('=').append(encode(entry.getValue()));
        }
        return sb.toString();
    }

    static String encode(String value) {
        var sb = new StringBuilder();
        for (int i = 0; i < value.length(); ++i) {
            var c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.'
                    || c == '-' || c == '_') {
                sb.append(c);
            } else {
                sb.append('%').append(String.format("%04x", (int) c));
            }
        }
        return sb.toString();
    }

    private List<Map<ParamInfo, String>> paramCombinations(BenchmarkInfo benchmark) {
        List<Map<ParamInfo, String>> result = new ArrayList<>();
        result.add(new LinkedHashMap<>());
        for (var param : benchmark.getParams()) {
            var next = new ArrayList<Map<ParamInfo, String>>();
            for (var combination : result) {
                for (var value : options.resolveParamValues(param)) {
                    var extended = new LinkedHashMap<>(combination);
                    extended.put(param, value);
                    next.add(extended);
                }
            }
            result = next;
        }
        return result;
    }

    private void fail(String message) {
        log.println("ERROR: " + message);
        failures.add(message);
    }
}
