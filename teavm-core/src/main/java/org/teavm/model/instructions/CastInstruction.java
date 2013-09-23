package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class CastInstruction extends Instruction {
    private Variable value;
    private Variable receiver;
    private ValueType targetType;

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

    public ValueType getTargetType() {
        return targetType;
    }

    public void setTargetType(ValueType targetType) {
        this.targetType = targetType;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
