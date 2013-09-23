package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class GetElementInstruction extends Instruction {
    private Variable array;
    private Variable index;
    private Variable receiver;

    public Variable getArray() {
        return array;
    }

    public void setArray(Variable array) {
        this.array = array;
    }

    public Variable getIndex() {
        return index;
    }

    public void setIndex(Variable index) {
        this.index = index;
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
