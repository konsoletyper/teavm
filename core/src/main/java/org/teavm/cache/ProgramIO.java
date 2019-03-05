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
    private ReferenceCache referenceCache;
    private static BinaryOperation[] binaryOperations = BinaryOperation.values();
    private static NumericOperandType[] numericOperandTypes = NumericOperandType.values();
    private static IntegerSubtype[] integerSubtypes = IntegerSubtype.values();
    private static CastIntegerDirection[] castIntegerDirections = CastIntegerDirection.values();
    private static BranchingCondition[] branchingConditions = BranchingCondition.values();
    private static BinaryBranchingCondition[] binaryBranchingConditions = BinaryBranchingCondition.values();
    private static ArrayElementType[] arrayElementTypes = ArrayElementType.values();

    public ProgramIO(ReferenceCache referenceCache, SymbolTable symbolTable, SymbolTable fileTable) {
        this.referenceCache = referenceCache;
        this.symbolTable = symbolTable;
        this.fileTable = fileTable;
    }

    public void write(Program program, OutputStream output) throws IOException {
        VarDataOutput data = new VarDataOutput(output);
        data.writeUnsigned(program.variableCount());
        data.writeUnsigned(program.basicBlockCount());
        for (int i = 0; i < program.variableCount(); ++i) {
            Variable var = program.variableAt(i);
            data.writeUnsigned(var.getRegister());
            data.write(var.getDebugName());
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock basicBlock = program.basicBlockAt(i);
            data.writeUnsigned(basicBlock.getExceptionVariable() != null
                    ? basicBlock.getExceptionVariable().getIndex() + 1 : 0);
            data.writeUnsigned(basicBlock.getPhis().size());
            data.writeUnsigned(basicBlock.getTryCatchBlocks().size());
            for (Phi phi : basicBlock.getPhis()) {
                data.writeUnsigned(phi.getReceiver().getIndex());
                data.writeUnsigned(phi.getIncomings().size());
                for (Incoming incoming : phi.getIncomings()) {
                    data.writeUnsigned(incoming.getSource().getIndex());
                    data.writeUnsigned(incoming.getValue().getIndex());
                }
            }
            for (TryCatchBlock tryCatch : basicBlock.getTryCatchBlocks()) {
                data.writeUnsigned(tryCatch.getExceptionType() != null ? symbolTable.lookup(
                        tryCatch.getExceptionType()) + 1 : 0);
                data.writeUnsigned(tryCatch.getHandler().getIndex());
            }
            TextLocation location = null;
            InstructionWriter insnWriter = new InstructionWriter(data);
            for (Instruction insn : basicBlock) {
                try {
                    if (!Objects.equals(location, insn.getLocation())) {
                        location = insn.getLocation();
                        if (location == null || location.getFileName() == null || location.getLine() < 0) {
                            data.writeUnsigned(1);
                        } else {
                            data.writeUnsigned(2);
                            data.writeUnsigned(fileTable.lookup(location.getFileName()));
                            data.writeUnsigned(location.getLine());
                        }
                    }
                    insn.acceptVisitor(insnWriter);
                } catch (IOExceptionWrapper e) {
                    throw (IOException) e.getCause();
                }
            }
            data.writeUnsigned(0);
        }
    }

    public Program read(InputStream input) throws IOException {
        VarDataInput data = new VarDataInput(input);
        Program program = new Program();
        int varCount = data.readUnsigned();
        int basicBlockCount = data.readUnsigned();
        for (int i = 0; i < varCount; ++i) {
            Variable var = program.createVariable();
            var.setRegister(data.readUnsigned());
            var.setDebugName(referenceCache.getCached(data.read()));
        }
        for (int i = 0; i < basicBlockCount; ++i) {
            program.createBasicBlock();
        }
        for (int i = 0; i < basicBlockCount; ++i) {
            BasicBlock block = program.basicBlockAt(i);

            int varIndex = data.readUnsigned();
            if (varIndex > 0) {
                block.setExceptionVariable(program.variableAt(varIndex - 1));
            }

            int phiCount = data.readUnsigned();
            int tryCatchCount = data.readUnsigned();
            for (int j = 0; j < phiCount; ++j) {
                Phi phi = new Phi();
                phi.setReceiver(program.variableAt(data.readUnsigned()));
                int incomingCount = data.readUnsigned();
                for (int k = 0; k < incomingCount; ++k) {
                    Incoming incoming = new Incoming();
                    incoming.setSource(program.basicBlockAt(data.readUnsigned()));
                    incoming.setValue(program.variableAt(data.readUnsigned()));
                    phi.getIncomings().add(incoming);
                }
                block.getPhis().add(phi);
            }
            for (int j = 0; j < tryCatchCount; ++j) {
                TryCatchBlock tryCatch = new TryCatchBlock();
                int typeIndex = data.readUnsigned();
                if (typeIndex >= 0) {
                    tryCatch.setExceptionType(symbolTable.at(typeIndex));
                }
                tryCatch.setHandler(program.basicBlockAt(data.readUnsigned()));

                block.getTryCatchBlocks().add(tryCatch);
            }

            TextLocation location = null;
            insnLoop: while (true) {
                int insnType = data.readUnsigned();
                switch (insnType) {
                    case 0:
                        break insnLoop;
                    case 1:
                        location = null;
                        break;
                    case 2: {
                        String file = fileTable.at(data.readUnsigned());
                        int line = data.readUnsigned();
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
        private VarDataOutput output;

        InstructionWriter(VarDataOutput output) {
            this.output = output;
        }

        @Override
        public void visit(EmptyInstruction insn) {
            try {
                output.writeUnsigned(3);
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            try {
                output.writeUnsigned(4);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(symbolTable.lookup(insn.getConstant().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            try {
                output.writeUnsigned(5);
                output.writeUnsigned(insn.getReceiver().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            try {
                output.writeUnsigned(6);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeSigned(insn.getConstant());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            try {
                output.writeUnsigned(7);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeSigned(insn.getConstant());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            try {
                output.writeUnsigned(8);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeFloat(insn.getConstant());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            try {
                output.writeUnsigned(9);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeDouble(insn.getConstant());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            try {
                output.writeUnsigned(10);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.write(insn.getConstant());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BinaryInstruction insn) {
            try {
                output.writeUnsigned(11 + insn.getOperation().ordinal());
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(insn.getOperandType().ordinal());
                output.writeUnsigned(insn.getFirstOperand().getIndex());
                output.writeUnsigned(insn.getSecondOperand().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NegateInstruction insn) {
            try {
                output.writeUnsigned(23);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(insn.getOperandType().ordinal());
                output.writeUnsigned(insn.getOperand().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(AssignInstruction insn) {
            try {
                output.writeUnsigned(24);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(insn.getAssignee().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(CastInstruction insn) {
            try {
                output.writeUnsigned(25);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(symbolTable.lookup(insn.getTargetType().toString()));
                output.writeUnsigned(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(CastNumberInstruction insn) {
            try {
                output.writeUnsigned(26);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(insn.getSourceType().ordinal() | (insn.getTargetType().ordinal() << 2));
                output.writeUnsigned(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            try {
                output.writeUnsigned(27);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(insn.getDirection().ordinal() | (insn.getTargetType().ordinal() << 1));
                output.writeUnsigned(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BranchingInstruction insn) {
            try {
                output.writeUnsigned(28 + insn.getCondition().ordinal());
                output.writeUnsigned(insn.getOperand().getIndex());
                output.writeUnsigned(insn.getConsequent().getIndex());
                output.writeUnsigned(insn.getAlternative().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
            try {
                output.writeUnsigned(36 + insn.getCondition().ordinal());
                output.writeUnsigned(insn.getFirstOperand().getIndex());
                output.writeUnsigned(insn.getSecondOperand().getIndex());
                output.writeUnsigned(insn.getConsequent().getIndex());
                output.writeUnsigned(insn.getAlternative().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(JumpInstruction insn) {
            try {
                output.writeUnsigned(40);
                output.writeUnsigned(insn.getTarget().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(SwitchInstruction insn) {
            try {
                output.writeUnsigned(41);
                output.writeUnsigned(insn.getCondition().getIndex());
                output.writeUnsigned(insn.getDefaultTarget().getIndex());
                output.writeUnsigned(insn.getEntries().size());
                for (SwitchTableEntry entry : insn.getEntries()) {
                    output.writeSigned(entry.getCondition());
                    output.writeUnsigned(entry.getTarget().getIndex());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ExitInstruction insn) {
            try {
                if (insn.getValueToReturn() != null) {
                    output.writeUnsigned(42);
                    output.writeUnsigned(insn.getValueToReturn().getIndex());
                } else {
                    output.writeUnsigned(43);
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(RaiseInstruction insn) {
            try {
                output.writeUnsigned(44);
                output.writeUnsigned(insn.getException().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            try {
                output.writeUnsigned(45);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(symbolTable.lookup(insn.getItemType().toString()));
                output.writeUnsigned(insn.getSize().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ConstructInstruction insn) {
            try {
                output.writeUnsigned(46);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(symbolTable.lookup(insn.getType()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            try {
                output.writeUnsigned(47);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(symbolTable.lookup(insn.getItemType().toString()));
                output.writeUnsigned(insn.getDimensions().size());
                for (Variable dimension : insn.getDimensions()) {
                    output.writeUnsigned(dimension.getIndex());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            try {
                output.writeUnsigned(insn.getInstance() != null ? 48 : 49);
                output.writeUnsigned(insn.getReceiver().getIndex());
                if (insn.getInstance() != null) {
                    output.writeUnsigned(insn.getInstance().getIndex());
                }
                output.writeUnsigned(symbolTable.lookup(insn.getField().getClassName()));
                output.writeUnsigned(symbolTable.lookup(insn.getField().getFieldName()));
                output.writeUnsigned(symbolTable.lookup(insn.getFieldType().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            try {
                output.writeUnsigned(insn.getInstance() != null ? 50 : 51);
                if (insn.getInstance() != null) {
                    output.writeUnsigned(insn.getInstance().getIndex());
                }
                output.writeUnsigned(symbolTable.lookup(insn.getField().getClassName()));
                output.writeUnsigned(symbolTable.lookup(insn.getField().getFieldName()));
                output.writeUnsigned(symbolTable.lookup(insn.getFieldType().toString()));
                output.writeUnsigned(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
            try {
                output.writeUnsigned(52);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(insn.getArray().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            try {
                output.writeUnsigned(53);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(insn.getArray().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            try {
                output.writeUnsigned(54 + insn.getElementType().ordinal());
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(insn.getArray().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(GetElementInstruction insn) {
            try {
                output.writeUnsigned(62 + insn.getType().ordinal());
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(insn.getArray().getIndex());
                output.writeUnsigned(insn.getIndex().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(PutElementInstruction insn) {
            try {
                output.writeUnsigned(70 + insn.getType().ordinal());
                output.writeUnsigned(insn.getArray().getIndex());
                output.writeUnsigned(insn.getIndex().getIndex());
                output.writeUnsigned(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InvokeInstruction insn) {
            try {
                switch (insn.getType()) {
                    case SPECIAL:
                        output.writeUnsigned(insn.getInstance() == null ? 78 : 79);
                        break;
                    case VIRTUAL:
                        output.writeUnsigned(80);
                        break;
                }
                output.writeUnsigned(insn.getReceiver() != null ? insn.getReceiver().getIndex() + 1 : 0);
                if (insn.getInstance() != null) {
                    output.writeUnsigned(insn.getInstance().getIndex());
                }
                output.writeUnsigned(symbolTable.lookup(insn.getMethod().getClassName()));
                output.writeUnsigned(symbolTable.lookup(insn.getMethod().getDescriptor().toString()));
                for (int i = 0; i < insn.getArguments().size(); ++i) {
                    output.writeUnsigned(insn.getArguments().get(i).getIndex());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InvokeDynamicInstruction insn) {
            try {
                output.writeUnsigned(81);
                output.writeUnsigned(insn.getReceiver() != null ? insn.getReceiver().getIndex() : -1);
                output.writeUnsigned(insn.getInstance() != null ? insn.getInstance().getIndex() : -1);
                output.writeUnsigned(symbolTable.lookup(insn.getMethod().toString()));
                for (int i = 0; i < insn.getArguments().size(); ++i) {
                    output.writeUnsigned(insn.getArguments().get(i).getIndex());
                }
                write(insn.getBootstrapMethod());
                output.writeUnsigned(insn.getBootstrapArguments().size());
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
                output.writeUnsigned(82);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(symbolTable.lookup(insn.getType().toString()));
                output.writeUnsigned(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InitClassInstruction insn) {
            try {
                output.writeUnsigned(83);
                output.writeUnsigned(symbolTable.lookup(insn.getClassName()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            try {
                output.writeUnsigned(84);
                output.writeUnsigned(insn.getReceiver().getIndex());
                output.writeUnsigned(insn.getValue().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
            try {
                output.writeUnsigned(85);
                output.writeUnsigned(insn.getObjectRef().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
            try {
                output.writeUnsigned(86);
                output.writeUnsigned(insn.getObjectRef().getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        private void write(MethodHandle handle) throws IOException {
            switch (handle.getKind()) {
                case GET_FIELD:
                    output.writeUnsigned(0);
                    break;
                case GET_STATIC_FIELD:
                    output.writeUnsigned(1);
                    break;
                case PUT_FIELD:
                    output.writeUnsigned(2);
                    break;
                case PUT_STATIC_FIELD:
                    output.writeUnsigned(3);
                    break;
                case INVOKE_VIRTUAL:
                    output.writeUnsigned(4);
                    break;
                case INVOKE_STATIC:
                    output.writeUnsigned(5);
                    break;
                case INVOKE_SPECIAL:
                    output.writeUnsigned(6);
                    break;
                case INVOKE_CONSTRUCTOR:
                    output.writeUnsigned(7);
                    break;
                case INVOKE_INTERFACE:
                    output.writeUnsigned(8);
                    break;
            }
            output.writeUnsigned(symbolTable.lookup(handle.getClassName()));
            switch (handle.getKind()) {
                case GET_FIELD:
                case GET_STATIC_FIELD:
                case PUT_FIELD:
                case PUT_STATIC_FIELD:
                    output.writeUnsigned(symbolTable.lookup(handle.getName()));
                    output.writeUnsigned(symbolTable.lookup(handle.getValueType().toString()));
                    break;
                default:
                    output.writeUnsigned(symbolTable.lookup(new MethodDescriptor(handle.getName(),
                            handle.signature()).toString()));
                    break;
            }
        }

        private void write(RuntimeConstant cst) throws IOException {
            switch (cst.getKind()) {
                case RuntimeConstant.INT:
                    output.writeUnsigned(0);
                    output.writeSigned(cst.getInt());
                    break;
                case RuntimeConstant.LONG:
                    output.writeUnsigned(1);
                    output.writeSigned(cst.getLong());
                    break;
                case RuntimeConstant.FLOAT:
                    output.writeUnsigned(2);
                    output.writeFloat(cst.getFloat());
                    break;
                case RuntimeConstant.DOUBLE:
                    output.writeUnsigned(3);
                    output.writeDouble(cst.getDouble());
                    break;
                case RuntimeConstant.STRING:
                    output.writeUnsigned(4);
                    output.write(cst.getString());
                    break;
                case RuntimeConstant.TYPE:
                    output.writeUnsigned(5);
                    output.writeUnsigned(symbolTable.lookup(cst.getValueType().toString()));
                    break;
                case RuntimeConstant.METHOD:
                    output.writeUnsigned(6);
                    output.writeUnsigned(symbolTable.lookup(ValueType.methodTypeToString(cst.getMethodType())));
                    break;
                case RuntimeConstant.METHOD_HANDLE:
                    output.writeUnsigned(7);
                    write(cst.getMethodHandle());
                    break;
            }
        }
    }

    static class IOExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = -1765050162629001951L;
        IOExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }

    private Instruction readInstruction(int insnType, Program program, VarDataInput input) throws IOException {
        switch (insnType) {
            case 3:
                return new EmptyInstruction();
            case 4: {
                ClassConstantInstruction insn = new ClassConstantInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setConstant(parseValueType(symbolTable.at(input.readUnsigned())));
                return insn;
            }
            case 5: {
                NullConstantInstruction insn = new NullConstantInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 6: {
                IntegerConstantInstruction insn = new IntegerConstantInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setConstant(input.readSigned());
                return insn;
            }
            case 7: {
                LongConstantInstruction insn = new LongConstantInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setConstant(input.readSignedLong());
                return insn;
            }
            case 8: {
                FloatConstantInstruction insn = new FloatConstantInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setConstant(input.readFloat());
                return insn;
            }
            case 9: {
                DoubleConstantInstruction insn = new DoubleConstantInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setConstant(input.readDouble());
                return insn;
            }
            case 10: {
                StringConstantInstruction insn = new StringConstantInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setConstant(input.read());
                return insn;
            }
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22: {
                Variable receiver = program.variableAt(input.readUnsigned());
                BinaryOperation operation = binaryOperations[insnType - 11];
                NumericOperandType operandType = numericOperandTypes[input.readUnsigned()];
                BinaryInstruction insn = new BinaryInstruction(operation, operandType);
                insn.setReceiver(receiver);
                insn.setFirstOperand(program.variableAt(input.readUnsigned()));
                insn.setSecondOperand(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 23: {
                Variable receiver = program.variableAt(input.readUnsigned());
                NumericOperandType operandType = numericOperandTypes[input.readUnsigned()];
                NegateInstruction insn = new NegateInstruction(operandType);
                insn.setReceiver(receiver);
                insn.setOperand(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 24: {
                AssignInstruction insn = new AssignInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setAssignee(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 25: {
                CastInstruction insn = new CastInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setTargetType(parseValueType(symbolTable.at(input.readUnsigned())));
                insn.setValue(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 26: {
                Variable receiver = program.variableAt(input.readUnsigned());
                int types = input.readUnsigned();
                NumericOperandType sourceType = numericOperandTypes[types & 3];
                NumericOperandType targetType = numericOperandTypes[types >> 2];
                CastNumberInstruction insn = new CastNumberInstruction(sourceType, targetType);
                insn.setReceiver(receiver);
                insn.setValue(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 27: {
                Variable receiver = program.variableAt(input.readUnsigned());
                int types = input.readUnsigned();
                CastIntegerDirection direction = castIntegerDirections[types & 1];
                IntegerSubtype targetType = integerSubtypes[types >> 1];
                CastIntegerInstruction insn = new CastIntegerInstruction(targetType, direction);
                insn.setReceiver(receiver);
                insn.setValue(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35: {
                BranchingInstruction insn = new BranchingInstruction(branchingConditions[insnType - 28]);
                insn.setOperand(program.variableAt(input.readUnsigned()));
                insn.setConsequent(program.basicBlockAt(input.readUnsigned()));
                insn.setAlternative(program.basicBlockAt(input.readUnsigned()));
                return insn;
            }
            case 36:
            case 37:
            case 38:
            case 39: {
                BinaryBranchingCondition cond = binaryBranchingConditions[insnType - 36];
                BinaryBranchingInstruction insn = new BinaryBranchingInstruction(cond);
                insn.setFirstOperand(program.variableAt(input.readUnsigned()));
                insn.setSecondOperand(program.variableAt(input.readUnsigned()));
                insn.setConsequent(program.basicBlockAt(input.readUnsigned()));
                insn.setAlternative(program.basicBlockAt(input.readUnsigned()));
                return insn;
            }
            case 40: {
                JumpInstruction insn = new JumpInstruction();
                insn.setTarget(program.basicBlockAt(input.readUnsigned()));
                return insn;
            }
            case 41: {
                SwitchInstruction insn = new SwitchInstruction();
                insn.setCondition(program.variableAt(input.readUnsigned()));
                insn.setDefaultTarget(program.basicBlockAt(input.readUnsigned()));
                int entryCount = input.readUnsigned();
                for (int i = 0; i < entryCount; ++i) {
                    SwitchTableEntry entry = new SwitchTableEntry();
                    entry.setCondition(input.readSigned());
                    entry.setTarget(program.basicBlockAt(input.readUnsigned()));
                    insn.getEntries().add(entry);
                }
                return insn;
            }
            case 42: {
                ExitInstruction insn = new ExitInstruction();
                insn.setValueToReturn(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 43: {
                return new ExitInstruction();
            }
            case 44: {
                RaiseInstruction insn = new RaiseInstruction();
                insn.setException(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 45: {
                ConstructArrayInstruction insn = new ConstructArrayInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setItemType(parseValueType(symbolTable.at(input.readUnsigned())));
                insn.setSize(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 46: {
                ConstructInstruction insn = new ConstructInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setType(symbolTable.at(input.readUnsigned()));
                return insn;
            }
            case 47: {
                ConstructMultiArrayInstruction insn = new ConstructMultiArrayInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setItemType(parseValueType(symbolTable.at(input.readUnsigned())));
                int dimensionCount = input.readUnsigned();
                for (int i = 0; i < dimensionCount; ++i) {
                    insn.getDimensions().add(program.variableAt(input.readUnsigned()));
                }
                return insn;
            }
            case 48: {
                GetFieldInstruction insn = new GetFieldInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setInstance(program.variableAt(input.readUnsigned()));
                String className = symbolTable.at(input.readUnsigned());
                String fieldName = symbolTable.at(input.readUnsigned());
                insn.setField(new FieldReference(className, fieldName));
                insn.setFieldType(parseValueType(symbolTable.at(input.readUnsigned())));
                return insn;
            }
            case 49: {
                GetFieldInstruction insn = new GetFieldInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                String className = symbolTable.at(input.readUnsigned());
                String fieldName = symbolTable.at(input.readUnsigned());
                insn.setField(new FieldReference(className, fieldName));
                insn.setFieldType(parseValueType(symbolTable.at(input.readUnsigned())));
                return insn;
            }
            case 50: {
                PutFieldInstruction insn = new PutFieldInstruction();
                insn.setInstance(program.variableAt(input.readUnsigned()));
                String className = symbolTable.at(input.readUnsigned());
                String fieldName = symbolTable.at(input.readUnsigned());
                ValueType type = parseValueType(symbolTable.at(input.readUnsigned()));
                insn.setField(new FieldReference(className, fieldName));
                insn.setValue(program.variableAt(input.readUnsigned()));
                insn.setFieldType(type);
                return insn;
            }
            case 51: {
                PutFieldInstruction insn = new PutFieldInstruction();
                String className = symbolTable.at(input.readUnsigned());
                String fieldName = symbolTable.at(input.readUnsigned());
                ValueType type = parseValueType(symbolTable.at(input.readUnsigned()));
                insn.setField(new FieldReference(className, fieldName));
                insn.setValue(program.variableAt(input.readUnsigned()));
                insn.setFieldType(type);
                return insn;
            }
            case 52: {
                ArrayLengthInstruction insn = new ArrayLengthInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setArray(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 53: {
                CloneArrayInstruction insn = new CloneArrayInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setArray(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61: {
                UnwrapArrayInstruction insn = new UnwrapArrayInstruction(arrayElementTypes[insnType - 54]);
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setArray(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 62:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69: {
                GetElementInstruction insn = new GetElementInstruction(arrayElementTypes[insnType - 62]);
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setArray(program.variableAt(input.readUnsigned()));
                insn.setIndex(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77: {
                PutElementInstruction insn = new PutElementInstruction(arrayElementTypes[insnType - 70]);
                insn.setArray(program.variableAt(input.readUnsigned()));
                insn.setIndex(program.variableAt(input.readUnsigned()));
                insn.setValue(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 78: {
                InvokeInstruction insn = new InvokeInstruction();
                insn.setType(InvocationType.SPECIAL);
                int receiverIndex = input.readUnsigned();
                insn.setReceiver(receiverIndex > 0 ? program.variableAt(receiverIndex - 1) : null);
                String className = symbolTable.at(input.readUnsigned());
                MethodDescriptor methodDesc = parseMethodDescriptor(symbolTable.at(input.readUnsigned()));
                insn.setMethod(createMethodReference(className, methodDesc));
                int paramCount = insn.getMethod().getDescriptor().parameterCount();
                Variable[] arguments = new Variable[paramCount];
                for (int i = 0; i < paramCount; ++i) {
                    arguments[i] = program.variableAt(input.readUnsigned());
                }
                insn.setArguments(arguments);
                return insn;
            }
            case 79: {
                InvokeInstruction insn = new InvokeInstruction();
                insn.setType(InvocationType.SPECIAL);
                int receiverIndex = input.readUnsigned();
                insn.setReceiver(receiverIndex > 0 ? program.variableAt(receiverIndex - 1) : null);
                insn.setInstance(program.variableAt(input.readUnsigned()));
                String className = symbolTable.at(input.readUnsigned());
                MethodDescriptor methodDesc = parseMethodDescriptor(symbolTable.at(input.readUnsigned()));
                insn.setMethod(createMethodReference(className, methodDesc));
                int paramCount = insn.getMethod().getDescriptor().parameterCount();
                Variable[] arguments = new Variable[paramCount];
                for (int i = 0; i < paramCount; ++i) {
                    arguments[i] = program.variableAt(input.readUnsigned());
                }
                insn.setArguments(arguments);
                return insn;
            }
            case 80: {
                InvokeInstruction insn = new InvokeInstruction();
                insn.setType(InvocationType.VIRTUAL);
                int receiverIndex = input.readUnsigned();
                insn.setReceiver(receiverIndex > 0 ? program.variableAt(receiverIndex - 1) : null);
                insn.setInstance(program.variableAt(input.readUnsigned()));
                String className = symbolTable.at(input.readUnsigned());
                MethodDescriptor methodDesc = parseMethodDescriptor(symbolTable.at(input.readUnsigned()));
                insn.setMethod(createMethodReference(className, methodDesc));
                int paramCount = insn.getMethod().getDescriptor().parameterCount();
                Variable[] arguments = new Variable[paramCount];
                for (int i = 0; i < paramCount; ++i) {
                    arguments[i] = program.variableAt(input.readUnsigned());
                }
                insn.setArguments(arguments);
                return insn;
            }
            case 82: {
                IsInstanceInstruction insn = new IsInstanceInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setType(parseValueType(symbolTable.at(input.readUnsigned())));
                insn.setValue(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 83: {
                InitClassInstruction insn = new InitClassInstruction();
                insn.setClassName(symbolTable.at(input.readUnsigned()));
                return insn;
            }
            case 84: {
                NullCheckInstruction insn = new NullCheckInstruction();
                insn.setReceiver(program.variableAt(input.readUnsigned()));
                insn.setValue(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 85: {
                MonitorEnterInstruction insn = new MonitorEnterInstruction();
                insn.setObjectRef(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 86: {
                MonitorExitInstruction insn = new MonitorExitInstruction();
                insn.setObjectRef(program.variableAt(input.readUnsigned()));
                return insn;
            }
            case 81: {
                InvokeDynamicInstruction insn = new InvokeDynamicInstruction();
                int receiver = input.readUnsigned();
                int instance = input.readUnsigned();
                insn.setReceiver(receiver > 0 ? program.variableAt(receiver - 1) : null);
                insn.setInstance(instance > 0 ? program.variableAt(instance - 1) : null);
                insn.setMethod(parseMethodDescriptor(symbolTable.at(input.readUnsigned())));
                int argsCount = insn.getMethod().parameterCount();
                for (int i = 0; i < argsCount; ++i) {
                    insn.getArguments().add(program.variableAt(input.readUnsigned()));
                }
                insn.setBootstrapMethod(readMethodHandle(input));
                int bootstrapArgsCount = input.readUnsigned();
                for (int i = 0; i < bootstrapArgsCount; ++i) {
                    insn.getBootstrapArguments().add(readRuntimeConstant(input));
                }
                return insn;
            }
            default:
                throw new RuntimeException("Unknown instruction type: " + insnType);
        }
    }

    private MethodHandle readMethodHandle(VarDataInput input) throws IOException {
        int kind = input.readUnsigned();
        switch (kind) {
            case 0:
                return MethodHandle.fieldGetter(symbolTable.at(input.readUnsigned()),
                        symbolTable.at(input.readUnsigned()),
                        parseValueType(symbolTable.at(input.readUnsigned())));
            case 1:
                return MethodHandle.staticFieldGetter(symbolTable.at(input.readUnsigned()),
                        symbolTable.at(input.readUnsigned()),
                        parseValueType(symbolTable.at(input.readUnsigned())));
            case 2:
                return MethodHandle.fieldSetter(symbolTable.at(input.readUnsigned()),
                        symbolTable.at(input.readUnsigned()),
                        parseValueType(symbolTable.at(input.readUnsigned())));
            case 3:
                return MethodHandle.staticFieldSetter(symbolTable.at(input.readUnsigned()),
                        symbolTable.at(input.readUnsigned()),
                        parseValueType(symbolTable.at(input.readUnsigned())));
            case 4:
                return MethodHandle.virtualCaller(symbolTable.at(input.readUnsigned()),
                        parseMethodDescriptor(symbolTable.at(input.readUnsigned())));
            case 5:
                return MethodHandle.staticCaller(symbolTable.at(input.readUnsigned()),
                        parseMethodDescriptor(symbolTable.at(input.readUnsigned())));
            case 6:
                return MethodHandle.specialCaller(symbolTable.at(input.readUnsigned()),
                        parseMethodDescriptor(symbolTable.at(input.readUnsigned())));
            case 7:
                return MethodHandle.constructorCaller(symbolTable.at(input.readUnsigned()),
                        parseMethodDescriptor(symbolTable.at(input.readUnsigned())));
            case 8:
                return MethodHandle.interfaceCaller(symbolTable.at(input.readUnsigned()),
                        parseMethodDescriptor(symbolTable.at(input.readUnsigned())));
            default:
                throw new IllegalArgumentException("Unexpected method handle type: " + kind);
        }
    }

    private RuntimeConstant readRuntimeConstant(VarDataInput input) throws IOException {
        int kind = input.readUnsigned();
        switch (kind) {
            case 0:
                return new RuntimeConstant(input.readSigned());
            case 1:
                return new RuntimeConstant(input.readSignedLong());
            case 2:
                return new RuntimeConstant(input.readFloat());
            case 3:
                return new RuntimeConstant(input.readDouble());
            case 4:
                return new RuntimeConstant(input.read());
            case 5:
                return new RuntimeConstant(parseValueType(symbolTable.at(input.readUnsigned())));
            case 6:
                return new RuntimeConstant(MethodDescriptor.parseSignature(symbolTable.at(input.readUnsigned())));
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
