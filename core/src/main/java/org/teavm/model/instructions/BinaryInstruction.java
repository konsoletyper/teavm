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

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

public class BinaryInstruction extends Instruction {
    private BinaryOperation operation;
    private Variable receiver;
    private Variable firstOperand;
    private Variable secondOperand;
    private NumericOperandType operandType;

    public BinaryInstruction(BinaryOperation operation, NumericOperandType operandType) {
        this.operation = operation;
        this.operandType = operandType;
    }

    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    public Variable getFirstOperand() {
        return firstOperand;
    }

    public void setFirstOperand(Variable firstOperand) {
        this.firstOperand = firstOperand;
    }

    public Variable getSecondOperand() {
        return secondOperand;
    }

    public void setSecondOperand(Variable secondOperand) {
        this.secondOperand = secondOperand;
    }

    public NumericOperandType getOperandType() {
        return operandType;
    }

    public BinaryOperation getOperation() {
        return operation;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
