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
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.teavm.model.MethodReference;

/**
 * Describes a single method marked with JMH's {@code @Benchmark} annotation and settings that are specified
 * via annotations. Settings that are not specified by annotations are {@code null} or negative.
 */
public final class BenchmarkInfo {
    /**
     * Marker for {@link #getArguments()} that means that a blackhole is passed to the corresponding parameter.
     */
    public static final int BLACKHOLE_ARGUMENT = -1;

    private final String className;
    private final MethodReference method;
    private final boolean isStatic;
    final List<StateInfo> states = new ArrayList<>();
    final List<ParamInfo> params = new ArrayList<>();
    final List<Integer> arguments = new ArrayList<>();
    StateInfo instanceState;
    Set<BenchmarkMode> modes;
    TimeUnit timeUnit;
    IterationSettings warmup = IterationSettings.BLANK;
    IterationSettings measurement = IterationSettings.BLANK;
    int forks = -1;
    int operationsPerInvocation = 1;

    BenchmarkInfo(String className, MethodReference method, boolean isStatic) {
        this.className = className;
        this.method = method;
        this.isStatic = isStatic;
    }

    /**
     * Name of the benchmark in JMH format, i.e. fully qualified class name followed by a dot and method name.
     */
    public String getName() {
        return className + "." + method.getName();
    }

    public String getShortName() {
        return className.substring(className.lastIndexOf('.') + 1) + "." + method.getName();
    }

    public String getClassName() {
        return className;
    }

    public MethodReference getMethod() {
        return method;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public List<StateInfo> getStates() {
        return states;
    }

    /**
     * State that holds an instance of a benchmark class, {@code null} for static benchmark methods.
     */
    public StateInfo getInstanceState() {
        return instanceState;
    }

    public List<ParamInfo> getParams() {
        return params;
    }

    /**
     * For each parameter of the benchmark method, index of a state to pass or {@link #BLACKHOLE_ARGUMENT}.
     */
    public List<Integer> getArguments() {
        return arguments;
    }

    public Set<BenchmarkMode> getModes() {
        return modes;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public IterationSettings getWarmup() {
        return warmup;
    }

    public IterationSettings getMeasurement() {
        return measurement;
    }

    public int getForks() {
        return forks;
    }

    public int getOperationsPerInvocation() {
        return operationsPerInvocation;
    }
}
