/*
 *  Copyright 2023 ihromant.
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
package org.teavm.classlib.impl;

import org.teavm.dependency.BootstrapMethodSubstitutor;
import org.teavm.dependency.DynamicCallSite;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.DynamicConstant;
import org.teavm.model.MethodReference;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.emit.PhiEmitter;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;

public class SwitchBootstrapSubstitutor implements BootstrapMethodSubstitutor {
    private static final String CONSTANT_BOOTSTRAPS = "java.lang.invoke.ConstantBootstraps";

    @Override
    public ValueEmitter substitute(DynamicCallSite callSite, ProgramEmitter pe) {
        boolean enumSwitch = callSite.getBootstrapMethod().getName().equals("enumSwitch");
        var labels = callSite.getBootstrapArguments();
        ValueEmitter target = callSite.getArguments().get(0);
        ValueEmitter restartIdx = callSite.getArguments().get(1);
        BasicBlock joint = pe.prepareBlock();
        PhiEmitter result = pe.phi(ValueType.INTEGER, joint);
        pe.when(() -> target.isNull()).thenDo(() -> {
            pe.constant(-1).propagateTo(result);
            pe.jump(joint);
        });

        var switchInsn = new SwitchInstruction();
        switchInsn.setCondition(restartIdx.getVariable());
        pe.addInstruction(switchInsn);

        var block = pe.prepareBlock();
        pe.enter(block);
        var enumType = enumSwitch
                ? (ValueType.Object) callSite.getCalledMethod().parameterType(0)
                : null;
        if (enumType != null) {
            pe.initClass(enumType.getClassName());
        }
        for (var i = 0; i < labels.size(); ++i) {
            var entry = new SwitchTableEntry();
            entry.setCondition(i);
            entry.setTarget(block);
            switchInsn.getEntries().add(entry);

            var label = labels.get(i);
            block = pe.prepareBlock();
            if (emitFragment(target, i, label, pe, result, joint, enumType, callSite.getAgent().getDiagnostics(),
                    callSite.getLocation(), callSite.getCaller())) {
                pe.jump(block);
            }
            pe.enter(block);
        }

        switchInsn.setDefaultTarget(block);

        pe.constant(callSite.getBootstrapArguments().size()).propagateTo(result);
        pe.jump(joint);
        pe.enter(joint);
        return result.getValue();
    }

    private boolean emitFragment(ValueEmitter target, int idx, RuntimeConstant label, ProgramEmitter pe,
            PhiEmitter result, BasicBlock exit, ValueType.Object enumType, Diagnostics diagnostics,
            TextLocation location, MethodReference caller) {
        switch (label.getKind()) {
            case RuntimeConstant.TYPE:
                ValueType type = label.getValueType();
                emitTypeFragment(target, idx, type, pe, result, exit);
                break;
            case RuntimeConstant.INT:
                int val = label.getInt();
                pe.when(() -> target.instanceOf(ValueType.object("java.lang.Number")).isTrue()
                        .and(() -> target.cast(Number.class)
                                .invokeVirtual("intValue", int.class).isEqualTo(pe.constant(val))))
                        .thenDo(() -> {
                            pe.constant(idx).propagateTo(result);
                            pe.jump(exit);
                        });
                pe.when(() -> target.instanceOf(ValueType.object("java.lang.Character")).isTrue()
                        .and(() -> target.cast(Character.class)
                                .invokeSpecial("charValue", char.class).isEqualTo(pe.constant(val))))
                        .thenDo(() -> {
                                    pe.constant(idx).propagateTo(result);
                                    pe.jump(exit);
                                });
                break;
            case RuntimeConstant.STRING:
                String str = label.getString();
                pe.when(enumType != null
                                ? () -> pe.getField(enumType.getClassName(), str, enumType).isSame(target)
                                : () -> pe.constant(str).invokeVirtual("equals", boolean.class,
                                        target.cast(Object.class)).isTrue())
                        .thenDo(() -> {
                            pe.constant(idx).propagateTo(result);
                            pe.jump(exit);
                        });
                break;
            case RuntimeConstant.DYNAMIC_CONSTANT:
                return handleDynamicConstant(idx, label.getDynamicConstant(), pe, result, exit,
                        diagnostics, location, caller);
            default:
                throw new IllegalArgumentException("Unsupported constant type: " + label.getKind());
        }
        return true;
    }

    private boolean handleDynamicConstant(int idx, DynamicConstant cst, ProgramEmitter pe,
            PhiEmitter result, BasicBlock exit, Diagnostics diagnostics, TextLocation location,
            MethodReference caller) {
        if (cst.bootstrapMethod.getClassName().equals(CONSTANT_BOOTSTRAPS)) {
            switch (cst.bootstrapMethod.getName()) {
                case "primitiveClass": {
                    pe.constant(idx).propagateTo(result);
                    pe.jump(exit);
                    return false;
                }
            }
        }
        var bsmRef = new MethodReference(cst.bootstrapMethod.getClassName(), cst.bootstrapMethod.getName(),
                cst.bootstrapMethod.signature());
        diagnostics.error(new CallLocation(caller, location), "Unsupported dynamic constant: {m0}",
                bsmRef);
        return false;
    }

    private void emitTypeFragment(ValueEmitter target, int idx, ValueType type, ProgramEmitter pe,
            PhiEmitter result, BasicBlock exit) {
        pe.when(() -> target.instanceOf(type).isTrue())
                .thenDo(() -> {
                    pe.constant(idx).propagateTo(result);
                    pe.jump(exit);
                });
    }
}
