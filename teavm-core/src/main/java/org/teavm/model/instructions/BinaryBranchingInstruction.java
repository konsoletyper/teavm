package org.teavm.model.instructions;

import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class BinaryBranchingInstruction extends Instruction {
    private Variable firstOperand;
    private Variable secondOperand;
    private BinaryBranchingCondition condition;
    private BasicBlock consequent;
    private BasicBlock alternative;

    public BinaryBranchingInstruction(BinaryBranchingCondition condition) {
        this.condition = condition;
    }

    public Variable getFirstOperand() {
        return firstOperand;
    }

    public void setFirstOperand(Variable firstOperand) {
        this.firstOperand = firstOperand;
    }

    public Variable getSecondOperand() {
        return secondOperand;
    }

    public void setSecondOperand(Variable secondOperand) {
        this.secondOperand = secondOperand;
    }

    public BinaryBranchingCondition getCondition() {
        return condition;
    }

    public BasicBlock getConsequent() {
        return consequent;
    }

    public void setConsequent(BasicBlock consequent) {
        this.consequent = consequent;
    }

    public BasicBlock getAlternative() {
        return alternative;
    }

    public void setAlternative(BasicBlock alternative) {
        this.alternative = alternative;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
