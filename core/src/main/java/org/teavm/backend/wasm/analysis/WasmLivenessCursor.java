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
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;

public class WasmLivenessCursor {
    private WasmFunction function;
    private int[][] genSets;
    private int[][] killSets;
    private BitSet liveInSet;
    private BitSet liveOutSet;
    private int index;
    private WasmInstruction instruction;
    private WasmInstruction nextInstruction;

    WasmLivenessCursor(WasmFunction function, int[][] genSets, int[][] killSets, BitSet liveInSet,
            WasmInstruction instruction) {
        this.function = function;
        this.genSets = genSets;
        this.killSets = killSets;
        this.liveInSet = liveInSet;
        this.instruction = instruction;
        advance();
    }

    private void advance() {
        nextInstruction = instruction != null ? instruction.getNext() : null;
        var gen = genSets[index];
        var kill = killSets[index];
        if (gen == null && kill == null) {
            liveOutSet = liveInSet;
        } else {
            liveOutSet = (BitSet) liveInSet.clone();
            if (kill != null) {
                for (int local : kill) {
                    liveOutSet.clear(local);
                }
            }
            if (gen != null) {
                for (int local : gen) {
                    liveOutSet.set(local);
                }
            }
        }
    }

    public WasmInstruction getInstruction() {
        return instruction;
    }

    public boolean isAtEnd() {
        return index == genSets.length - 1;
    }

    public boolean next() {
        if (index == genSets.length) {
            throw new NoSuchElementException();
        }
        var hasNext = ++index < genSets.length - 1;
        liveInSet = liveOutSet;
        instruction = nextInstruction;
        advance();
        return hasNext;
    }

    /**
     * Checks whether local is live right before current instruction. When cursor points to the end
     * of the list, checks whether local is live at the end of the list.
     */
    public boolean isLiveIn(WasmLocal local) {
        return liveInSet.get(local.getIndex());
    }

    /**
     * Checks whether local is live right after current instruction.
     */
    public boolean isLiveOut(WasmLocal local) {
        checkNotAtEnd();
        return liveOutSet.get(local.getIndex());
    }

    public List<WasmLocal> getLiveIn() {
        return toLocals(liveInSet);
    }

    public List<WasmLocal> getLiveOut() {
        checkNotAtEnd();
        return toLocals(liveOutSet);
    }

    public List<? extends WasmLocal> getGenSet() {
        return toLocals(genSets[index]);
    }

    public List<? extends WasmLocal> getKillSet() {
        return toLocals(killSets[index]);
    }

    private void checkNotAtEnd() {
        if (index == genSets.length) {
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

    private List<? extends WasmLocal> toLocals(int[] set) {
        if (set == null) {
            return Collections.emptyList();
        }
        var locals = function.getLocalVariables();
        var result = new ArrayList<WasmLocal>(set.length);
        for (var i : set) {
            result.add(locals.get(i));
        }
        return result;
    }
}
