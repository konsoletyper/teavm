package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class NegateInstruction extends Instruction {
    private NumericOperandType operandType;
    private Variable operand;
    private Variable receiver;

    public NegateInstruction(NumericOperandType operandType) {
        this.operandType = operandType;
    }

    public Variable getOperand() {
        return operand;
    }

    public void setOperand(Variable operand) {
        this.operand = operand;
    }

    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    public NumericOperandType getOperandType() {
        return operandType;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
