package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class CastNumberInstruction extends Instruction {
    private Variable value;
    private Variable receiver;
    private NumericOperandType sourceType;
    private NumericOperandType targetType;

    public CastNumberInstruction(NumericOperandType sourceType, NumericOperandType targetType) {
        super();
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    public Variable getValue() {
        return value;
    }

    public void setValue(Variable value) {
        this.value = value;
    }

    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    public NumericOperandType getSourceType() {
        return sourceType;
    }

    public NumericOperandType getTargetType() {
        return targetType;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
