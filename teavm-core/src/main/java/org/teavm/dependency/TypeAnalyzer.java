package org.teavm.dependency;

import org.teavm.model.ClassHolderSource;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
class TypeAnalyzer implements InstructionVisitor {
    private ClassHolderSource classSource;
    private ValueType[] types;
    private int[] definedVars;

    public TypeAnalyzer(ClassHolderSource classSource, int variableCount) {
        types = new ValueType[variableCount];
    }

    @Override
    public void visit(EmptyInstruction insn) {
    }

    private void define(Variable var, ValueType type) {
        types[var.getIndex()] = type;
        definedVars = new int[] { var.getIndex() };
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.object("java.lang.Class"));
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.NULL);
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.INTEGER);
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.LONG);
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.FLOAT);
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.DOUBLE);
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.object("java.lang.String"));
    }

    @Override
    public void visit(BinaryInstruction insn) {
        switch (insn.getOperation()) {
            case ADD:
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
            case SHIFT_RIGHT_UNSIGNED:
            case AND:
            case OR:
            case XOR:
                define(insn.getReceiver(), map(insn.getOperandType()));
                break;
            case COMPARE:
                define(insn.getReceiver(), ValueType.INTEGER);
                break;
        }
    }

    private ValueType map(NumericOperandType type) {
        switch (type) {
            case INT:
                return ValueType.INTEGER;
            case LONG:
                return ValueType.LONG;
            case FLOAT:
                return ValueType.FLOAT;
            case DOUBLE:
                return ValueType.DOUBLE;
        }
        throw new AssertionError("Unknown type: " + type);
    }

    @Override
    public void visit(NegateInstruction insn) {
        define(insn.getReceiver(), map(insn.getOperandType()));
    }

    @Override
    public void visit(AssignInstruction insn) {
        define(insn.getReceiver(), types[insn.getAssignee().getIndex()]);
    }

    @Override
    public void visit(CastInstruction insn) {
        define(insn.getReceiver(), insn.getTargetType());
    }

    @Override
    public void visit(CastNumberInstruction insn) {
    }

    @Override
    public void visit(BranchingInstruction insn) {
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
    }

    @Override
    public void visit(JumpInstruction insn) {
    }

    @Override
    public void visit(SwitchInstruction insn) {
    }

    @Override
    public void visit(ExitInstruction insn) {
    }

    @Override
    public void visit(RaiseInstruction insn) {
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
    }

    @Override
    public void visit(ConstructInstruction insn) {
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
    }

    @Override
    public void visit(GetFieldInstruction insn) {
    }

    @Override
    public void visit(PutFieldInstruction insn) {
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
    }

    @Override
    public void visit(GetElementInstruction insn) {
    }

    @Override
    public void visit(PutElementInstruction insn) {
    }

    @Override
    public void visit(InvokeInstruction insn) {
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
    }
}
