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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import org.teavm.model.BasicBlock;
import org.teavm.model.FieldReference;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ReferenceCache;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.TextLocation;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BinaryOperation;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerDirection;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.IntegerSubtype;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class ProgramIO {
    private SymbolTable symbolTable;
    private SymbolTable fileTable;
    private ReferenceCache referenceCache = new ReferenceCache();
    private static BinaryOperation[] binaryOperations = BinaryOperation.values();
    private static NumericOperandType[] numericOperandTypes = NumericOperandType.values();
    private static IntegerSubtype[] integerSubtypes = IntegerSubtype.values();
    private static CastIntegerDirection[] castIntegerDirections = CastIntegerDirection.values();
    private static BranchingCondition[] branchingConditions = BranchingCondition.values();
    private static BinaryBranchingCondition[] binaryBranchingConditions = BinaryBranchingCondition.values();
    private static ArrayElementType[] arrayElementTypes = ArrayElementType.values();

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
            data.writeUTF(var.getDebugName() != null ? var.getDebugName() : "");
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock basicBlock = program.basicBlockAt(i);
            data.writeShort(basicBlock.getExceptionVariable() != null
                    ? basicBlock.getExceptionVariable().getIndex() : -1);
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
                data.writeShort(tryCatch.getHandler().getIndex());
            }
            TextLocation location = null;
            InstructionWriter insnWriter = new InstructionWriter(data);
            for (Instruction insn : basicBlock) {
                try {
                    if (!Objects.equals(location, insn.getLocation())) {
                        location = insn.getLocation();
                        if (location == null || location.getFileName() == null || location.getLine() < 0) {
                            data.writeByte(-2);
                        } else {
                            data.writeByte(-3);
                            data.writeShort(fileTable.lookup(location.getFileName()));
                            data.writeShort(location.getLine());
                        }
                    }
                    insn.acceptVisitor(insnWriter);
                } catch (IOExceptionWrapper e) {
                    throw (IOException) e.getCause();
                }
            }
            data.writeByte(-1);
        }
    }

    public Program read(InputStream input) throws IOException {
        DataInput data = new DataInputStream(input);
        Program program = new Program();
        int varCount = data.readShort();
        int basicBlockCount = data.readShort();
        for (int i = 0; i < varCount; ++i) {
            Variable var = program.createVariable();
            var.setRegister(data.readShort());
            var.setDebugName(referenceCache.getCached(data.readUTF()));
            if (var.getDebugName().isEmpty()) {
                var.setDebugName(null);
            }
        }
        for (int i = 0; i < basicBlockCount; ++i) {
            program.createBasicBlock();
        }
        for (int i = 0; i < basicBlockCount; ++i) {
            BasicBlock block = program.basicBlockAt(i);

            short varIndex = data.readShort();
            if (varIndex >= 0) {
                block.setExceptionVariable(program.variableAt(varIndex));
            }

            int phiCount = data.readShort();
            int tryCatchCount = data.readShort();
            for (int j = 0; j < phiCount; ++j) {
                Phi phi = new Phi();
                phi.setReceiver(program.variableAt(data.readShort()));
                int incomingCount = data.readShort();
                for (int k = 0; k < incomingCount; ++k) {
                    Incoming incoming = new Incoming();
                    incoming.setSource(program.basicBlockAt(data.readShort()));
                    incoming.setValue(program.variableAt(data.readShort()));
                    phi.getIncomings().add(incoming);
                }
                block.getPhis().add(phi);
            }
            for (int j = 0; j < tryCatchCount; ++j) {
                TryCatchBlock tryCatch = new TryCatchBlock();
                int typeIndex = data.readInt();
                if (typeIndex >= 0) {
                    tryCatch.setExceptionType(symbolTable.at(typeIndex));
                }
                tryCatch.setHandler(program.basicBlockAt(data.readShort()));

                block.getTryCatchBlocks().add(tryCatch);
            }

            TextLocation location = null;
            insnLoop: while (true) {
                byte insnType = data.readByte();
                switch (insnType) {
                    case -1:
                        break insnLoop;
                    case -2:
                        location = null;
                        break;
                    case -3: {
                        String file = fileTable.at(data.readShort());
                        short line = data.readShort();
                        location = new TextLocation(file, line);
                        break;
                    }
                    default: {
                        Instruction insn = readInstruction(insnType, program, data);
                        insn.setLocation(location);
                        block.add(insn);
                        break;
                    }
                }
            }
        }
        return program;
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
                output.writeUTF(insn.getConstant());
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
                output.writeInt(symbolTable.lookup(insn.getField().getClassName()));
                output.writeInt(symbolTable.lookup(insn.getField().getFieldName()));
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
                output.writeInt(symbolTable.lookup(insn.getField().getClassName()));
                output.writeInt(symbolTable.lookup(insn.getField().getFieldName()));
                output.writeInt(symbolTable.lookup(insn.getFieldType().toString()));
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
                output.writeByte(insn.getType().ordinal());
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
                output.writeByte(insn.getType().ordinal());
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
                        output.write(insn.getInstance() == null ? 33 : 34);
                        break;
                    case VIRTUAL:
                        output.write(35);
                        break;
                }
                output.writeShort(insn.getReceiver() != null ? insn.getReceiver().getIndex() : -1);
                if (insn.getInstance() != null) {
                    output.writeShort(insn.getInstance().getIndex());
                }
                output.writeInt(symbolTable.lookup(insn.getMethod().getClassName()));
                output.writeInt(symbolTable.lookup(insn.getMethod().getDescriptor().toString()));
                for (int i = 0; i < insn.getArguments().size(); ++i) {
                    output.writeShort(insn.getArguments().get(i).getIndex());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InvokeDynamicInstruction insn) {
            try {
                output.writeByte(41);
                output.writeShort(insn.getReceiver() != null ? insn.getReceiver().getIndex() : -1);
                output.writeShort(insn.getInstance() != null ? insn.getInstance().getIndex() : -1);
                output.writeInt(symbolTable.lookup(insn.getMethod().toString()));
                for (int i = 0; i < insn.getArguments().size(); ++i) {
                    output.writeShort(insn.getArguments().get(i).getIndex());
                }
                write(insn.getBootstrapMethod());
                output.writeByte(insn.getBootstrapArguments().size());
                for (int i = 0; i < insn.getBootstrapArguments().size(); ++i) {
                    write(insn.getBootstrapArguments().get(i));
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            try {
                output.writeByte(36);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeInt(symbolTable.lookup(insn.getType().toString()));
                output.writeShort(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InitClassInstruction insn) {
            try {
                output.writeByte(37);
                output.writeInt(symbolTable.lookup(insn.getClassName()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            try {
                output.writeByte(38);
                output.writeShort(insn.getReceiver().getIndex());
                output.writeShort(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
            try {
                output.writeByte(39);
                output.writeShort(insn.getObjectRef().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
            try {
                output.writeByte(40);
                output.writeShort(insn.getObjectRef().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        private void write(MethodHandle handle) throws IOException {
            switch (handle.getKind()) {
                case GET_FIELD:
                    output.writeByte(0);
                    break;
                case GET_STATIC_FIELD:
                    output.writeByte(1);
                    break;
                case PUT_FIELD:
                    output.writeByte(2);
                    break;
                case PUT_STATIC_FIELD:
                    output.writeByte(3);
                    break;
                case INVOKE_VIRTUAL:
                    output.writeByte(4);
                    break;
                case INVOKE_STATIC:
                    output.writeByte(5);
                    break;
                case INVOKE_SPECIAL:
                    output.writeByte(6);
                    break;
                case INVOKE_CONSTRUCTOR:
                    output.writeByte(7);
                    break;
                case INVOKE_INTERFACE:
                    output.writeByte(8);
                    break;
            }
            output.writeInt(symbolTable.lookup(handle.getClassName()));
            switch (handle.getKind()) {
                case GET_FIELD:
                case GET_STATIC_FIELD:
                case PUT_FIELD:
                case PUT_STATIC_FIELD:
                    output.writeInt(symbolTable.lookup(handle.getName()));
                    output.writeInt(symbolTable.lookup(handle.getValueType().toString()));
                    break;
                default:
                    output.writeInt(symbolTable.lookup(new MethodDescriptor(handle.getName(),
                            handle.signature()).toString()));
                    break;
            }
        }

        private void write(RuntimeConstant cst) throws IOException {
            switch (cst.getKind()) {
                case RuntimeConstant.INT:
                    output.writeByte(0);
                    output.writeInt(cst.getInt());
                    break;
                case RuntimeConstant.LONG:
                    output.writeByte(1);
                    output.writeLong(cst.getLong());
                    break;
                case RuntimeConstant.FLOAT:
                    output.writeByte(2);
                    output.writeFloat(cst.getFloat());
                    break;
                case RuntimeConstant.DOUBLE:
                    output.writeByte(3);
                    output.writeDouble(cst.getDouble());
                    break;
                case RuntimeConstant.STRING:
                    output.writeByte(4);
                    output.writeUTF(cst.getString());
                    break;
                case RuntimeConstant.TYPE:
                    output.writeByte(5);
                    output.writeInt(symbolTable.lookup(cst.getValueType().toString()));
                    break;
                case RuntimeConstant.METHOD:
                    output.writeByte(6);
                    output.writeInt(symbolTable.lookup(ValueType.methodTypeToString(cst.getMethodType())));
                    break;
                case RuntimeConstant.METHOD_HANDLE:
                    output.writeByte(7);
                    write(cst.getMethodHandle());
                    break;
            }
        }
    }

    private static class IOExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = -1765050162629001951L;
        public IOExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }

    private Instruction readInstruction(byte insnType, Program program, DataInput input) throws IOException {
        switch (insnType) {
            case 0:
                return new EmptyInstruction();
            case 1: {
                ClassConstantInstruction insn = new ClassConstantInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setConstant(parseValueType(symbolTable.at(input.readInt())));
                return insn;
            }
            case 2: {
                NullConstantInstruction insn = new NullConstantInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                return insn;
            }
            case 3: {
                IntegerConstantInstruction insn = new IntegerConstantInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setConstant(input.readInt());
                return insn;
            }
            case 4: {
                LongConstantInstruction insn = new LongConstantInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setConstant(input.readLong());
                return insn;
            }
            case 5: {
                FloatConstantInstruction insn = new FloatConstantInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setConstant(input.readFloat());
                return insn;
            }
            case 6: {
                DoubleConstantInstruction insn = new DoubleConstantInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setConstant(input.readDouble());
                return insn;
            }
            case 7: {
                StringConstantInstruction insn = new StringConstantInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setConstant(input.readUTF());
                return insn;
            }
            case 8: {
                Variable receiver = program.variableAt(input.readShort());
                BinaryOperation operation = binaryOperations[input.readByte()];
                NumericOperandType operandType = numericOperandTypes[input.readByte()];
                BinaryInstruction insn = new BinaryInstruction(operation, operandType);
                insn.setReceiver(receiver);
                insn.setFirstOperand(program.variableAt(input.readShort()));
                insn.setSecondOperand(program.variableAt(input.readShort()));
                return insn;
            }
            case 9: {
                Variable receiver = program.variableAt(input.readShort());
                NumericOperandType operandType = numericOperandTypes[input.readByte()];
                NegateInstruction insn = new NegateInstruction(operandType);
                insn.setReceiver(receiver);
                insn.setOperand(program.variableAt(input.readShort()));
                return insn;
            }
            case 10: {
                AssignInstruction insn = new AssignInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setAssignee(program.variableAt(input.readShort()));
                return insn;
            }
            case 11: {
                CastInstruction insn = new CastInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setTargetType(parseValueType(symbolTable.at(input.readInt())));
                insn.setValue(program.variableAt(input.readShort()));
                return insn;
            }
            case 12: {
                Variable receiver = program.variableAt(input.readShort());
                NumericOperandType sourceType = numericOperandTypes[input.readByte()];
                NumericOperandType targetType = numericOperandTypes[input.readByte()];
                CastNumberInstruction insn = new CastNumberInstruction(sourceType, targetType);
                insn.setReceiver(receiver);
                insn.setValue(program.variableAt(input.readShort()));
                return insn;
            }
            case 13: {
                Variable receiver = program.variableAt(input.readShort());
                IntegerSubtype targetType = integerSubtypes[input.readByte()];
                CastIntegerDirection direction = castIntegerDirections[input.readByte()];
                CastIntegerInstruction insn = new CastIntegerInstruction(targetType, direction);
                insn.setReceiver(receiver);
                insn.setValue(program.variableAt(input.readShort()));
                return insn;
            }
            case 14: {
                BranchingInstruction insn = new BranchingInstruction(branchingConditions[input.readByte()]);
                insn.setOperand(program.variableAt(input.readShort()));
                insn.setConsequent(program.basicBlockAt(input.readShort()));
                insn.setAlternative(program.basicBlockAt(input.readShort()));
                return insn;
            }
            case 15: {
                BinaryBranchingCondition cond = binaryBranchingConditions[input.readByte()];
                BinaryBranchingInstruction insn = new BinaryBranchingInstruction(cond);
                insn.setFirstOperand(program.variableAt(input.readShort()));
                insn.setSecondOperand(program.variableAt(input.readShort()));
                insn.setConsequent(program.basicBlockAt(input.readShort()));
                insn.setAlternative(program.basicBlockAt(input.readShort()));
                return insn;
            }
            case 16: {
                JumpInstruction insn = new JumpInstruction();
                insn.setTarget(program.basicBlockAt(input.readShort()));
                return insn;
            }
            case 17: {
                SwitchInstruction insn = new SwitchInstruction();
                insn.setCondition(program.variableAt(input.readShort()));
                insn.setDefaultTarget(program.basicBlockAt(input.readShort()));
                int entryCount = input.readShort();
                for (int i = 0; i < entryCount; ++i) {
                    SwitchTableEntry entry = new SwitchTableEntry();
                    entry.setCondition(input.readInt());
                    entry.setTarget(program.basicBlockAt(input.readShort()));
                    insn.getEntries().add(entry);
                }
                return insn;
            }
            case 18: {
                ExitInstruction insn = new ExitInstruction();
                insn.setValueToReturn(program.variableAt(input.readShort()));
                return insn;
            }
            case 19: {
                return new ExitInstruction();
            }
            case 20: {
                RaiseInstruction insn = new RaiseInstruction();
                insn.setException(program.variableAt(input.readShort()));
                return insn;
            }
            case 21: {
                ConstructArrayInstruction insn = new ConstructArrayInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setItemType(parseValueType(symbolTable.at(input.readInt())));
                insn.setSize(program.variableAt(input.readShort()));
                return insn;
            }
            case 22: {
                ConstructInstruction insn = new ConstructInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setType(symbolTable.at(input.readInt()));
                return insn;
            }
            case 23: {
                ConstructMultiArrayInstruction insn = new ConstructMultiArrayInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setItemType(parseValueType(symbolTable.at(input.readInt())));
                int dimensionCount = input.readByte();
                for (int i = 0; i < dimensionCount; ++i) {
                    insn.getDimensions().add(program.variableAt(input.readShort()));
                }
                return insn;
            }
            case 24: {
                GetFieldInstruction insn = new GetFieldInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setInstance(program.variableAt(input.readShort()));
                String className = symbolTable.at(input.readInt());
                String fieldName = symbolTable.at(input.readInt());
                insn.setField(new FieldReference(className, fieldName));
                insn.setFieldType(parseValueType(symbolTable.at(input.readInt())));
                return insn;
            }
            case 25: {
                GetFieldInstruction insn = new GetFieldInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                String className = symbolTable.at(input.readInt());
                String fieldName = symbolTable.at(input.readInt());
                insn.setField(new FieldReference(className, fieldName));
                insn.setFieldType(parseValueType(symbolTable.at(input.readInt())));
                return insn;
            }
            case 26: {
                PutFieldInstruction insn = new PutFieldInstruction();
                insn.setInstance(program.variableAt(input.readShort()));
                String className = symbolTable.at(input.readInt());
                String fieldName = symbolTable.at(input.readInt());
                ValueType type = parseValueType(symbolTable.at(input.readInt()));
                insn.setField(new FieldReference(className, fieldName));
                insn.setValue(program.variableAt(input.readShort()));
                insn.setFieldType(type);
                return insn;
            }
            case 27: {
                PutFieldInstruction insn = new PutFieldInstruction();
                String className = symbolTable.at(input.readInt());
                String fieldName = symbolTable.at(input.readInt());
                ValueType type = parseValueType(symbolTable.at(input.readInt()));
                insn.setField(new FieldReference(className, fieldName));
                insn.setValue(program.variableAt(input.readShort()));
                insn.setFieldType(type);
                return insn;
            }
            case 28: {
                ArrayLengthInstruction insn = new ArrayLengthInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setArray(program.variableAt(input.readShort()));
                return insn;
            }
            case 29: {
                CloneArrayInstruction insn = new CloneArrayInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setArray(program.variableAt(input.readShort()));
                return insn;
            }
            case 30: {
                Variable receiver = program.variableAt(input.readShort());
                UnwrapArrayInstruction insn = new UnwrapArrayInstruction(arrayElementTypes[input.readByte()]);
                insn.setReceiver(receiver);
                insn.setArray(program.variableAt(input.readShort()));
                return insn;
            }
            case 31: {
                GetElementInstruction insn = new GetElementInstruction(arrayElementTypes[input.readByte()]);
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setArray(program.variableAt(input.readShort()));
                insn.setIndex(program.variableAt(input.readShort()));
                return insn;
            }
            case 32: {
                PutElementInstruction insn = new PutElementInstruction(arrayElementTypes[input.readByte()]);
                insn.setArray(program.variableAt(input.readShort()));
                insn.setIndex(program.variableAt(input.readShort()));
                insn.setValue(program.variableAt(input.readShort()));
                return insn;
            }
            case 33: {
                InvokeInstruction insn = new InvokeInstruction();
                insn.setType(InvocationType.SPECIAL);
                int receiverIndex = input.readShort();
                insn.setReceiver(receiverIndex >= 0 ? program.variableAt(receiverIndex) : null);
                String className = symbolTable.at(input.readInt());
                MethodDescriptor methodDesc = parseMethodDescriptor(symbolTable.at(input.readInt()));
                insn.setMethod(createMethodReference(className, methodDesc));
                int paramCount = insn.getMethod().getDescriptor().parameterCount();
                Variable[] arguments = new Variable[paramCount];
                for (int i = 0; i < paramCount; ++i) {
                    arguments[i] = program.variableAt(input.readShort());
                }
                insn.setArguments(arguments);
                return insn;
            }
            case 34: {
                InvokeInstruction insn = new InvokeInstruction();
                insn.setType(InvocationType.SPECIAL);
                int receiverIndex = input.readShort();
                insn.setReceiver(receiverIndex >= 0 ? program.variableAt(receiverIndex) : null);
                insn.setInstance(program.variableAt(input.readShort()));
                String className = symbolTable.at(input.readInt());
                MethodDescriptor methodDesc = parseMethodDescriptor(symbolTable.at(input.readInt()));
                insn.setMethod(createMethodReference(className, methodDesc));
                int paramCount = insn.getMethod().getDescriptor().parameterCount();
                Variable[] arguments = new Variable[paramCount];
                for (int i = 0; i < paramCount; ++i) {
                    arguments[i] = program.variableAt(input.readShort());
                }
                insn.setArguments(arguments);
                return insn;
            }
            case 35: {
                InvokeInstruction insn = new InvokeInstruction();
                insn.setType(InvocationType.VIRTUAL);
                int receiverIndex = input.readShort();
                insn.setReceiver(receiverIndex >= 0 ? program.variableAt(receiverIndex) : null);
                insn.setInstance(program.variableAt(input.readShort()));
                String className = symbolTable.at(input.readInt());
                MethodDescriptor methodDesc = parseMethodDescriptor(symbolTable.at(input.readInt()));
                insn.setMethod(createMethodReference(className, methodDesc));
                int paramCount = insn.getMethod().getDescriptor().parameterCount();
                Variable[] arguments = new Variable[paramCount];
                for (int i = 0; i < paramCount; ++i) {
                    arguments[i] = program.variableAt(input.readShort());
                }
                insn.setArguments(arguments);
                return insn;
            }
            case 36: {
                IsInstanceInstruction insn = new IsInstanceInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setType(parseValueType(symbolTable.at(input.readInt())));
                insn.setValue(program.variableAt(input.readShort()));
                return insn;
            }
            case 37: {
                InitClassInstruction insn = new InitClassInstruction();
                insn.setClassName(symbolTable.at(input.readInt()));
                return insn;
            }
            case 38: {
                NullCheckInstruction insn = new NullCheckInstruction();
                insn.setReceiver(program.variableAt(input.readShort()));
                insn.setValue(program.variableAt(input.readShort()));
                return insn;
            }
            case 39: {
                MonitorEnterInstruction insn = new MonitorEnterInstruction();
                insn.setObjectRef(program.variableAt(input.readShort()));
                return insn;
            }
            case 40: {
                MonitorExitInstruction insn = new MonitorExitInstruction();
                insn.setObjectRef(program.variableAt(input.readShort()));
                return insn;
            }
            case 41: {
                InvokeDynamicInstruction insn = new InvokeDynamicInstruction();
                short receiver = input.readShort();
                short instance = input.readShort();
                insn.setReceiver(receiver >= 0 ? program.variableAt(receiver) : null);
                insn.setInstance(instance >= 0 ? program.variableAt(instance) : null);
                insn.setMethod(parseMethodDescriptor(symbolTable.at(input.readInt())));
                int argsCount = insn.getMethod().parameterCount();
                for (int i = 0; i < argsCount; ++i) {
                    insn.getArguments().add(program.variableAt(input.readShort()));
                }
                insn.setBootstrapMethod(readMethodHandle(input));
                int bootstrapArgsCount = input.readByte();
                for (int i = 0; i < bootstrapArgsCount; ++i) {
                    insn.getBootstrapArguments().add(readRuntimeConstant(input));
                }
                return insn;
            }
            default:
                throw new RuntimeException("Unknown instruction type: " + insnType);
        }
    }

    private MethodHandle readMethodHandle(DataInput input) throws IOException {
        byte kind = input.readByte();
        switch (kind) {
            case 0:
                return MethodHandle.fieldGetter(symbolTable.at(input.readInt()), symbolTable.at(input.readInt()),
                        parseValueType(symbolTable.at(input.readInt())));
            case 1:
                return MethodHandle.staticFieldGetter(symbolTable.at(input.readInt()), symbolTable.at(input.readInt()),
                        parseValueType(symbolTable.at(input.readInt())));
            case 2:
                return MethodHandle.fieldSetter(symbolTable.at(input.readInt()), symbolTable.at(input.readInt()),
                        parseValueType(symbolTable.at(input.readInt())));
            case 3:
                return MethodHandle.staticFieldSetter(symbolTable.at(input.readInt()), symbolTable.at(input.readInt()),
                        parseValueType(symbolTable.at(input.readInt())));
            case 4:
                return MethodHandle.virtualCaller(symbolTable.at(input.readInt()),
                        parseMethodDescriptor(symbolTable.at(input.readInt())));
            case 5:
                return MethodHandle.staticCaller(symbolTable.at(input.readInt()),
                        parseMethodDescriptor(symbolTable.at(input.readInt())));
            case 6:
                return MethodHandle.specialCaller(symbolTable.at(input.readInt()),
                        parseMethodDescriptor(symbolTable.at(input.readInt())));
            case 7:
                return MethodHandle.constructorCaller(symbolTable.at(input.readInt()),
                        parseMethodDescriptor(symbolTable.at(input.readInt())));
            case 8:
                return MethodHandle.interfaceCaller(symbolTable.at(input.readInt()),
                        parseMethodDescriptor(symbolTable.at(input.readInt())));
            default:
                throw new IllegalArgumentException("Unexpected method handle type: " + kind);
        }
    }

    private RuntimeConstant readRuntimeConstant(DataInput input) throws IOException {
        byte kind = input.readByte();
        switch (kind) {
            case 0:
                return new RuntimeConstant(input.readInt());
            case 1:
                return new RuntimeConstant(input.readLong());
            case 2:
                return new RuntimeConstant(input.readFloat());
            case 3:
                return new RuntimeConstant(input.readDouble());
            case 4:
                return new RuntimeConstant(input.readUTF());
            case 5:
                return new RuntimeConstant(parseValueType(symbolTable.at(input.readInt())));
            case 6:
                return new RuntimeConstant(MethodDescriptor.parseSignature(symbolTable.at(input.readInt())));
            case 7:
                return new RuntimeConstant(readMethodHandle(input));
            default:
                throw new IllegalArgumentException("Unexpected runtime constant type: " + kind);
        }
    }
    
    private MethodDescriptor parseMethodDescriptor(String key) {
        return referenceCache.parseDescriptorCached(key);
    }
    
    private ValueType parseValueType(String key) {
        return referenceCache.parseValueTypeCached(key);
    }
    
    private MethodReference createMethodReference(String className, MethodDescriptor method) {
        return referenceCache.getCached(className, method);
    }
}
