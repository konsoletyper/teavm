/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.model.emit;

import java.util.HashMap;
import java.util.Map;
import org.teavm.model.BasicBlock;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;

public class StringChooseEmitter {
    private ProgramEmitter pe;
    private ValueEmitter testValue;
    private SwitchInstruction insn;
    private BasicBlock joinBlock;
    private Map<Integer, ForkEmitter> hashForks = new HashMap<>();
    private BasicBlock otherwiseBlock;

    StringChooseEmitter(ProgramEmitter pe, ValueEmitter testValue, SwitchInstruction insn, BasicBlock joinBlock) {
        this.pe = pe;
        this.insn = insn;
        this.joinBlock = joinBlock;
        this.otherwiseBlock = pe.prepareBlock();
        this.testValue = testValue;
        insn.setCondition(testValue.invokeVirtual("hashCode", int.class).getVariable());
        insn.setDefaultTarget(otherwiseBlock);
        pe.addInstruction(insn);
        pe.enter(otherwiseBlock);
        pe.jump(joinBlock);
        pe.enter(joinBlock);
    }

    public StringChooseEmitter option(String value, FragmentEmitter fragment) {
        int hash = value.hashCode();

        BasicBlock block = pe.prepareBlock();
        ForkEmitter fork = hashForks.get(hash);
        if (fork == null) {
            SwitchTableEntry entry = new SwitchTableEntry();
            entry.setCondition(hash);
            entry.setTarget(block);
            pe.enter(entry.getTarget());
            insn.getEntries().add(entry);
        } else {
            fork.setElse(block);
        }

        pe.enter(block);
        fork = testValue.invokeVirtual("equals", boolean.class, pe.constant(value).cast(Object.class))
                .fork(BranchingCondition.NOT_EQUAL);
        hashForks.put(hash, fork);

        block = pe.prepareBlock();
        fork.setThen(block);
        fork.setElse(otherwiseBlock);
        pe.enter(block);
        pe.emitAndJump(fragment, joinBlock);
        pe.enter(joinBlock);
        return this;
    }

    public ProgramEmitter otherwise(FragmentEmitter fragment) {
        otherwiseBlock.removeAllInstructions();
        pe.enter(otherwiseBlock);
        pe.emitAndJump(fragment, joinBlock);
        pe.enter(joinBlock);
        return pe;
    }
}
