package org.teavm.model.util;

import org.teavm.model.Variable;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
public class DefinitionExtractor implements InstructionVisitor {
    private Variable[] definedVariables;

    public Variable[] getDefinedVariables() {
        return definedVariables;
    }

    @Override
    public void visit(EmptyInstruction insn) {
        definedVariables = new Variable[0];
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(BinaryInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(NegateInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(AssignInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(BranchingInstruction insn) {
        definedVariables = new Variable[0];
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        definedVariables = new Variable[0];
    }

    @Override
    public void visit(JumpInstruction insn) {
        definedVariables = new Variable[0];
    }

    @Override
    public void visit(SwitchInstruction insn) {
        definedVariables = new Variable[0];
    }

    @Override
    public void visit(ExitInstruction insn) {
        definedVariables = new Variable[0];
    }

    @Override
    public void visit(RaiseInstruction insn) {
        definedVariables = new Variable[0];
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(ConstructInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        definedVariables = new Variable[0];
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(GetElementInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(PutElementInstruction insn) {
        definedVariables = new Variable[0];
    }

    @Override
    public void visit(InvokeInstruction insn) {
        if (insn.getReceiver() == null) {
            definedVariables = new Variable[0];
        } else {
            definedVariables = new Variable[] { insn.getReceiver() };
        }
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(CastInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        definedVariables = new Variable[] { insn.getReceiver() };
    }

    @Override
    public void visit(InitClassInstruction insn) {
    }
}
