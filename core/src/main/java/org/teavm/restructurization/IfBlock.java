/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.restructurization;

import org.teavm.model.Instruction;

public class IfBlock extends LabeledBlock {
    Instruction condition;
    boolean inverted;
    Block thenBody;
    Block elseBody;

    public Instruction getCondition() {
        return condition;
    }

    public boolean isInverted() {
        return inverted;
    }

    public Block getThenBody() {
        return thenBody;
    }

    public Block getElseBody() {
        return elseBody;
    }

    @Override
    public void acceptVisitor(BlockVisitor visitor) {
        visitor.visit(this);
    }
}
