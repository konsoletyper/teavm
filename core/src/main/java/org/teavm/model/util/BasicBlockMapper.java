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

import java.util.function.IntUnaryOperator;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

public class BasicBlockMapper implements InstructionVisitor {
    private IntUnaryOperator mapFunction;

    public BasicBlockMapper(IntUnaryOperator mapFunction) {
        this.mapFunction = mapFunction;
    }

    private BasicBlock map(BasicBlock block) {
        Program program = block.getProgram();
        return program.basicBlockAt(mapFunction.applyAsInt(block.getIndex()));
    }

    public void transform(Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            transform(program.basicBlockAt(i));
        }
    }

    public void transform(BasicBlock block) {
        Instruction lastInsn = block.getLastInstruction();
        if (lastInsn == null) {
            return;
        }
        lastInsn.acceptVisitor(this);

        for (Phi phi : block.getPhis()) {
            for (Incoming incoming : phi.getIncomings()) {
                incoming.setSource(map(incoming.getSource()));
            }
        }
        for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
            tryCatch.setHandler(map(tryCatch.getHandler()));
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
    }

    @Override
    public void visit(NegateInstruction insn) {
    }

    @Override
    public void visit(AssignInstruction insn) {
    }

    @Override
    public void visit(CastInstruction insn) {
    }

    @Override
    public void visit(CastNumberInstruction insn) {
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
    }

    @Override
    public void visit(BranchingInstruction insn) {
        insn.setConsequent(map(insn.getConsequent()));
        insn.setAlternative(map(insn.getAlternative()));
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        insn.setConsequent(map(insn.getConsequent()));
        insn.setAlternative(map(insn.getAlternative()));
    }

    @Override
    public void visit(JumpInstruction insn) {
        insn.setTarget(map(insn.getTarget()));
    }

    @Override
    public void visit(SwitchInstruction insn) {
        for (SwitchTableEntry entry : insn.getEntries()) {
            entry.setTarget(map(entry.getTarget()));
        }
        insn.setDefaultTarget(map(insn.getDefaultTarget()));
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
    public void visit(InvokeDynamicInstruction insn) {
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

    @Override
    public void visit(MonitorEnterInstruction insn) {
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
    }
}
