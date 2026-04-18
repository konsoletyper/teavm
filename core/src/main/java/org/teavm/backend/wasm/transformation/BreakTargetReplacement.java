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
package org.teavm.backend.wasm.transformation;

import java.util.function.Function;
import org.teavm.backend.wasm.model.instruction.WasmBranchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmBreakInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCastBranchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmNullBranchInstruction;

public class BreakTargetReplacement extends WasmDefaultInstructionVisitor {
    private Function<WasmInstructionList, WasmInstructionList> mapping;

    public BreakTargetReplacement(Function<WasmInstructionList, WasmInstructionList> mapping) {
        this.mapping = mapping;
    }

    @Override
    public void visit(WasmBreakInstruction instruction) {

        super.visit(instruction);
    }

    @Override
    public void visit(WasmBranchInstruction instruction) {
        super.visit(instruction);
    }

    @Override
    public void visit(WasmCastBranchInstruction instruction) {
        super.visit(instruction);
    }

    @Override
    public void visit(WasmNullBranchInstruction instruction) {
        super.visit(instruction);
    }

    private WasmInstructionList map(WasmInstructionList list) {
        var result = mapping.apply(list);
        return result != null ? result : list;
    }
}
