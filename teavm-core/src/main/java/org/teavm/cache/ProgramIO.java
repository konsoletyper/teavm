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
package org.teavm.cache;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public class ProgramIO {
    private SymbolTable symbolTable;

    public ProgramIO(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void write(Program program, OutputStream output) throws IOException {
        DataOutput data = new DataOutputStream(output);
        data.writeShort(program.variableCount());
        data.writeShort(program.basicBlockCount());
        for (int i = 0; i < program.variableCount(); ++i) {
            Variable var = program.variableAt(i);
            data.writeShort(var.getRegister());
            data.writeShort(var.getDebugNames().size());
            for (String debugString : var.getDebugNames()) {
                data.writeUTF(debugString);
            }
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock basicBlock = program.basicBlockAt(i);
            data.writeShort(basicBlock.getPhis().size());
            data.writeShort(basicBlock.getTryCatchBlocks().size());
            data.writeShort(basicBlock.getInstructions().size());
            for (Phi phi : basicBlock.getPhis()) {
                data.writeShort(phi.getReceiver().getIndex());
                data.writeShort(phi.getIncomings().size());
                for (Incoming incoming : phi.getIncomings()) {
                    data.writeShort(incoming.getSource().getIndex());
                    data.writeShort(incoming.getValue().getIndex());
                }
            }
            for (TryCatchBlock tryCatch : basicBlock.getTryCatchBlocks()) {
                data.writeInt(tryCatch.getExceptionType() != null ? symbolTable.findSymbol(
                        tryCatch.getExceptionType()) : -1);
                data.writeShort(tryCatch.getExceptionVariable() != null ?
                        tryCatch.getExceptionVariable().getIndex() : -1);
                data.writeShort(tryCatch.getProtectedBlock().getIndex());
                data.writeShort(tryCatch.getHandler().getIndex());
            }
            InstructionWriter insnWriter = new InstructionWriter(data);
            for (Instruction insn : basicBlock.getInstructions()) {
                try {
                    insn.acceptVisitor(insnWriter);
                } catch (IOExceptionWrapper e) {
                    throw (IOException)e.getCause();
                }
            }
        }
    }

    private class InstructionWriter implements InstructionVisitor {
        private DataOutput output;

        public InstructionWriter(DataOutput output) {
            this.output = output;
        }

        @Override
        public void visit(EmptyInstruction insn) {
            try {
                output.writeByte(0);
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            try {
                output.writeByte(1);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeInt(symbolTable.findSymbol(insn.getConstant().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            try {
                output.writeByte(2);
                output.writeShort(insn.getReceiver().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            try {
                output.writeByte(3);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeInt(insn.getConstant());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            try {
                output.writeByte(4);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeLong(insn.getConstant());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            try {
                output.writeByte(5);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeFloat(insn.getConstant());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            try {
                output.writeByte(6);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeDouble(insn.getConstant());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            try {
                output.writeByte(7);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeInt(symbolTable.findSymbol(insn.getConstant()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BinaryInstruction insn) {
            try {
                output.writeByte(8);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeByte(insn.getOperation().ordinal());
                output.writeByte(insn.getOperandType().ordinal());
                output.writeShort(insn.getFirstOperand().getIndex());
                output.writeShort(insn.getSecondOperand().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NegateInstruction insn) {
            try {
                output.writeByte(9);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeByte(insn.getOperandType().ordinal());
                output.writeShort(insn.getOperand().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(AssignInstruction insn) {
            try {
                output.writeByte(10);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeShort(insn.getAssignee().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(CastInstruction insn) {
            try {
                output.writeByte(11);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeInt(symbolTable.findSymbol(insn.getTargetType().toString()));
                output.writeShort(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(CastNumberInstruction insn) {
            try {
                output.writeByte(12);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeByte(insn.getSourceType().ordinal());
                output.writeByte(insn.getTargetType().ordinal());
                output.writeShort(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            try {
                output.writeByte(13);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeByte(insn.getTargetType().ordinal());
                output.writeByte(insn.getDirection().ordinal());
                output.writeShort(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BranchingInstruction insn) {
            try {
                output.writeByte(14);
                output.writeByte(insn.getCondition().ordinal());
                output.writeShort(insn.getOperand().getIndex());
                output.writeShort(insn.getConsequent().getIndex());
                output.writeShort(insn.getAlternative().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
            try {
                output.writeByte(15);
                output.writeByte(insn.getCondition().ordinal());
                output.writeShort(insn.getFirstOperand().getIndex());
                output.writeShort(insn.getSecondOperand().getIndex());
                output.writeShort(insn.getConsequent().getIndex());
                output.writeShort(insn.getAlternative().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(JumpInstruction insn) {
            try {
                output.writeByte(16);
                output.writeShort(insn.getTarget().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
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
        }

        @Override
        public void visit(ConstructInstruction insn) {
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
        }

        @Override
        public void visit(GetFieldInstruction insn) {
        }

        @Override
        public void visit(PutFieldInstruction insn) {
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
        }

        @Override
        public void visit(GetElementInstruction insn) {
        }

        @Override
        public void visit(PutElementInstruction insn) {
        }

        @Override
        public void visit(InvokeInstruction insn) {
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
        }

        @Override
        public void visit(InitClassInstruction insn) {
        }

        @Override
        public void visit(NullCheckInstruction insn) {
        }

    }

    private static class IOExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = -1765050162629001951L;
        public IOExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }
}
