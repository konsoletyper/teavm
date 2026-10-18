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
import org.teavm.backend.wasm.model.instruction.WasmTypeInference;
import org.teavm.common.DisjointSet;
import org.teavm.common.GraphBuilder;

public class WasmLocalAssignmentFinder extends WasmDefaultInstructionVisitor {
    private final GraphBuilder graphBuilder;

    public WasmLocalAssignmentFinder(GraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
    }

    @Override
    public void visitMany(WasmInstructionList instructions) {
        visitMany(instructions, Collections.emptyList());
    }

    public void visitMany(WasmInstructionList instructions, WasmBlockType type) {
        visitMany(instructions, type != null ? type.getInputTypes() : Collections.emptyList());
    }

    public void visitMany(WasmInstructionList instructions, List<? extends WasmType> inputs) {
        var typeInference = new WasmTypeInference();
        typeInference.typeStack.addAll(inputs);
        var reads = new ArrayList<WasmLocal>(Collections.nCopies(inputs.size(), null));
        for (var insn : instructions) {
            if (insn instanceof WasmSetLocal set) {
                var source = reads.get(reads.size() - 1);
                if (source != null) {
                    graphBuilder.addEdge(source.getIndex(), set.getLocal().getIndex());
                }
            } else if (insn instanceof WasmTeeLocal tee) {
                var source = reads.get(reads.size() - 1);
                if (source != null) {
                    graphBuilder.addEdge(source.getIndex(), tee.getLocal().getIndex());
                }
                reads.set(reads.size() - 1, tee.getLocal());
            }
            insn.acceptVisitor(this);
            insn.acceptVisitor(typeInference);
            if (reads.size() > typeInference.getDepthBeforeLastInstructionOut()) {
                reads.subList(typeInference.getDepthBeforeLastInstructionOut(), reads.size()).clear();
            }
            if (reads.size() < typeInference.typeStack.size()) {
                reads.addAll(Collections.nCopies(typeInference.typeStack.size() - reads.size(), null));
            }
            if (insn instanceof WasmGetLocal get) {
                reads.set(reads.size() - 1, get.getLocal());
            }
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
}
