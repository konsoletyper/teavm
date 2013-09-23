package org.teavm.model.instructions;

/**
 *
 * @author Alexey Andreev
 */
public interface InstructionVisitor {
    void visit(EmptyInstruction insn);

    void visit(ClassConstantInstruction insn);

    void visit(NullConstantInstruction insn);

    void visit(IntegerConstantInstruction insn);

    void visit(LongConstantInstruction insn);

    void visit(FloatConstantInstruction insn);

    void visit(DoubleConstantInstruction insn);

    void visit(StringConstantInstruction insn);

    void visit(BinaryInstruction insn);

    void visit(NegateInstruction insn);

    void visit(AssignInstruction insn);

    void visit(CastInstruction insn);

    void visit(CastNumberInstruction insn);

    void visit(BranchingInstruction insn);

    void visit(BinaryBranchingInstruction insn);

    void visit(JumpInstruction insn);

    void visit(SwitchInstruction insn);

    void visit(ExitInstruction insn);

    void visit(RaiseInstruction insn);

    void visit(ConstructArrayInstruction insn);

    void visit(ConstructInstruction insn);

    void visit(ConstructMultiArrayInstruction insn);

    void visit(GetFieldInstruction insn);

    void visit(PutFieldInstruction insn);

    void visit(ArrayLengthInstruction insn);

    void visit(CloneArrayInstruction insn);

    void visit(GetElementInstruction insn);

    void visit(PutElementInstruction insn);

    void visit(InvokeInstruction insn);

    void visit(IsInstanceInstruction insn);
}
