package org.teavm.model.instructions;

import org.teavm.model.Instruction;

/**
 *
 * @author Alexey Andreev
 */
public class EmptyInstruction extends Instruction {
    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
