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
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;

/**
 * Finds benchmarks in given classes and extracts information from JMH annotations.
 * Annotations are matched by names, so JMH itself does not need to be available.
 */
public class BenchmarkScanner {
    private static final String JMH_ANNOTATIONS = "org.openjdk.jmh.annotations.";
    static final String BENCHMARK = JMH_ANNOTATIONS + "Benchmark";
    static final String STATE = JMH_ANNOTATIONS + "State";
    static final String PARAM = JMH_ANNOTATIONS + "Param";
    static final String SETUP = JMH_ANNOTATIONS + "Setup";
    static final String TEAR_DOWN = JMH_ANNOTATIONS + "TearDown";
    static final String BENCHMARK_MODE = JMH_ANNOTATIONS + "BenchmarkMode";
    static final String OUTPUT_TIME_UNIT = JMH_ANNOTATIONS + "OutputTimeUnit";
    static final String WARMUP = JMH_ANNOTATIONS + "Warmup";
    static final String MEASUREMENT = JMH_ANNOTATIONS + "Measurement";
    static final String FORK = JMH_ANNOTATIONS + "Fork";
    static final String OPERATIONS_PER_INVOCATION = JMH_ANNOTATIONS + "OperationsPerInvocation";
    static final String BLACKHOLE = "org.openjdk.jmh.infra.Blackhole";

    private static final Set<String> BOXED_TYPES = Set.of(Boolean.class.getName(), Byte.class.getName(),
            Short.class.getName(), Integer.class.getName(), Long.class.getName(), Float.class.getName(),
            Double.class.getName());

    private final ClassReaderSource classSource;
    private final List<String> problems = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public BenchmarkScanner(ClassReaderSource classSource) {
        this.classSource = classSource;
    }

