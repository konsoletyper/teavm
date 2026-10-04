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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.teavm.model.AccessLevel;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.emit.PhiEmitter;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.perf.runtime.BenchmarkEntryPoint;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * Replaces native methods of {@link BenchmarkEntryPoint} with code that creates states, sets parameters,
 * calls setup/teardown methods and the benchmark method itself. Benchmark method is called directly from a loop
 * in {@code runBatch} method, so there are no virtual calls or reflection between the harness and the benchmark.
 */
public class BenchmarkEntryPointTransformer implements ClassHolderTransformer, TeaVMPlugin {
    private static final String ENTRY_POINT = BenchmarkEntryPoint.class.getName();
    private static final String BLACKHOLE_FIELD = "blackhole$";

    private final BenchmarkInfo benchmark;

    public BenchmarkEntryPointTransformer(BenchmarkInfo benchmark) {
        this.benchmark = benchmark;
    }

    @Override
    public void install(TeaVMHost host) {
        host.add(this);
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        if (!cls.getName().equals(ENTRY_POINT)) {
            return;
        }

        for (var state : benchmark.getStates()) {
            var field = new FieldHolder(stateFieldName(state));
            field.setType(ValueType.object(state.getClassName()));
            field.setLevel(AccessLevel.PRIVATE);
            field.getModifiers().add(ElementModifier.STATIC);
            cls.addField(field);
        }
        var blackholeField = new FieldHolder(BLACKHOLE_FIELD);
        blackholeField.setType(ValueType.object(BenchmarkScanner.BLACKHOLE));
        blackholeField.setLevel(AccessLevel.PRIVATE);
        blackholeField.getModifiers().add(ElementModifier.STATIC);
        cls.addField(blackholeField);

        var hierarchy = context.getHierarchy();
        for (var method : cls.getMethods()) {
            if (!method.hasModifier(ElementModifier.NATIVE)) {
                continue;
            }
            switch (method.getName()) {
                case "createStates":
                    generateCreateStates(method, hierarchy);
                    break;
                case "setParam":
                    generateSetParam(method, hierarchy);
                    break;
                case "setupTrial":
                    generateLifecycle(method, hierarchy, false, StateInfo::getSetupTrial);
                    break;
                case "setupIteration":
                    generateLifecycle(method, hierarchy, false, StateInfo::getSetupIteration);
                    break;
                case "tearDownIteration":
                    generateLifecycle(method, hierarchy, true, StateInfo::getTearDownIteration);
                    break;
                case "tearDownTrial":
                    generateLifecycle(method, hierarchy, true, StateInfo::getTearDownTrial);
                    break;
                case "runBatch":
                    generateRunBatch(method, hierarchy);
                    break;
                default:
                    continue;
            }
            method.getModifiers().remove(ElementModifier.NATIVE);
        }
    }

    private static String stateFieldName(StateInfo state) {
        return "state$" + state.getIndex();
    }

    private ValueEmitter getState(ProgramEmitter pe, StateInfo state) {
        return pe.getField(ENTRY_POINT, stateFieldName(state), ValueType.object(state.getClassName()));
    }

    private void generateCreateStates(MethodHolder method, ClassHierarchy hierarchy) {
        var pe = ProgramEmitter.create(method, hierarchy);
        for (var state : benchmark.getStates()) {
            pe.setField(ENTRY_POINT, stateFieldName(state), pe.construct(state.getClassName()));
        }
        pe.setField(ENTRY_POINT, BLACKHOLE_FIELD, pe.construct(BenchmarkScanner.BLACKHOLE));
        pe.exit();
    }

    private void generateSetParam(MethodHolder method, ClassHierarchy hierarchy) {
        var pe = ProgramEmitter.create(method, hierarchy);
        var index = pe.var(1, int.class);
        var value = pe.var(2, String.class);
        for (var param : benchmark.getParams()) {
            var state = benchmark.getStates().get(param.getStateIndex());
            pe.when(index.isEqualTo(pe.constant(param.getIndex()))).thenDo(() -> {
                var parsedValue = parseValue(pe, param.getType(), value);
                getState(pe, state)
                        .cast(ValueType.object(param.getField().getClassName()))
                        .setField(param.getName(), parsedValue);
            });
        }
        pe.exit();
    }

