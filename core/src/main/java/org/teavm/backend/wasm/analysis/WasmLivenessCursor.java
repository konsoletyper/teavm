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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.NoSuchElementException;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;

/**
 * <p>Cursor over instructions of a single list, produced by {@link WasmLivenessAnalysis#iterate}.
 * Initially points to the first instruction of the list. After the last instruction cursor points to
 * the end of the list, where {@link #getInstruction()} returns {@code null}.</p>
 *
 * <p>Live-in set is the set of locals live right before current instruction. It takes into account
 * all successors of the instruction, including branch targets and exception handlers.
 * Live-out set is the set of locals live right after current instruction, i.e. at the point where
 * control falls through to the next instruction of the list. For instructions that never fall
 * through (like {@code br} or {@code return}) live-out set is meaningless.</p>
 */
public class WasmLivenessCursor {
    private WasmFunction function;
    private WasmInstruction[] instructions;
    private BitSet[] liveSets;
    private int index;

    WasmLivenessCursor(WasmFunction function, WasmInstruction[] instructions, BitSet[] liveSets) {
        this.function = function;
        this.instructions = instructions;
        this.liveSets = liveSets;
    }

    /**
     * Returns current instruction, or {@code null} if cursor points to the end of the list.
     */
    public WasmInstruction getInstruction() {
        return index < instructions.length ? instructions[index] : null;
    }

    public boolean isAtEnd() {
        return index == instructions.length;
    }

    /**
     * Moves cursor to the next instruction.
     *
     * @return {@code false} if cursor reached the end of the list.
     */
    public boolean next() {
        if (index == instructions.length) {
            throw new NoSuchElementException();
        }
        return ++index < instructions.length;
    }

    /**
     * Checks whether local is live right before current instruction. When cursor points to the end
     * of the list, checks whether local is live at the end of the list.
     */
    public boolean isLiveIn(WasmLocal local) {
        return liveSets[index].get(local.getIndex());
    }

    /**
     * Checks whether local is live right after current instruction.
     */
    public boolean isLiveOut(WasmLocal local) {
        checkNotAtEnd();
        return liveSets[index + 1].get(local.getIndex());
    }

    public List<WasmLocal> getLiveIn() {
        return toLocals(liveSets[index]);
    }

    public List<WasmLocal> getLiveOut() {
        checkNotAtEnd();
        return toLocals(liveSets[index + 1]);
    }

    private void checkNotAtEnd() {
        if (index == instructions.length) {
            throw new IllegalStateException("Cursor points to the end of the list");
        }
    }

    private List<WasmLocal> toLocals(BitSet set) {
        var locals = function.getLocalVariables();
        var result = new ArrayList<WasmLocal>(set.cardinality());
        for (var i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
            result.add(locals.get(i));
        }
        return result;
    }
}
