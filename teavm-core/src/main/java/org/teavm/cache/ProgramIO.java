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
    private SymbolTable fileTable;

    public ProgramIO(SymbolTable symbolTable, SymbolTable fileTable) {
        this.symbolTable = symbolTable;
        this.fileTable = fileTable;
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
            for (Phi phi : basicBlock.getPhis()) {
                data.writeShort(phi.getReceiver().getIndex());
                data.writeShort(phi.getIncomings().size());
                for (Incoming incoming : phi.getIncomings()) {
                    data.writeShort(incoming.getSource().getIndex());
                    data.writeShort(incoming.getValue().getIndex());
                }
            }
            for (TryCatchBlock tryCatch : basicBlock.getTryCatchBlocks()) {
                data.writeInt(tryCatch.getExceptionType() != null ? symbolTable.lookup(
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
            data.writeByte(-1);
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
                output.writeInt(symbolTable.lookup(insn.getConstant().toString()));
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
                output.writeInt(symbolTable.lookup(insn.getConstant()));
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
                output.writeInt(symbolTable.lookup(insn.getTargetType().toString()));
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
            try {
                output.writeByte(17);
                output.writeShort(insn.getCondition().getIndex());
                output.writeShort(insn.getDefaultTarget().getIndex());
                output.writeShort(insn.getEntries().size());
                for (SwitchTableEntry entry : insn.getEntries()) {
                    output.writeInt(entry.getCondition());
                    output.writeShort(entry.getTarget().getIndex());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ExitInstruction insn) {
            try {
                if (insn.getValueToReturn() != null) {
                    output.writeByte(18);
                    output.writeShort(insn.getValueToReturn() != null ? insn.getValueToReturn().getIndex() : -1);
                } else {
                    output.writeByte(19);
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(RaiseInstruction insn) {
            try {
                output.writeByte(20);
                output.writeShort(insn.getException().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            try {
                output.writeByte(21);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeInt(symbolTable.lookup(insn.getItemType().toString()));
                output.writeShort(insn.getSize().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ConstructInstruction insn) {
            try {
                output.writeByte(22);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeInt(symbolTable.lookup(insn.getType()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            try {
                output.writeByte(23);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeInt(symbolTable.lookup(insn.getItemType().toString()));
                output.writeByte(insn.getDimensions().size());
                for (Variable dimension : insn.getDimensions()) {
                    output.writeShort(dimension.getIndex());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            try {
                output.writeByte(insn.getInstance() != null ? 24 : 25);
                output.writeShort(insn.getReceiver().getIndex());
                if (insn.getInstance() != null) {
                    output.writeShort(insn.getInstance().getIndex());
                }
                output.writeInt(symbolTable.lookup(insn.getField().toString()));
                output.writeInt(symbolTable.lookup(insn.getFieldType().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            try {
                output.writeByte(insn.getInstance() != null ? 26 : 27);
                if (insn.getInstance() != null) {
                    output.writeShort(insn.getInstance().getIndex());
                }
                output.writeInt(symbolTable.lookup(insn.getField().toString()));
                output.writeShort(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
            try {
                output.writeByte(28);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeShort(insn.getArray().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            try {
                output.writeByte(29);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeShort(insn.getArray().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            try {
                output.writeByte(30);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeByte(insn.getElementType().ordinal());
                output.writeShort(insn.getArray().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(GetElementInstruction insn) {
            try {
                output.writeByte(31);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeShort(insn.getArray().getIndex());
                output.writeShort(insn.getIndex().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(PutElementInstruction insn) {
            try {
                output.writeByte(32);
                output.writeShort(insn.getArray().getIndex());
                output.writeShort(insn.getIndex().getIndex());
                output.writeShort(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InvokeInstruction insn) {
            try {
                switch (insn.getType()) {
                    case SPECIAL:
                        output.write(insn.getInstance() == null ? 32 : 33);
                        break;
                    case VIRTUAL:
                        output.write(34);
                        break;
                }
                output.writeShort(insn.getReceiver() != null ? insn.getReceiver().getIndex() : -1);
                if (insn.getInstance() != null) {
                    output.writeShort(insn.getInstance().getIndex());
                }
                output.writeInt(symbolTable.lookup(insn.getMethod().toString()));
                for (int i = 0; i < insn.getArguments().size(); ++i) {
                    output.writeShort(insn.getArguments().get(i).getIndex());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            try {
                output.writeByte(35);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeShort(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InitClassInstruction insn) {
            try {
                output.writeByte(36);
                output.writeInt(symbolTable.lookup(insn.getClassName()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            try {
                output.writeByte(37);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeShort(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }
    }

    private static class IOExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = -1765050162629001951L;
        public IOExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }
}
