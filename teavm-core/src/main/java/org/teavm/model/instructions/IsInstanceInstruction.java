package org.teavm.model.instructions;

import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class IsInstanceInstruction extends Instruction {
    private Variable value;
    private ValueType type;
    private Variable receiver;

    public Variable getValue() {
        return value;
    }

    public void setValue(Variable value) {
        this.value = value;
    }

    public ValueType getType() {
        return type;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
