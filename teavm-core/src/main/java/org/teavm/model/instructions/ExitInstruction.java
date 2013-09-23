package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class ExitInstruction extends Instruction {
    private Variable valueToReturn;

    public Variable getValueToReturn() {
        return valueToReturn;
    }

    public void setValueToReturn(Variable valueToReturn) {
        this.valueToReturn = valueToReturn;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
