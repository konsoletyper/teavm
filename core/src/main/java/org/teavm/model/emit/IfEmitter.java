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

public class IfEmitter {
    private ProgramEmitter pe;
    private ForkEmitter fork;
    private BasicBlock join;

    IfEmitter(ProgramEmitter pe, ForkEmitter fork, BasicBlock join) {
        this.pe = pe;
        this.fork = fork;
        this.join = join;
        fork.setThen(join);
        fork.setElse(join);
    }

    public IfEmitter thenDo(FragmentEmitter fragment) {
        BasicBlock block = pe.prepareBlock();
        fork.setThen(block);
        pe.enter(block);
        pe.emitAndJump(fragment, join);
        pe.enter(join);
        return this;
    }

    public IfEmitter elseDo(FragmentEmitter fragment) {
        BasicBlock block = pe.prepareBlock();
        fork.setElse(block);
        pe.enter(block);
        pe.emitAndJump(fragment, join);
        pe.enter(join);
        return this;
    }
}
