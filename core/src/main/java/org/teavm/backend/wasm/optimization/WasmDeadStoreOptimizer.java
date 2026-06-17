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
package org.teavm.backend.wasm.optimization;

import java.util.ArrayList;
import org.teavm.backend.wasm.analysis.WasmLivenessAnalysis;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmFloat32Constant;
import org.teavm.backend.wasm.model.instruction.WasmFloat64Constant;
import org.teavm.backend.wasm.model.instruction.WasmFunctionReference;
import org.teavm.backend.wasm.model.instruction.WasmGetGlobal;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmInt64Constant;
import org.teavm.backend.wasm.model.instruction.WasmNullConstant;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTry;

/**
 * <p>Removes stores to locals that are never read afterwards, i.e. {@code local.tee} is removed and
 * {@code local.set} is replaced by {@code drop}, if the local is not live after the instruction. Then removes
 * locals that are not referenced anymore.</p>
 *
 * <p>When the value of a dead {@code local.set} is produced by the immediately preceding instruction that takes
 * no operands and has neither side effects nor traps (like {@code i32.const} or {@code local.get}),
 * this instruction is removed along with {@code local.set} instead of producing {@code drop}.</p>
 *
 * <p>Liveness is computed by {@link WasmLivenessAnalysis} before any store is removed. Removing a store
 * never makes any local live, so live sets remain exact during the whole pass and a single walk
 * is enough.</p>
 *
 * <p>Stores to locals of non-nullable reference types are kept, unless the local is never read, since Wasm
 * validation requires them to precede every {@code local.get} in the same block, even in unreachable
 * code.</p>
 */
public class WasmDeadStoreOptimizer {
    private WasmLivenessAnalysis liveness;
    private boolean[] removable;

    public void optimize(WasmFunction function) {
        if (!function.getLocalVariables().isEmpty()) {
            liveness = new WasmLivenessAnalysis(function);
            removable = findRemovable(function);
            processList(function.getBody());
            liveness = null;
            removable = null;
            removeUnusedLocals(function);
        }
    }

    private void processList(WasmInstructionList list) {
        var cursor = liveness.iterate(list);
        while (!cursor.isAtEnd()) {
            var insn = cursor.getInstruction();
            if (insn instanceof WasmBlock) {
                processList(((WasmBlock) insn).getBody());
            } else if (insn instanceof WasmConditional) {
                processList(((WasmConditional) insn).getThenBlock());
                processList(((WasmConditional) insn).getElseBlock());
            } else if (insn instanceof WasmTry) {
                processList(((WasmTry) insn).getBody());
            } else if (insn instanceof WasmTeeLocal) {
                var local = ((WasmTeeLocal) insn).getLocal();
                if (removable[local.getIndex()] && !cursor.isLiveOut(local)) {
                    insn.delete();
                }
            } else if (insn instanceof WasmSetLocal) {
                var local = ((WasmSetLocal) insn).getLocal();
                if (removable[local.getIndex()] && !cursor.isLiveOut(local)) {
                    var previous = insn.getPrevious();
                    if (isPureValue(previous)) {
                        // Cursor works on a snapshot of the list, so deleting passed instruction is safe
                        previous.delete();
                    } else {
                        var drop = new WasmDrop();
                        drop.setLocation(insn.getLocation());
                        insn.insertNext(drop);
                    }
                    insn.delete();
                }
            }
            cursor.next();
        }
    }

    private static boolean isPureValue(WasmInstruction insn) {
        return insn instanceof WasmInt32Constant || insn instanceof WasmInt64Constant
                || insn instanceof WasmFloat32Constant || insn instanceof WasmFloat64Constant
                || insn instanceof WasmNullConstant || insn instanceof WasmGetLocal
                || insn instanceof WasmGetGlobal || insn instanceof WasmFunctionReference;
    }

    private static boolean[] findRemovable(WasmFunction function) {
        var locals = function.getLocalVariables();
        var result = new boolean[locals.size()];
        var hasNonNullable = false;
        for (var i = 0; i < result.length; ++i) {
            var type = locals.get(i).getType();
            result[i] = !(type instanceof WasmType.Reference) || ((WasmType.Reference) type).isNullable();
            hasNonNullable |= !result[i];
        }
        if (hasNonNullable) {
            var read = new boolean[result.length];
            new WasmDefaultInstructionVisitor() {
                @Override
                public void visit(WasmGetLocal instruction) {
                    read[instruction.getLocal().getIndex()] = true;
                }
            }.visitMany(function.getBody());
            for (var i = 0; i < result.length; ++i) {
                result[i] |= !read[i];
            }
        }
        return result;
    }

    private static void removeUnusedLocals(WasmFunction function) {
        var locals = function.getLocalVariables();
        var used = new boolean[locals.size()];
        var parameterCount = Math.min(function.getType().getParameterTypes().size(), locals.size());
        for (var i = 0; i < parameterCount; ++i) {
            used[i] = true;
        }
        new WasmDefaultInstructionVisitor() {
            @Override
            public void visit(WasmGetLocal instruction) {
                used[instruction.getLocal().getIndex()] = true;
            }

            @Override
            public void visit(WasmSetLocal instruction) {
                used[instruction.getLocal().getIndex()] = true;
            }

            @Override
            public void visit(WasmTeeLocal instruction) {
                used[instruction.getLocal().getIndex()] = true;
            }
        }.visitMany(function.getBody());
        var usedLocals = new ArrayList<WasmLocal>();
        for (var i = 0; i < locals.size(); ++i) {
            if (used[i]) {
                usedLocals.add(locals.get(i));
            }
        }
        if (usedLocals.size() < locals.size()) {
            function.replaceLocals(usedLocals);
        }
    }
}
