/*
 *  Copyright 2013 Alexey Andreev.
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

import java.util.List;
import org.teavm.model.BasicBlock;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.instructions.*;

public class InstructionTransitionExtractor implements InstructionVisitor {
    private BasicBlock[] targets;

    public BasicBlock[] getTargets() {
        return targets;
    }

    @Override
    public void visit(EmptyInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(BinaryInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(NegateInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(AssignInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(BranchingInstruction insn) {
        targets = new BasicBlock[] { insn.getConsequent(), insn.getAlternative() };
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        targets = new BasicBlock[] { insn.getConsequent(), insn.getAlternative() };
    }

    @Override
    public void visit(JumpInstruction insn) {
        targets = new BasicBlock[] { insn.getTarget() };
    }

    @Override
    public void visit(SwitchInstruction insn) {
        List<SwitchTableEntry> entries = insn.getEntries();
        targets = new BasicBlock[entries.size() + 1];
        for (int i = 0; i < entries.size(); ++i) {
            targets[i] = entries.get(i).getTarget();
        }
        targets[entries.size()] = insn.getDefaultTarget();
    }

    @Override
    public void visit(ExitInstruction insn) {
        targets = new BasicBlock[0];
    }

    @Override
    public void visit(RaiseInstruction insn) {
        targets = new BasicBlock[0];
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(ConstructInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(GetElementInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(PutElementInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(InvokeInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(InvokeDynamicInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(CastInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(InitClassInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(NullCheckInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(MonitorEnterInstruction insn) {
        targets = null;
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
        targets = null;
    }
}
