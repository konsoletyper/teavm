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
package org.teavm.model.optimization;

import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public final class VariableUsageGraphBuilder {
    private VariableUsageGraphBuilder() {
    }

    public static Graph build(Program program) {
        GraphBuilder builder = new GraphBuilder(program.variableCount());
        InstructionAnalyzer analyzer = new InstructionAnalyzer(builder);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                insn.acceptVisitor(analyzer);
            }
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    builder.addEdge(incoming.getValue().getIndex(), phi.getReceiver().getIndex());
                }
            }
        }
        return builder.build();
    }

    private static class InstructionAnalyzer extends AbstractInstructionVisitor {
        private GraphBuilder builder;

        public InstructionAnalyzer(GraphBuilder builder) {
            this.builder = builder;
        }

        private void use(Variable receiver, Variable... arguments) {
            for (Variable arg : arguments) {
                builder.addEdge(arg.getIndex(), receiver.getIndex());
            }
        }

        @Override
        public void visit(BinaryInstruction insn) {
            use(insn.getReceiver(), insn.getFirstOperand(), insn.getSecondOperand());
        }

        @Override
        public void visit(NegateInstruction insn) {
            use(insn.getReceiver(), insn.getOperand());
        }

        @Override
        public void visit(AssignInstruction insn) {
            use(insn.getReceiver(), insn.getAssignee());
        }

        @Override
        public void visit(CastInstruction insn) {
            use(insn.getReceiver(), insn.getValue());
        }

        @Override
        public void visit(CastNumberInstruction insn) {
            use(insn.getReceiver(), insn.getValue());
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            use(insn.getReceiver(), insn.getValue());
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            use(insn.getReceiver(), insn.getSize());
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            use(insn.getReceiver(), insn.getDimensions().toArray(new Variable[0]));
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            if (insn.getInstance() != null) {
                use(insn.getReceiver(), insn.getInstance());
            }
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
            use(insn.getReceiver(), insn.getArray());
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            use(insn.getReceiver(), insn.getArray());
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            use(insn.getReceiver(), insn.getArray());
        }

        @Override
        public void visit(GetElementInstruction insn) {
            use(insn.getReceiver(), insn.getArray(), insn.getIndex());
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            use(insn.getReceiver(), insn.getValue());
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            use(insn.getReceiver(), insn.getValue());
        }
    }
}