    private ValueEmitter parseValue(ProgramEmitter pe, ValueType type, ValueEmitter value) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return pe.invoke(Boolean.class, "parseBoolean", boolean.class, value);
                case BYTE:
                    return pe.invoke(Byte.class, "parseByte", byte.class, value);
                case SHORT:
                    return pe.invoke(Short.class, "parseShort", short.class, value);
                case CHARACTER:
                    return value.invokeVirtual("charAt", char.class, pe.constant(0));
                case INTEGER:
                    return pe.invoke(Integer.class, "parseInt", int.class, value);
                case LONG:
                    return pe.invoke(Long.class, "parseLong", long.class, value);
                case FLOAT:
                    return pe.invoke(Float.class, "parseFloat", float.class, value);
                case DOUBLE:
                    return pe.invoke(Double.class, "parseDouble", double.class, value);
            }
        }
        var className = ((ValueType.Object) type).getClassName();
        if (className.equals(String.class.getName())) {
            return value;
        }
        // Either one of boxed primitive types or enum, all of them have static valueOf(String) method
        return pe.invoke(className, "valueOf", type, value);
    }

    private void generateLifecycle(MethodHolder method, ClassHierarchy hierarchy, boolean reverse,
            Function<StateInfo, List<MethodReference>> methods) {
        var pe = ProgramEmitter.create(method, hierarchy);
        var states = new ArrayList<>(benchmark.getStates());
        if (reverse) {
            Collections.reverse(states);
        }
        for (var state : states) {
            for (var lifecycleMethod : methods.apply(state)) {
                var instance = getState(pe, state).cast(ValueType.object(lifecycleMethod.getClassName()));
                var reader = hierarchy.getClassSource().resolve(lifecycleMethod);
                if (reader != null && reader.getLevel() == AccessLevel.PRIVATE) {
                    instance.invokeSpecial(lifecycleMethod);
                } else {
                    instance.invokeVirtual(lifecycleMethod);
                }
            }
        }
        pe.exit();
    }

    private void generateRunBatch(MethodHolder method, ClassHierarchy hierarchy) {
        var pe = ProgramEmitter.create(method, hierarchy);
        var count = pe.var(1, int.class);

        var blackhole = pe.getField(ENTRY_POINT, BLACKHOLE_FIELD, ValueType.object(BenchmarkScanner.BLACKHOLE));
        var arguments = new ArrayList<ValueEmitter>();
        for (var argument : benchmark.getArguments()) {
            if (argument == BenchmarkInfo.BLACKHOLE_ARGUMENT) {
                arguments.add(blackhole);
            } else {
                arguments.add(getState(pe, benchmark.getStates().get(argument)));
            }
        }
        ValueEmitter instance = null;
        if (!benchmark.isStatic()) {
            instance = getState(pe, benchmark.getInstanceState());
        }

        BasicBlock loopHead = pe.getProgram().createBasicBlock();
        BasicBlock loopBody = pe.getProgram().createBasicBlock();
        BasicBlock loopExit = pe.getProgram().createBasicBlock();
        PhiEmitter index = pe.phi(int.class, loopHead);
        pe.constant(0).propagateTo(index);
        pe.jump(loopHead);

        pe.enter(loopHead);
        pe.when(index.getValue().isLessThan(count))
                .thenDo(() -> pe.jump(loopBody))
                .elseDo(() -> pe.jump(loopExit));

        pe.enter(loopBody);
        var benchmarkMethod = benchmark.getMethod();
        var argumentArray = arguments.toArray(new ValueEmitter[0]);
        ValueEmitter result;
        if (instance == null) {
            result = pe.invoke(benchmarkMethod, argumentArray);
        } else {
            result = instance.cast(ValueType.object(benchmarkMethod.getClassName()))
                    .invokeVirtual(benchmarkMethod, argumentArray);
        }
        if (result != null) {
            consume(blackhole, result);
        }
        index.getValue().add(1).propagateTo(index);
        pe.jump(loopHead);

        pe.enter(loopExit);
        pe.exit();
    }

    private void consume(ValueEmitter blackhole, ValueEmitter value) {
        var type = value.getType();
        if (!(type instanceof ValueType.Primitive)) {
            value = value.cast(Object.class);
            type = ValueType.object("java.lang.Object");
        }
        blackhole.invokeVirtual(new MethodReference(BenchmarkScanner.BLACKHOLE, "consume", type, ValueType.VOID),
                value);
    }
}
