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
package org.teavm.backend.wasm.analysis;

import java.util.HashSet;
import java.util.Set;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmCastBranch;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmNullBranch;
import org.teavm.backend.wasm.model.instruction.WasmSwitch;
import org.teavm.backend.wasm.model.instruction.WasmTry;

public class WasmInstructionReachabilityAnalysis extends WasmDefaultInstructionVisitor {
    private Set<WasmInstruction> unreachables = new HashSet<>();
    private Set<WasmInstruction> reachables = new HashSet<>();

    public boolean isUnreachable(WasmInstruction instruction) {
        return instruction.isTerminating() || unreachables.contains(instruction);
    }

    @Override
    public void visit(WasmBranch instruction) {
        reachables.add(instruction.getTarget().getBreakTarget());
    }

    @Override
    public void visit(WasmNullBranch instruction) {
        reachables.add(instruction.getTarget().getBreakTarget());
    }

    @Override
    public void visit(WasmCastBranch instruction) {
        reachables.add(instruction.getTarget().getBreakTarget());
    }

    @Override
    public void visit(WasmSwitch instruction) {
        reachables.add(instruction.getDefaultTarget().getBreakTarget());
        for (var target : instruction.getTargets()) {
            reachables.add(target.getBreakTarget());
        }
    }

    @Override
    public void visit(WasmBreak instruction) {
        reachables.add(instruction.getTarget().getBreakTarget());
    }

    @Override
    public void visit(WasmBlock instruction) {
        super.visit(instruction);
        var reachable = reachables.remove(instruction);
        if ((!reachable || instruction.isLoop()) && isUnreachable(instruction.getBody())) {
            unreachables.add(instruction);
        }
    }

    @Override
    public void visit(WasmConditional instruction) {
        super.visit(instruction);
        var reachable = reachables.remove(instruction);
        if (!reachable && isUnreachable(instruction.getThenBlock()) && isUnreachable(instruction.getElseBlock())) {
            unreachables.add(instruction);
        }
    }

    @Override
    public void visit(WasmTry instruction) {
        super.visit(instruction);
        if (isUnreachable(instruction.getBody())) {
            unreachables.add(instruction);
        }
        for (var catchClause : instruction.getCatches()) {
            reachables.add(catchClause.getTarget().getBreakTarget());
        }
    }

    private boolean isUnreachable(WasmInstructionList list) {
        return list.getLast() != null && isUnreachable(list.getLast());
    }
}
