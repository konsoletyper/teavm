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

import org.teavm.model.BasicBlock;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;

public class ChooseEmitter {
    private ProgramEmitter pe;
    private SwitchInstruction insn;
    private BasicBlock joinBlock;

    ChooseEmitter(ProgramEmitter pe, SwitchInstruction insn, BasicBlock joinBlock) {
        this.pe = pe;
        this.insn = insn;
        this.joinBlock = joinBlock;
        insn.setDefaultTarget(joinBlock);
    }

    public ChooseEmitter option(int value, FragmentEmitter fragment) {
        SwitchTableEntry entry = new SwitchTableEntry();
        entry.setCondition(value);
        entry.setTarget(pe.prepareBlock());
        pe.enter(entry.getTarget());
        pe.emitAndJump(fragment, joinBlock);
        pe.enter(joinBlock);
        insn.getEntries().add(entry);
        return this;
    }

    public ProgramEmitter otherwise(FragmentEmitter fragment) {
        insn.setDefaultTarget(pe.prepareBlock());
        pe.enter(insn.getDefaultTarget());
        pe.emitAndJump(fragment, joinBlock);
        pe.enter(joinBlock);
        return pe;
    }

    public FragmentEmitter breakChoise() {
        return () -> pe.jump(joinBlock);
    }
}
