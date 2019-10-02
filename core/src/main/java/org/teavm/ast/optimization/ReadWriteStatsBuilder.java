/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.ast.optimization;

import java.util.Arrays;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.common.IntegerStack;
import org.teavm.model.*;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.UsageExtractor;

class ReadWriteStatsBuilder {
    public int[] reads;
    public int[] writes;
    public Object[] constants;

    private ReadWriteStatsBuilder() {
    }

    public ReadWriteStatsBuilder(int variableCount) {
        reads = new int[variableCount];
        writes = new int[variableCount];
        constants = new Object[variableCount];
    }

    public ReadWriteStatsBuilder copy() {
        ReadWriteStatsBuilder result = new ReadWriteStatsBuilder();
        result.reads = Arrays.copyOf(reads, reads.length);
        result.writes = Arrays.copyOf(writes, writes.length);
        return result;
    }

    public void analyze(Program program) {
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        Graph dom = GraphUtils.buildDominatorGraph(GraphUtils.buildDominatorTree(cfg), cfg.size());
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        UsageExtractor useExtractor = new UsageExtractor();
        IntegerStack stack = new IntegerStack(program.basicBlockCount());
        stack.push(0);
        ConstantExtractor constantExtractor = new ConstantExtractor(constants);
        while (!stack.isEmpty()) {
            int node = stack.pop();
            BasicBlock block = program.basicBlockAt(node);

            if (block.getExceptionVariable() != null) {
                writes[block.getExceptionVariable().getIndex()]++;
                reads[block.getExceptionVariable().getIndex()]++;
            }

            for (Instruction insn : block) {
                insn.acceptVisitor(defExtractor);
                insn.acceptVisitor(useExtractor);
                for (Variable var : defExtractor.getDefinedVariables()) {
                    writes[var.getIndex()]++;
                }
                for (Variable var : useExtractor.getUsedVariables()) {
                    reads[var.getIndex()]++;
                }

                insn.acceptVisitor(constantExtractor);
            }

            for (Phi phi : block.getPhis()) {
                writes[phi.getReceiver().getIndex()] += phi.getIncomings().size();
                for (Incoming incoming : phi.getIncomings()) {
                    if (writes[incoming.getValue().getIndex()] == 0) {
                        reads[incoming.getValue().getIndex()]++;
                    }
                }
            }

            for (int succ : dom.outgoingEdges(node)) {
                stack.push(succ);
            }
        }
    }

    static class ConstantExtractor extends AbstractInstructionVisitor {
        private Object[] constants;

        ConstantExtractor(Object[] constants) {
            this.constants = constants;
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            constants[insn.getReceiver().getIndex()] = insn.getConstant();
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            constants[insn.getReceiver().getIndex()] = insn.getConstant();
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            constants[insn.getReceiver().getIndex()] = insn.getConstant();
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            constants[insn.getReceiver().getIndex()] = insn.getConstant();
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            constants[insn.getReceiver().getIndex()] = insn.getConstant();
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            constants[insn.getReceiver().getIndex()] = insn.getConstant();
        }
    }
}
