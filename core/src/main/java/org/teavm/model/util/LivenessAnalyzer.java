/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.model.util;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.IntStack;
import java.util.BitSet;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;

public class LivenessAnalyzer {
    private BitSet[] liveVars;
    private BitSet[] liveOutVars;

    public boolean liveIn(int block, int var) {
        return liveVars[block].get(var);
    }

    public BitSet liveIn(int block) {
        return (BitSet) liveVars[block].clone();
    }

    public BitSet liveOut(int block) {
        return (BitSet) liveOutVars[block].clone();
    }

    public void analyze(Program program) {
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        DominatorTree dominatorTree = GraphUtils.buildDominatorTree(cfg);
        liveVars = new BitSet[cfg.size()];
        liveOutVars = new BitSet[cfg.size()];
        for (int i = 0; i < liveVars.length; ++i) {
            liveVars[i] = new BitSet(program.basicBlockCount());
            liveOutVars[i] = new BitSet(program.basicBlockCount());
        }

        UsageExtractor usageExtractor = new UsageExtractor();
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        IntStack stack = new IntStack();
        int[] definitions = new int[program.variableCount()];
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);

            if (block.getExceptionVariable() != null) {
                definitions[block.getExceptionVariable().getIndex()] = i;
            }

            for (Instruction insn : block) {
                insn.acceptVisitor(usageExtractor);
                IntSet usedVars = new IntHashSet();
                for (Variable var : usageExtractor.getUsedVariables()) {
                    stack.push(i);
                    stack.push(var.getIndex());
                    usedVars.add(var.getIndex());
                }
                insn.acceptVisitor(defExtractor);
                for (Variable var : defExtractor.getDefinedVariables()) {
                    if (!usedVars.contains(var.getIndex())) {
                        definitions[var.getIndex()] = i;
                    }
                }
            }

            for (Phi phi : block.getPhis()) {
                definitions[phi.getReceiver().getIndex()] = i;
                for (Incoming incoming : phi.getIncomings()) {
                    stack.push(incoming.getSource().getIndex());
                    stack.push(incoming.getValue().getIndex());
                }
            }
        }

        while (!stack.isEmpty()) {
            int variable = stack.pop();
            int block = stack.pop();
            BitSet blockLiveVars = liveVars[block];
            if (blockLiveVars.get(variable) || definitions[variable] == block
                    || !dominatorTree.dominates(definitions[variable], block)) {
                continue;
            }
            liveVars[block].set(variable, true);
            for (int pred : cfg.incomingEdges(block)) {
                stack.push(pred);
                stack.push(variable);
            }
        }

        for (int i = 0; i < liveVars.length; ++i) {
            for (int j : cfg.incomingEdges(i)) {
                liveOutVars[j].or(liveVars[i]);
            }
            BasicBlock block = program.basicBlockAt(i);
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    liveOutVars[incoming.getSource().getIndex()].set(incoming.getValue().getIndex());
                }
            }
        }
    }
}
