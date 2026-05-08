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
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmCastBranch;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmNullBranch;
import org.teavm.backend.wasm.model.instruction.WasmSwitch;

public class BreakTargetReplacement extends WasmDefaultInstructionVisitor {
    private Function<WasmInstructionList, WasmInstructionList> mapping;

    public BreakTargetReplacement(Function<WasmInstructionList, WasmInstructionList> mapping) {
        this.mapping = mapping;
    }

    @Override
    public void visit(WasmBreak instruction) {
        instruction.setTarget(map(instruction.getTarget()));
        super.visit(instruction);
    }

    @Override
    public void visit(WasmBranch instruction) {
        instruction.setTarget(map(instruction.getTarget()));
        super.visit(instruction);
    }

    @Override
    public void visit(WasmCastBranch instruction) {
        instruction.setTarget(map(instruction.getTarget()));
        super.visit(instruction);
    }

    @Override
    public void visit(WasmNullBranch instruction) {
        instruction.setTarget(map(instruction.getTarget()));
        super.visit(instruction);
    }

    @Override
    public void visit(WasmSwitch instruction) {
        instruction.setDefaultTarget(map(instruction.getDefaultTarget()));
        for (var i = 0; i < instruction.getTargets().size(); ++i) {
            instruction.getTargets().set(i, map(instruction.getTargets().get(i)));
        }
        super.visit(instruction);
    }

    private WasmInstructionList map(WasmInstructionList list) {
        var result = mapping.apply(list);
        return result != null ? result : list;
    }
}
