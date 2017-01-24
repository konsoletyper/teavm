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

import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

public class BasicBlockMapper extends AbstractInstructionVisitor {
    private Function<BasicBlock, BasicBlock> mapFunction;

    public BasicBlockMapper(Function<BasicBlock, BasicBlock> mapFunction) {
        this.mapFunction = mapFunction;
    }

    public BasicBlockMapper(IntUnaryOperator mapFunction) {
        this((BasicBlock block) -> block.getProgram().basicBlockAt(mapFunction.applyAsInt(block.getIndex())));
    }

    private BasicBlock map(BasicBlock block) {
        return mapFunction.apply(block);
    }

    public void transform(Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            transform(program.basicBlockAt(i));
        }
    }

    public void transform(BasicBlock block) {
        Instruction lastInsn = block.getLastInstruction();
        if (lastInsn != null) {
            lastInsn.acceptVisitor(this);
        }

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
}
