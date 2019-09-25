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

import com.carrotsearch.hppc.IntStack;
import java.util.BitSet;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
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

    public void analyze(Program program, MethodDescriptor descriptor) {
        analyze(program, descriptor.parameterCount() + 1);
    }

    public void analyze(Program program, int parameterCount) {
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        DominatorTree dominatorTree = GraphUtils.buildDominatorTree(cfg);
        BitSet[] visited = new BitSet[program.basicBlockCount()];
        liveVars = new BitSet[program.basicBlockCount()];
        liveOutVars = new BitSet[program.basicBlockCount()];
        for (int i = 0; i < liveVars.length; ++i) {
            visited[i] = new BitSet(program.variableCount());
            liveVars[i] = new BitSet(program.variableCount());
            liveOutVars[i] = new BitSet(program.variableCount());
        }

        UsageExtractor usageExtractor = new UsageExtractor();
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        IntStack stack = new IntStack();
        int[] definitions = new int[program.variableCount()];
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);

            BitSet usedVariables = new BitSet(program.variableCount());

            for (Instruction insn = block.getLastInstruction(); insn != null; insn = insn.getPrevious()) {
                insn.acceptVisitor(defExtractor);
                for (Variable var : defExtractor.getDefinedVariables()) {
                    definitions[var.getIndex()] = i;
                    usedVariables.clear(var.getIndex());
                }
                insn.acceptVisitor(usageExtractor);
                for (Variable var : usageExtractor.getUsedVariables()) {
                    usedVariables.set(var.getIndex());
                }
            }

            for (Phi phi : block.getPhis()) {
                definitions[phi.getReceiver().getIndex()] = i;
                usedVariables.clear(phi.getReceiver().getIndex());
                for (Incoming incoming : phi.getIncomings()) {
                    stack.push(incoming.getSource().getIndex());
                    stack.push(incoming.getValue().getIndex());
                }
            }

            if (block.getExceptionVariable() != null) {
                definitions[block.getExceptionVariable().getIndex()] = i;
                usedVariables.clear(block.getExceptionVariable().getIndex());
            }

            if (i == 0) {
                for (int v = 0; v < parameterCount; ++v) {
                    definitions[v] = 0;
                    usedVariables.clear(v);
                }
            }

            int[] predecessors = cfg.incomingEdges(i);
            for (int v = usedVariables.nextSetBit(0); v >= 0; v = usedVariables.nextSetBit(v + 1)) {
                liveVars[i].set(v);
                for (int pred : predecessors) {
                    stack.push(pred);
                    stack.push(v);
                }
            }
        }

        while (!stack.isEmpty()) {
            int variable = stack.pop();
            int block = stack.pop();
            BitSet blockVisited = visited[block];
            if (blockVisited.get(variable)) {
                continue;
            }
            blockVisited.set(variable);

            if (definitions[variable] == block || !dominatorTree.dominates(definitions[variable], block)) {
                continue;
            }
            liveVars[block].set(variable, true);
            for (int pred : cfg.incomingEdges(block)) {
                if (!visited[pred].get(variable)) {
                    stack.push(pred);
                    stack.push(variable);
                }
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
