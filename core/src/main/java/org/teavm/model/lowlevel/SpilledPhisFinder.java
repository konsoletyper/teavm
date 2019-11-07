/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.model.lowlevel;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import org.teavm.common.DominatorTree;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.Phi;
import org.teavm.model.Program;

class SpilledPhisFinder {
    private static final byte VISITING = 1;
    private static final byte VISITED = 2;
    private DominatorTree dom;
    boolean[] autoSpilled;
    byte[] status;
    int[][] variableSpilledBlocks;
    Phi[] definingPhis;
    int variableCount;
    private int[] variableClasses;
    private int[] colors;

    SpilledPhisFinder(List<Map<Instruction, BitSet>> liveInInformation, DominatorTree dom, Program program,
            int[] variableClasses, int[] colors) {
        this.dom = dom;
        variableCount = program.variableCount();
        autoSpilled = new boolean[variableCount];
        status = new byte[variableCount];
        variableSpilledBlocks = variableSpilledBlocks(liveInInformation, variableCount, variableClasses);
        definingPhis = findPhis(program, variableClasses);
        this.variableClasses = variableClasses;
        this.colors = colors;
    }

    private static int[][] variableSpilledBlocks(List<Map<Instruction, BitSet>> liveInInformation, int count,
            int[] variableClasses) {
        IntSet[] builder = new IntSet[count];
        for (int b = 0; b < liveInInformation.size(); b++) {
            Map<Instruction, BitSet> blockLiveIn = liveInInformation.get(b);
            for (BitSet liveVarsSet : blockLiveIn.values()) {
                for (int v = liveVarsSet.nextSetBit(0); v >= 0; v = liveVarsSet.nextSetBit(v + 1)) {
                    int cls = variableClasses[v];
                    if (builder[cls] == null) {
                        builder[cls] = new IntHashSet();
                    }
                    builder[cls].add(b);
                }
            }
        }

        int[][] result = new int[builder.length][];
        for (int v = 0; v < result.length; ++v) {
            if (builder[v] != null) {
                result[v] = builder[v].toArray();
                Arrays.sort(result[v]);
            }
        }
        return result;
    }

    private static Phi[] findPhis(Program program, int[] variableClasses) {
        Phi[] result = new Phi[program.variableCount()];
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Phi phi : block.getPhis()) {
                result[variableClasses[phi.getReceiver().getIndex()]] = phi;
            }
        }
        return result;
    }

    boolean[] find() {
        for (int v = 0; v < variableCount; ++v) {
            isAutoSpilled(v);
        }
        return autoSpilled;
    }

    private boolean isAutoSpilled(int v) {
        v = variableClasses[v];

        if (status[v] == VISITED) {
            return autoSpilled[v];
        }

        if (status[v] == VISITING) {
            return false;
        }

        Phi definingPhi = definingPhis[v];
        if (definingPhi == null) {
            status[v] = VISITED;
            return false;
        }

        status[v] = VISITING;

        boolean result = true;
        for (Incoming incoming : definingPhi.getIncomings()) {
            if (colors[incoming.getValue().getIndex()] != colors[definingPhi.getReceiver().getIndex()]) {
                result = false;
                break;
            }
            if (!isAutoSpilled(incoming.getValue().getIndex())) {
                int[] spilledAt = variableSpilledBlocks[incoming.getValue().getIndex()];
                result = false;
                if (spilledAt != null) {
                    for (int spilledAtBlock : spilledAt) {
                        if (dom.dominates(spilledAtBlock, incoming.getSource().getIndex())) {
                            result = true;
                            break;
                        }
                    }
                }
                if (!result) {
                    break;
                }
            }
        }

        autoSpilled[v] = result;
        status[v] = VISITED;
        return result;
    }
}
