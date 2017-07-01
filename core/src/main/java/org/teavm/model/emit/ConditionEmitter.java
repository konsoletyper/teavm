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

public class ConditionEmitter {
    private ProgramEmitter pe;
    ForkEmitter fork;

    ConditionEmitter(ProgramEmitter pe, ForkEmitter fork) {
        this.pe = pe;
        this.fork = fork;
    }

    public ConditionEmitter and(ConditionProducer other) {
        BasicBlock block = pe.prepareBlock();
        pe.enter(block);
        ConditionEmitter otherEmitter = other.produce();
        ForkEmitter newFork = fork.and(block, otherEmitter.fork);
        return new ConditionEmitter(pe, newFork);
    }

    public ConditionEmitter or(ConditionProducer other) {
        BasicBlock block = pe.prepareBlock();
        pe.enter(block);
        ConditionEmitter otherEmitter = other.produce();
        ForkEmitter newFork = fork.or(block, otherEmitter.fork);
        return new ConditionEmitter(pe, newFork);
    }

    public ConditionEmitter not() {
        return new ConditionEmitter(pe, fork.not());
    }
}
