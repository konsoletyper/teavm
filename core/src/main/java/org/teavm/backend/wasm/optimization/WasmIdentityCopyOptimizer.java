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
import java.util.Collections;
import java.util.List;
import org.teavm.backend.wasm.model.WasmBlockType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTry;
import org.teavm.backend.wasm.model.instruction.WasmTypeInference;

public class WasmIdentityCopyOptimizer extends WasmDefaultInstructionVisitor {
    private List<WasmLocal> writtenLocals = new ArrayList<>();

    @Override
    public void visitMany(WasmInstructionList instructions) {
        visitMany(instructions, null);
        writtenLocals.clear();
    }

    public void visitMany(WasmInstructionList instructions, WasmBlockType type) {
        var initialTypes = type != null ? type.getInputTypes() : Collections.<WasmType>emptyList();
        var typeInference = new WasmTypeInference();
        typeInference.typeStack.addAll(initialTypes);
        var reads = new ArrayList<WasmGetLocal>(Collections.nCopies(initialTypes.size(), null));
        for (var insn = instructions.getFirst(); insn != null;) {
            var next = insn.getNext();
            var writesStart = writtenLocals.size();
            if (insn instanceof WasmSetLocal set) {
                var get = reads.get(reads.size() - 1);
                if (get != null && get.getLocal() == set.getLocal()) {
                    set.delete();
                    get.delete();
                } else {
                    writtenLocals.add(set.getLocal());
                }
            } else if (insn instanceof WasmTeeLocal tee) {
                var get = reads.get(reads.size() - 1);
                if (get != null && get.getLocal() == tee.getLocal()) {
                    get.delete();
                    get = new WasmGetLocal(tee.getLocal());
                    get.setLocation(tee.getLocation());
                    tee.insertPrevious(get);
                    tee.delete();
                    reads.set(reads.size() - 1, get);
                } else {
                    writtenLocals.add(tee.getLocal());
                }
            }
            insn.acceptVisitor(this);
            insn.acceptVisitor(typeInference);
            if (reads.size() > typeInference.getDepthBeforeLastInstructionOut()) {
                reads.subList(typeInference.getDepthBeforeLastInstructionOut(), reads.size()).clear();
            }
            // values read from locals that this instruction (or instructions nested in it) overwrote
            // are no longer equal to these locals
            for (var i = writesStart; i < writtenLocals.size(); ++i) {
                var written = writtenLocals.get(i);
                for (var j = 0; j < reads.size(); ++j) {
                    var read = reads.get(j);
                    if (read != null && read.getLocal() == written) {
                        reads.set(j, null);
                    }
                }
            }
            if (reads.size() < typeInference.typeStack.size()) {
                reads.addAll(Collections.nCopies(typeInference.typeStack.size() - reads.size(), null));
            }
            if (insn instanceof WasmGetLocal get) {
                reads.set(reads.size() - 1, get);
            }
            insn = next;
        }
    }

    @Override
    public void visit(WasmBlock instruction) {
        visitMany(instruction.getBody(), instruction.getType());
    }

    @Override
    public void visit(WasmConditional instruction) {
        visitMany(instruction.getThenBlock(), instruction.getType());
        visitMany(instruction.getElseBlock(), instruction.getType());
    }

    @Override
    public void visit(WasmTry instruction) {
        visitMany(instruction.getBody(), null);
    }
}
