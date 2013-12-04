package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class CastIntegerInstruction extends Instruction {
    private Variable value;
    private Variable receiver;
    private IntegerSubtype targetType;
    private CastIntegerDirection direction;

    public CastIntegerInstruction(IntegerSubtype targetType, CastIntegerDirection direction) {
        this.targetType = targetType;
        this.direction = direction;
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

    public IntegerSubtype getTargetType() {
        return targetType;
    }

    public CastIntegerDirection getDirection() {
        return direction;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
