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

/**
 * <p>Walks Wasm instructions in order of dominator tree, i.e. every instruction is visited after
 * all instructions that dominate it. Along the walk, some state is maintained by subclass. When walk
 * reaches a point whose immediate dominator is not the previous visited point, state is restored to
 * the one that was saved at the dominator.</p>
 *
 * <p>Since Wasm is structured, instructions are visited in their textual order. Points where control flow
 * forks and joins are handled as follows:</p>
 *
 * <ul>
 *   <li>{@code block} and {@code try_table}: body is visited with current state. If body list is a target
 *     of some branch (including exception handler), then the point after the instruction is a join point,
 *     dominated by the point before the instruction, so state saved before the instruction is restored.
 *     Otherwise, the only way to reach the following instruction is to fall through from the end of body,
 *     and the walk continues with the state from the end of body.</li>
 *   <li>{@code loop}: body is visited with current state, since loop header is dominated by the point before
 *     the loop. The point after the loop is reachable only by falling through from the end of body,
 *     so walk continues with the state from the end of body.</li>
 *   <li>{@code if}: both clauses are visited with the state saved before the instruction. If no branch
 *     targets the instruction and only one clause can fall through to its end, the walk continues
 *     with the state from the end of that clause. Otherwise, state saved before the instruction is
 *     restored.</li>
 * </ul>
 *
 * <p>Note that for join points the walker may choose a dominator which is not immediate, for example
 * for a block with branches only from its tail, which is safe, though less precise.</p>
 *
 * <p>Instructions that follow terminating instructions in the same list are visited as well,
 * though they are unreachable and state at them is not meaningful.</p>
 *
 * @param <S> type of state snapshots.
 */
public abstract class WasmDominanceOrderWalker<S> {
    private Set<WasmInstructionList> branchTargets = new HashSet<>();

    public void walk(WasmInstructionList body) {
        new BranchTargetCollector().visitMany(body);
        walkList(body);
        branchTargets.clear();
    }

    /**
     * Takes snapshot of current state. Snapshot must not be affected by further modifications of state.
     */
    protected abstract S saveState();

    /**
     * Restores state from a snapshot. Same snapshot may be restored multiple times.
     */
    protected abstract void restoreState(S state);

    /**
     * Called before visiting instructions of a list.
     */
    protected void enterList(WasmInstructionList list) {
    }

    /**
     * Called after all instructions of a list visited.
     */
    protected void exitList(WasmInstructionList list) {
    }

    /**
     * Called when instruction is visited. For instructions with nested lists, called before nested lists
     * are visited.
     */
    protected void enterInstruction(WasmInstruction instruction) {
    }

    /**
     * Called after instruction is visited. For instructions with nested lists, called after nested
     * lists are visited and the state for the point right after the instruction is established.
     */
    protected void exitInstruction(WasmInstruction instruction) {
    }

    private void walkList(WasmInstructionList list) {
        enterList(list);
        for (var insn = list.getFirst(); insn != null; insn = insn.getNext()) {
            enterInstruction(insn);
            if (insn instanceof WasmBlock block) {
                if (block.isLoop()) {
                    walkList(block.getBody());
                } else {
                    walkJoined(block.getBody());
                }
            } else if (insn instanceof WasmTry) {
                walkJoined(((WasmTry) insn).getBody());
            } else if (insn instanceof WasmConditional) {
                walkConditional((WasmConditional) insn);
            }
            exitInstruction(insn);
        }
        exitList(list);
    }

    private void walkJoined(WasmInstructionList body) {
        if (branchTargets.contains(body)) {
            var state = saveState();
            walkList(body);
            restoreState(state);
        } else {
            walkList(body);
        }
    }

    private void walkConditional(WasmConditional conditional) {
        var thenBlock = conditional.getThenBlock();
        var elseBlock = conditional.getElseBlock();
        var state = saveState();
        var targeted = branchTargets.contains(thenBlock) || branchTargets.contains(elseBlock);
        var thenFallsThrough = fallsThrough(thenBlock);
        var elseFallsThrough = fallsThrough(elseBlock);

        walkList(thenBlock);
        S thenState = null;
        if (!targeted && thenFallsThrough && !elseFallsThrough) {
            thenState = saveState();
        }
        restoreState(state);
        walkList(elseBlock);

        if (thenState != null) {
            restoreState(thenState);
        } else if (targeted || elseFallsThrough == thenFallsThrough) {
            restoreState(state);
        }
    }

    private static boolean fallsThrough(WasmInstructionList list) {
        return list.isEmpty() || !list.getLast().isTerminating();
    }

    private class BranchTargetCollector extends WasmDefaultInstructionVisitor {
        @Override
        public void visit(WasmBranch instruction) {
            branchTargets.add(instruction.getTarget());
        }

        @Override
        public void visit(WasmNullBranch instruction) {
            branchTargets.add(instruction.getTarget());
        }

        @Override
        public void visit(WasmCastBranch instruction) {
            branchTargets.add(instruction.getTarget());
        }

        @Override
        public void visit(WasmBreak instruction) {
            branchTargets.add(instruction.getTarget());
        }

        @Override
        public void visit(WasmSwitch instruction) {
            branchTargets.addAll(instruction.getTargets());
            branchTargets.add(instruction.getDefaultTarget());
        }

        @Override
        public void visit(WasmTry instruction) {
            for (var catchClause : instruction.getCatches()) {
                branchTargets.add(catchClause.getTarget());
            }
            super.visit(instruction);
        }
    }
}
