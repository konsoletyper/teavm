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

import java.util.HashSet;
import java.util.Set;
import org.teavm.backend.wasm.model.WasmFunction;
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
import org.teavm.backend.wasm.model.instruction.WasmUnreachable;

/**
 * <p>Removes instructions that are never executed.</p>
 *
 * <p>Function body is walked forward once, tracking whether current point is reachable. Point after
 * a terminating instruction is unreachable. Point after a block, conditional or {@code try_table} is reachable
 * if control falls through the end of its body (any clause of the conditional) or if it's targeted
 * by a branch that is reachable itself. Since all branches except branches to loops target points
 * that follow the branch, and branches to loops don't affect reachability, a single walk is enough.
 * Branches in removed code don't count, so removing them may make more code unreachable.</p>
 *
 * <p>Unreachable code always forms a tail of an instruction list. When it follows an instruction that
 * is not terminating (i.e. block, conditional or {@code try_table} that never completes), Wasm validation
 * still considers the following point reachable, and the operand stack must be consistent with
 * the end of the list. In this case unreachable tail is replaced with {@code unreachable} instruction.</p>
 */
public class WasmDeadCodeEliminator extends WasmDefaultInstructionVisitor {
    private Set<WasmInstruction> targets = new HashSet<>();
    private boolean reachable;

    public void optimize(WasmFunction function) {
        reachable = true;
        visitMany(function.getBody());
        targets.clear();
    }

    @Override
    public void visitMany(WasmInstructionList instructions) {
        for (var insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
            visitDefault(insn);
            if (insn.isTerminating()) {
                reachable = false;
            }
            if (!reachable) {
                removeTail(insn);
                break;
            }
        }
    }

    private void removeTail(WasmInstruction insn) {
        if (insn.getNext() == null) {
            return;
        }
        while (insn.getNext() != null) {
            insn.getNext().delete();
        }
        if (!insn.isTerminating()) {
            insn.insertNext(new WasmUnreachable());
        }
    }

    @Override
    public void visit(WasmBlock instruction) {
        super.visit(instruction);
        if (targets.remove(instruction) && !instruction.isLoop()) {
            reachable = true;
        }
    }

    @Override
    public void visit(WasmConditional instruction) {
        visitMany(instruction.getThenBlock());
        var thenReachable = reachable;
        reachable = true;
        visitMany(instruction.getElseBlock());
        reachable |= thenReachable;
        if (targets.remove(instruction)) {
            reachable = true;
        }
    }

    @Override
    public void visit(WasmTry instruction) {
        for (var catchClause : instruction.getCatches()) {
            addTarget(catchClause.getTarget());
        }
        super.visit(instruction);
        if (targets.remove(instruction)) {
            reachable = true;
        }
    }

    @Override
    public void visit(WasmBranch instruction) {
        addTarget(instruction.getTarget());
    }

    @Override
    public void visit(WasmNullBranch instruction) {
        addTarget(instruction.getTarget());
    }

    @Override
    public void visit(WasmCastBranch instruction) {
        addTarget(instruction.getTarget());
    }

    @Override
    public void visit(WasmBreak instruction) {
        addTarget(instruction.getTarget());
    }

    @Override
    public void visit(WasmSwitch instruction) {
        addTarget(instruction.getDefaultTarget());
        for (var target : instruction.getTargets()) {
            addTarget(target);
        }
    }

    private void addTarget(WasmInstructionList target) {
        var breakTarget = target.getBreakTarget();
        if (breakTarget != null) {
            targets.add(breakTarget);
        }
    }
}
