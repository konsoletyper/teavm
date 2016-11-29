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
package org.teavm.model.instructions;

import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Variable;

public class BranchingInstruction extends Instruction {
    private Variable operand;
    private BranchingCondition condition;
    private BasicBlock consequent;
    private BasicBlock alternative;

    public BranchingInstruction(BranchingCondition condition) {
        this.condition = condition;
    }

    public Variable getOperand() {
        return operand;
    }

    public void setOperand(Variable operand) {
        this.operand = operand;
    }

    public BranchingCondition getCondition() {
        return condition;
    }

    public BasicBlock getConsequent() {
        return consequent;
    }

    public void setConsequent(BasicBlock consequent) {
        this.consequent = consequent;
    }

    public BasicBlock getAlternative() {
        return alternative;
    }

    public void setAlternative(BasicBlock alternative) {
        this.alternative = alternative;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
