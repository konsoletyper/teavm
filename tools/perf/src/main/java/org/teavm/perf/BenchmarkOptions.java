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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Settings that override settings given by annotations, usually specified via command line.
 * When neither annotations nor these options specify a setting, defaults are used. Defaults are the same as
 * JMH ones, except for iteration count, iteration time and number of forks, which are smaller to keep running
 * time reasonable.
 */
public class BenchmarkOptions {
    public static final IterationSettings DEFAULT_WARMUP = new IterationSettings(5, 1_000_000_000L, 1);
    public static final IterationSettings DEFAULT_MEASUREMENT = new IterationSettings(5, 1_000_000_000L, 1);
    public static final int DEFAULT_FORKS = 1;
    public static final Set<BenchmarkMode> DEFAULT_MODES = EnumSet.of(BenchmarkMode.THROUGHPUT);
    public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    private final List<Pattern> includes = new ArrayList<>();
    private final List<Pattern> excludes = new ArrayList<>();
    private IterationSettings warmup = IterationSettings.BLANK;
    private IterationSettings measurement = IterationSettings.BLANK;
    private int forks = -1;
    private Set<BenchmarkMode> modes;
    private TimeUnit timeUnit;
    private int operationsPerInvocation = -1;
    private final Map<String, List<String>> params = new LinkedHashMap<>();

    public List<Pattern> getIncludes() {
        return includes;
    }

    public List<Pattern> getExcludes() {
        return excludes;
    }

    public IterationSettings getWarmup() {
        return warmup;
    }

    public void setWarmup(IterationSettings warmup) {
        this.warmup = warmup;
    }

    public IterationSettings getMeasurement() {
        return measurement;
    }

    public void setMeasurement(IterationSettings measurement) {
        this.measurement = measurement;
    }

    public int getForks() {
        return forks;
    }

    public void setForks(int forks) {
        this.forks = forks;
    }

    public Set<BenchmarkMode> getModes() {
        return modes;
    }

    public void setModes(Set<BenchmarkMode> modes) {
        this.modes = modes;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public int getOperationsPerInvocation() {
        return operationsPerInvocation;
    }

    public void setOperationsPerInvocation(int operationsPerInvocation) {
        this.operationsPerInvocation = operationsPerInvocation;
    }

    public Map<String, List<String>> getParams() {
        return params;
    }

    /**
     * Checks whether benchmark should run. Like in JMH, patterns are matched against any part of
     * fully qualified benchmark name.
     */
    public boolean matches(BenchmarkInfo benchmark) {
        var name = benchmark.getName();
        if (!includes.isEmpty() && includes.stream().noneMatch(p -> p.matcher(name).find())) {
            return false;
        }
        return excludes.stream().noneMatch(p -> p.matcher(name).find());
    }

    public IterationSettings resolveWarmup(BenchmarkInfo benchmark) {
        return warmup.orElse(benchmark.getWarmup()).orElse(DEFAULT_WARMUP);
    }

    public IterationSettings resolveMeasurement(BenchmarkInfo benchmark) {
        return measurement.orElse(benchmark.getMeasurement()).orElse(DEFAULT_MEASUREMENT);
    }

    public int resolveForks(BenchmarkInfo benchmark) {
        var result = forks >= 0 ? forks : benchmark.getForks() >= 0 ? benchmark.getForks() : DEFAULT_FORKS;
        return Math.max(1, result);
    }

    public Set<BenchmarkMode> resolveModes(BenchmarkInfo benchmark) {
        return modes != null ? modes : benchmark.getModes() != null ? benchmark.getModes() : DEFAULT_MODES;
    }

    public TimeUnit resolveTimeUnit(BenchmarkInfo benchmark) {
        return timeUnit != null ? timeUnit : benchmark.getTimeUnit() != null ? benchmark.getTimeUnit()
                : DEFAULT_TIME_UNIT;
    }

    public int resolveOperationsPerInvocation(BenchmarkInfo benchmark) {
        return operationsPerInvocation > 0 ? operationsPerInvocation : benchmark.getOperationsPerInvocation();
    }

    public List<String> resolveParamValues(ParamInfo param) {
        var values = params.get(param.getName());
        return values != null ? values : param.getDefaultValues();
    }
}
