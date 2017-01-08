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

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;
import org.teavm.common.Graph;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;

public class LivenessAnalyzer {
    private BitSet[] liveVars;

    public boolean liveIn(int block, int var) {
        return liveVars[block].get(var);
    }

    public BitSet liveIn(int block) {
        return (BitSet) liveVars[block].clone();
    }

    public void analyze(Program program) {
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        liveVars = new BitSet[cfg.size()];
        for (int i = 0; i < liveVars.length; ++i) {
            liveVars[i] = new BitSet(program.basicBlockCount());
        }

        UsageExtractor usageExtractor = new UsageExtractor();
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        Deque<Task> stack = new ArrayDeque<>();
        int[] definitions = new int[program.variableCount()];
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);

            if (block.getExceptionVariable() != null) {
                definitions[block.getExceptionVariable().getIndex()] = i;
            }

            for (Instruction insn : block) {
                insn.acceptVisitor(usageExtractor);
                IntSet usedVars = new IntOpenHashSet();
                for (Variable var : usageExtractor.getUsedVariables()) {
                    Task task = new Task();
                    task.block = i;
                    task.var = var.getIndex();
                    stack.push(task);
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
                    Task task = new Task();
                    task.block = incoming.getSource().getIndex();
                    task.var = incoming.getValue().getIndex();
                    stack.push(task);
                }
            }
        }

        while (!stack.isEmpty()) {
            Task task = stack.pop();
            if (liveVars[task.block].get(task.var) || definitions[task.var] == task.block) {
                continue;
            }
            liveVars[task.block].set(task.var, true);
            for (int pred : cfg.incomingEdges(task.block)) {
                Task nextTask = new Task();
                nextTask.block = pred;
                nextTask.var = task.var;
                stack.push(nextTask);
            }
        }
    }

    private static class Task {
        int block;
        int var;
    }
}
