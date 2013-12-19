package org.teavm.model.instructions;

import org.teavm.model.Instruction;

/**
 *
 * @author Alexey Andreev
 */
public class InitClassInstruction extends Instruction {
    private String className;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