    public List<String> getProblems() {
        return problems;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<BenchmarkInfo> scan(Collection<String> classNames) {
        var result = new ArrayList<BenchmarkInfo>();
        for (var className : classNames) {
            var cls = classSource.get(className);
            if (cls == null || cls.hasModifier(ElementModifier.ABSTRACT)
                    || cls.hasModifier(ElementModifier.INTERFACE)) {
                continue;
            }
            for (var method : collectBenchmarkMethods(cls)) {
                var info = analyze(cls, method);
                if (info != null) {
                    result.add(info);
                }
            }
        }
        result.sort((a, b) -> a.getName().compareTo(b.getName()));
        return result;
    }

    private List<MethodReader> collectBenchmarkMethods(ClassReader cls) {
        var result = new ArrayList<MethodReader>();
        var seen = new HashSet<MethodDescriptor>();
        for (var current = cls; current != null; current = superclass(current)) {
            for (var method : current.getMethods()) {
                if (seen.add(method.getDescriptor()) && method.getAnnotations().get(BENCHMARK) != null) {
                    result.add(method);
                }
            }
        }
        result.sort((a, b) -> a.getName().compareTo(b.getName()));
        return result;
    }

    private ClassReader superclass(ClassReader cls) {
        return cls.getParent() != null ? classSource.get(cls.getParent()) : null;
    }

    private BenchmarkInfo analyze(ClassReader cls, MethodReader method) {
        var isStatic = method.hasModifier(ElementModifier.STATIC);
        var info = new BenchmarkInfo(cls.getName(), method.getReference(), isStatic);
        var context = new AnalysisContext(info);
        if (method.hasModifier(ElementModifier.ABSTRACT)) {
            context.problem("benchmark method must not be abstract");
        }
        if (method.getLevel() == AccessLevel.PRIVATE) {
            context.problem("benchmark method must not be private");
        }

        for (int i = 0; i < method.parameterCount(); ++i) {
            var type = method.parameterType(i);
            if (!(type instanceof ValueType.Object)) {
                context.problem("parameter #" + (i + 1) + " has unsupported type " + type);
                continue;
            }
            var typeName = ((ValueType.Object) type).getClassName();
            if (typeName.equals(BLACKHOLE)) {
                info.arguments.add(BenchmarkInfo.BLACKHOLE_ARGUMENT);
                continue;
            }
            var stateClass = classSource.get(typeName);
            if (stateClass == null || findClassAnnotation(stateClass, STATE) == null) {
                context.problem("parameter #" + (i + 1) + " of type " + typeName
                        + " is neither a Blackhole, nor a class annotated with @State");
                continue;
            }
            var state = context.getState(stateClass);
            if (state != null) {
                info.arguments.add(state.getIndex());
            }
        }
        if (!isStatic) {
            info.instanceState = context.getState(cls);
        }

        readSettings(cls, method, context);

        if (context.failed) {
            return null;
        }
        return info;
    }

    private void readSettings(ClassReader cls, MethodReader method, AnalysisContext context) {
        var info = context.info;

        var modeAnnot = findAnnotation(cls, method, BENCHMARK_MODE);
        if (modeAnnot != null) {
            var modes = EnumSet.noneOf(BenchmarkMode.class);
            for (var value : list(modeAnnot.getValue("value"))) {
                var name = value.getEnumValue().getFieldName();
                switch (name) {
                    case "All":
                        modes.addAll(EnumSet.allOf(BenchmarkMode.class));
                        break;
                    case "SampleTime":
                        warnings.add(info.getName() + ": SampleTime mode is not supported, "
                                + "using AverageTime instead");
                        modes.add(BenchmarkMode.AVERAGE_TIME);
                        break;
                    default: {
                        var mode = BenchmarkMode.parse(name);
                        if (mode == null) {
                            context.problem("unsupported benchmark mode " + name);
                        } else {
                            modes.add(mode);
                        }
                        break;
                    }
                }
            }
            if (!modes.isEmpty()) {
                info.modes = modes;
            }
        }

        var timeUnitAnnot = findAnnotation(cls, method, OUTPUT_TIME_UNIT);
        if (timeUnitAnnot != null) {
            info.timeUnit = TimeUnit.valueOf(timeUnitAnnot.getValue("value").getEnumValue().getFieldName());
        }

        var warmupAnnot = findAnnotation(cls, method, WARMUP);
        if (warmupAnnot != null) {
            info.warmup = readIterationSettings(warmupAnnot);
        }
        var measurementAnnot = findAnnotation(cls, method, MEASUREMENT);
        if (measurementAnnot != null) {
            info.measurement = readIterationSettings(measurementAnnot);
        }

        var forkAnnot = findAnnotation(cls, method, FORK);
        if (forkAnnot != null) {
            info.forks = intValue(forkAnnot, "value", -1);
        }

        var opiAnnot = findAnnotation(cls, method, OPERATIONS_PER_INVOCATION);
        if (opiAnnot != null) {
            info.operationsPerInvocation = intValue(opiAnnot, "value", 1);
            if (info.operationsPerInvocation <= 0) {
                context.problem("@OperationsPerInvocation must be positive");
            }
        }
    }

    private IterationSettings readIterationSettings(AnnotationReader annot) {
        var iterations = intValue(annot, "iterations", -1);
        var time = intValue(annot, "time", -1);
        var batchSize = intValue(annot, "batchSize", -1);
        var timeUnitValue = annot.getValue("timeUnit");
        var timeUnit = timeUnitValue != null
                ? TimeUnit.valueOf(timeUnitValue.getEnumValue().getFieldName())
                : TimeUnit.SECONDS;
        return new IterationSettings(iterations, time >= 0 ? timeUnit.toNanos(time) : -1, batchSize);
    }

    private static int intValue(AnnotationReader annot, String name, int defaultValue) {
        var value = annot.getValue(name);
        return value != null ? value.getInt() : defaultValue;
    }

    private static List<AnnotationValue> list(AnnotationValue value) {
        if (value == null) {
            return List.of();
        }
        return value.getType() == AnnotationValue.LIST ? value.getList() : List.of(value);
    }

    private AnnotationReader findAnnotation(ClassReader cls, MethodReader method, String name) {
        var annot = method.getAnnotations().get(name);
        return annot != null ? annot : findClassAnnotation(cls, name);
    }

    private AnnotationReader findClassAnnotation(ClassReader cls, String name) {
        for (var current = cls; current != null; current = superclass(current)) {
            var annot = current.getAnnotations().get(name);
            if (annot != null) {
                return annot;
            }
        }
        return null;
    }

    private class AnalysisContext {
        final BenchmarkInfo info;
        final Map<String, StateInfo> states = new HashMap<>();
        boolean failed;

        AnalysisContext(BenchmarkInfo info) {
            this.info = info;
        }

        void problem(String message) {
            problems.add(info.getName() + ": " + message);
            failed = true;
        }

        StateInfo getState(ClassReader cls) {
            var existing = states.get(cls.getName());
            if (existing != null) {
                return existing;
            }
            var state = new StateInfo(info.states.size(), cls.getName());
            states.put(cls.getName(), state);
            info.states.add(state);

            var hierarchy = new ArrayList<ClassReader>();
            for (var current = cls; current != null; current = superclass(current)) {
                hierarchy.add(0, current);
            }
            var setupMethods = new LinkedHashMap<MethodDescriptor, MethodReader>();
            for (var current : hierarchy) {
                for (var field : current.getFields()) {
                    var paramAnnot = field.getAnnotations().get(PARAM);
                    if (paramAnnot != null) {
                        addParam(state, field, paramAnnot);
                    }
                }
                for (var method : current.getMethods()) {
                    if (method.getAnnotations().get(SETUP) != null || method.getAnnotations().get(TEAR_DOWN) != null) {
                        setupMethods.put(method.getDescriptor(), method);
                    }
                }
            }
            for (var method : setupMethods.values()) {
                addSetupMethod(state, method);
            }
            return state;
        }

        private void addSetupMethod(StateInfo state, MethodReader method) {
            var isSetup = method.getAnnotations().get(SETUP) != null;
            var annot = method.getAnnotations().get(isSetup ? SETUP : TEAR_DOWN);
            var levelValue = annot.getValue("value");
            var level = levelValue != null ? levelValue.getEnumValue().getFieldName() : "Trial";
            if (method.parameterCount() > 0) {
                problem("@" + (isSetup ? "Setup" : "TearDown") + " method " + method.getReference()
                        + " has parameters, which is not supported");
                return;
            }
            if (method.hasModifier(ElementModifier.STATIC)) {
                problem("@" + (isSetup ? "Setup" : "TearDown") + " method " + method.getReference()
                        + " must not be static");
                return;
            }
            switch (level) {
                case "Trial":
                    (isSetup ? state.setupTrial : state.tearDownTrial).add(method.getReference());
                    break;
                case "Iteration":
                    (isSetup ? state.setupIteration : state.tearDownIteration).add(method.getReference());
                    break;
                default:
                    problem("level " + level + " of method " + method.getReference() + " is not supported");
                    break;
            }
        }

        private void addParam(StateInfo state, FieldReader field, AnnotationReader annot) {
            if (field.hasModifier(ElementModifier.STATIC) || field.hasModifier(ElementModifier.FINAL)) {
                problem("@Param field " + field.getReference() + " must be neither static nor final");
                return;
            }
            var type = field.getType();
            var values = new ArrayList<String>();
            for (var value : list(annot.getValue("value"))) {
                values.add(value.getString());
            }
            if (values.size() == 1 && values.get(0).equals("￿￿￿")) {
                // JMH's Param.BLANK_ARGS
                values.clear();
            }

            ClassReader enumClass = null;
            if (type instanceof ValueType.Object) {
                var className = ((ValueType.Object) type).getClassName();
                var cls = classSource.get(className);
                if (cls != null && cls.hasModifier(ElementModifier.ENUM)) {
                    enumClass = cls;
                } else if (!className.equals(String.class.getName()) && !BOXED_TYPES.contains(className)) {
                    problem("@Param field " + field.getReference() + " has unsupported type " + type);
                    return;
                }
            } else if (!(type instanceof ValueType.Primitive)) {
                problem("@Param field " + field.getReference() + " has unsupported type " + type);
                return;
            }

            if (values.isEmpty()) {
                if (enumClass != null) {
                    for (var enumField : enumClass.getFields()) {
                        if (enumField.hasModifier(ElementModifier.ENUM)) {
                            values.add(enumField.getName());
                        }
                    }
                } else if (type == ValueType.BOOLEAN || type.isObject(Boolean.class)) {
                    values.add("false");
                    values.add("true");
                } else {
                    problem("@Param field " + field.getReference() + " does not specify any values");
                    return;
                }
            }

            for (var existing : info.params) {
                if (existing.getName().equals(field.getName())) {
                    problem("@Param field " + field.getReference() + " conflicts with " + existing.getField()
                            + ", parameters must have unique names");
                    return;
                }
            }
            info.params.add(new ParamInfo(info.params.size(), state.getIndex(), field.getReference(), type, values));
        }
    }
}
