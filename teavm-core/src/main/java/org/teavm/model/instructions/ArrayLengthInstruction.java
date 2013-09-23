package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class ArrayLengthInstruction extends Instruction {
    private Variable array;
    private Variable receiver;

    public Variable getArray() {
        return array;
    }

    public void setArray(Variable array) {
        this.array = array;
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
