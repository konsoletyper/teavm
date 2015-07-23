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
import org.teavm.model.instructions.BranchingCondition;

/**
 *
 * @author Alexey Andreev
 */
public class IfEmitter {
    private ProgramEmitter pe;
    private ForkEmitter fork;
    private BasicBlock join;

    IfEmitter(ProgramEmitter pe, ForkEmitter fork) {
        this.pe = pe;
        this.fork = fork;
        this.join = pe.createBlock();
    }

    public IfEmitter and(ComputationEmitter condition) {
        BasicBlock block = pe.createBlock();
        fork = fork.and(block, condition.emit(pe).fork(BranchingCondition.NOT_EQUAL));
        pe.setBlock(join);
        return this;
    }

    public IfEmitter or(ComputationEmitter condition) {
        BasicBlock block = pe.createBlock();
        fork = fork.or(block, condition.emit(pe).fork(BranchingCondition.NOT_EQUAL));
        pe.setBlock(join);
        return this;
    }

    public IfEmitter thenDo(FragmentEmitter fragment) {
        fork.setThen(pe.createBlock());
        fragment.emit(pe);
        pe.jump(join);
        return this;
    }

    public IfEmitter elseDo(FragmentEmitter fragment) {
        fork.setThen(pe.createBlock());
        fragment.emit(pe);
        pe.jump(join);
        return this;
    }
}
