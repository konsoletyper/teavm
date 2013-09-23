package org.teavm.model.instructions;

import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class ConstructArrayInstruction extends Instruction {
    private Variable size;
    private ValueType itemType;
    private Variable receiver;

    public Variable getSize() {
        return size;
    }

    public void setSize(Variable size) {
        this.size = size;
    }

    public ValueType getItemType() {
        return itemType;
    }

    public void setItemType(ValueType itemType) {
        this.itemType = itemType;
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
