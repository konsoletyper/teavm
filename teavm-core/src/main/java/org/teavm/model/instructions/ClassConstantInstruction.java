package org.teavm.model.instructions;

import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class ClassConstantInstruction extends Instruction {
    private ValueType constant;
    private Variable receiver;

    public ValueType getConstant() {
        return constant;
    }

    public void setConstant(ValueType constant) {
        this.constant = constant;
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
