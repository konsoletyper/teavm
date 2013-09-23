package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class PutElementInstruction extends Instruction {
    private Variable array;
    private Variable index;
    private Variable value;

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

    public Variable getValue() {
        return value;
    }

    public void setValue(Variable value) {
        this.value = value;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
