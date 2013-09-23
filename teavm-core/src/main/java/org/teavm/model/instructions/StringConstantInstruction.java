package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class StringConstantInstruction extends Instruction {
    private String constant;
    private Variable receiver;

    public String getConstant() {
        return constant;
    }

    public void setConstant(String constant) {
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
