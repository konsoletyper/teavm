package org.teavm.model;

import org.teavm.model.instructions.InstructionVisitor;

/**
 *
 * @author Alexey Andreev
 */
public abstract class Instruction {
    private BasicBlock basicBlock;

    void setBasicBlock(BasicBlock basicBlock) {
        this.basicBlock = basicBlock;
    }

    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    public Program getProgram() {
        return basicBlock != null ? basicBlock.getProgram() : null;
    }

    public abstract void acceptVisitor(InstructionVisitor visitor);
}
