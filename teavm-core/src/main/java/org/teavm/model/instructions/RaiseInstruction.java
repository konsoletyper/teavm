package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class RaiseInstruction extends Instruction {
    private Variable exception;

    public Variable getException() {
        return exception;
    }

    public void setException(Variable exception) {
        this.exception = exception;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
