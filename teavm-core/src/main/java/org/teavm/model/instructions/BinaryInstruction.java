package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
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
