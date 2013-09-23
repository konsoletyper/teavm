package org.teavm.model.instructions;

import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class AssignInstruction extends Instruction {
    private Variable assignee;
    private Variable receiver;

    public Variable getAssignee() {
        return assignee;
    }

    public void setAssignee(Variable assignee) {
        this.assignee = assignee;
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
