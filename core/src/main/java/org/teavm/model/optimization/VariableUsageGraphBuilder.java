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
import org.teavm.model.*;
import org.teavm.model.instructions.*;

public final class VariableUsageGraphBuilder {
    private VariableUsageGraphBuilder() {
    }

    public static Graph build(Program program) {
        GraphBuilder builder = new GraphBuilder(program.variableCount());
        InstructionAnalyzer analyzer = new InstructionAnalyzer(builder);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block.getInstructions()) {
                insn.acceptVisitor(analyzer);
            }
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    builder.addEdge(incoming.getValue().getIndex(), phi.getReceiver().getIndex());
                }
            }
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                for (TryCatchJoint joint : tryCatch.getJoints()) {
                    for (Variable sourceVar : joint.getSourceVariables()) {
                        builder.addEdge(sourceVar.getIndex(), joint.getReceiver().getIndex());
                    }
                }
            }
        }
        return builder.build();
    }

    private static class InstructionAnalyzer implements InstructionVisitor {
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
        public void visit(EmptyInstruction insn) {
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
        }

        @Override
        public void visit(NullConstantInstruction insn) {
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
        }

        @Override
        public void visit(LongConstantInstruction insn) {
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
        }

        @Override
        public void visit(StringConstantInstruction insn) {
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
        public void visit(BranchingInstruction insn) {
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
        }

        @Override
        public void visit(JumpInstruction insn) {
        }

        @Override
        public void visit(SwitchInstruction insn) {
        }

        @Override
        public void visit(ExitInstruction insn) {
        }

        @Override
        public void visit(RaiseInstruction insn) {
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            use(insn.getReceiver(), insn.getSize());
        }

        @Override
        public void visit(ConstructInstruction insn) {
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
        public void visit(PutFieldInstruction insn) {
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
        public void visit(PutElementInstruction insn) {
        }

        @Override
        public void visit(InvokeInstruction insn) {
        }

        @Override
        public void visit(InvokeDynamicInstruction insn) {
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            use(insn.getReceiver(), insn.getValue());
        }

        @Override
        public void visit(InitClassInstruction insn) {
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            use(insn.getReceiver(), insn.getValue());
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {

        }

        @Override
        public void visit(MonitorExitInstruction insn) {

        }
    }
}
