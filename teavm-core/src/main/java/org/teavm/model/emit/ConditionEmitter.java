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
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BranchingCondition;

/**
 *
 * @author Alexey Andreev
 */
public class ConditionEmitter {
    private ProgramEmitter pe;
    private ComputationEmitter argument;
    private BasicBlock join;

    ConditionEmitter(ProgramEmitter pe, ComputationEmitter argument, BasicBlock join) {
        this.pe = pe;
        this.argument = argument;
        this.join = join;
    }

    public IfEmitter isTrue() {
        return new IfEmitter(pe, argument.emit().fork(BranchingCondition.NOT_EQUAL), join);
    }

    public IfEmitter isFalse() {
        return new IfEmitter(pe, argument.emit().fork(BranchingCondition.NOT_NULL), join);
    }

    public IfEmitter equalTo(ComputationEmitter other) {
        return new IfEmitter(pe, argument.emit().fork(BinaryBranchingCondition.EQUAL, other.emit()), join);
    }

    public IfEmitter notEqualTo(ComputationEmitter other) {
        return new IfEmitter(pe, argument.emit().fork(BinaryBranchingCondition.NOT_EQUAL, other.emit()), join);
    }

    public IfEmitter sameAs(ComputationEmitter other) {
        return new IfEmitter(pe, argument.emit().fork(BinaryBranchingCondition.REFERENCE_EQUAL, other.emit()),
                join);
    }

    public IfEmitter notSameAs(ComputationEmitter other) {
        return new IfEmitter(pe, argument.emit().fork(BinaryBranchingCondition.REFERENCE_NOT_EQUAL, other.emit()),
                join);
    }
}
