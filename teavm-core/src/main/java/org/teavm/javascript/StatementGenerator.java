package org.teavm.javascript;

import java.util.*;
import org.teavm.common.GraphIndexer;
import org.teavm.javascript.ast.*;
import org.teavm.javascript.ast.BinaryOperation;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.instructions.InvocationType;

/**
 *
 * @author Alexey Andreev
 */
public class StatementGenerator implements InstructionVisitor {
    private int lastSwitchId;
    List<Statement> statements = new ArrayList<>();
    GraphIndexer indexer;
    BasicBlock nextBlock;
    BasicBlock currentBlock;
    Decompiler.Block[] blockMap;
    Program program;
    ClassHolderSource classSource;

    @Override
    public void visit(EmptyInstruction insn) {
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver().getIndex());
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        assign(Expr.constant(null), insn.getReceiver().getIndex());
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver().getIndex());
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver().getIndex());
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver().getIndex());
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver().getIndex());
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver().getIndex());
    }

    @Override
    public void visit(BinaryInstruction insn) {
        int first = insn.getFirstOperand().getIndex();
        int second = insn.getSecondOperand().getIndex();
        int result = insn.getReceiver().getIndex();
        switch (insn.getOperation()) {
            case ADD:
                switch (insn.getOperandType()) {
                    case INT:
                        intBinary(first, second, result, BinaryOperation.ADD);
                        break;
                    case LONG:
                        binary(first, second, result, BinaryOperation.ADD_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.ADD);
                        break;
                }
                break;
            case SUBTRACT:
                switch (insn.getOperandType()) {
                    case INT:
                        intBinary(first, second, result, BinaryOperation.SUBTRACT);
                        break;
                    case LONG:
                        binary(first, second, result, BinaryOperation.SUBTRACT_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.SUBTRACT);
                        break;
                }
                break;
            case MULTIPLY:
                switch (insn.getOperandType()) {
                    case INT:
                        intBinary(first, second, result, BinaryOperation.MULTIPLY);
                        break;
                    case LONG:
                        binary(first, second, result, BinaryOperation.MULTIPLY_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.MULTIPLY);
                        break;
                }
                break;
            case DIVIDE:
                switch (insn.getOperandType()) {
                    case INT:
                        intBinary(first, second, result, BinaryOperation.DIVIDE);
                        break;
                    case LONG:
                        binary(first, second, result, BinaryOperation.DIVIDE_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.DIVIDE);
                        break;
                }
                break;
            case MODULO:
                switch (insn.getOperandType()) {
                    case LONG:
                        binary(first, second, result, BinaryOperation.MODULO_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.MODULO);
                        break;
                }
                break;
            case COMPARE:
                switch (insn.getOperandType()) {
                    case LONG:
                        binary(first, second, result, BinaryOperation.COMPARE_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.COMPARE);
                        break;
                }
                break;
            case AND:
                switch (insn.getOperandType()) {
                    case LONG:
                        binary(first, second, result, BinaryOperation.BITWISE_AND_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.BITWISE_AND);
                        break;
                }
                break;
            case OR:
                switch (insn.getOperandType()) {
                    case LONG:
                        binary(first, second, result, BinaryOperation.BITWISE_OR_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.BITWISE_OR);
                        break;
                }
                break;
            case XOR:
                switch (insn.getOperandType()) {
                    case LONG:
                        binary(first, second, result, BinaryOperation.BITWISE_XOR_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.BITWISE_XOR);
                        break;
                }
                break;
            case SHIFT_LEFT:
                switch (insn.getOperandType()) {
                    case LONG:
                        binary(first, second, result, BinaryOperation.LEFT_SHIFT_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.LEFT_SHIFT);
                        break;
                }
                break;
            case SHIFT_RIGHT:
                switch (insn.getOperandType()) {
                    case LONG:
                        binary(first, second, result, BinaryOperation.RIGHT_SHIFT_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.RIGHT_SHIFT);
                        break;
                }
                break;
            case SHIFT_RIGHT_UNSIGNED:
                switch (insn.getOperandType()) {
                    case LONG:
                        binary(first, second, result, BinaryOperation.UNSIGNED_RIGHT_SHIFT_LONG);
                        break;
                    default:
                        binary(first, second, result, BinaryOperation.UNSIGNED_RIGHT_SHIFT);
                        break;
                }
                break;
        }
    }

    @Override
    public void visit(NegateInstruction insn) {
        switch (insn.getOperandType()) {
            case INT:
                assign(castToInteger(Expr.unary(UnaryOperation.NEGATE, Expr.var(insn.getOperand().getIndex()))),
                        insn.getReceiver().getIndex());
                break;
            case LONG:
                assign(Expr.unary(UnaryOperation.NEGATE_LONG, Expr.var(insn.getOperand().getIndex())),
                        insn.getReceiver().getIndex());
                break;
            default:
                assign(Expr.unary(UnaryOperation.NEGATE, Expr.var(insn.getOperand().getIndex())),
                        insn.getReceiver().getIndex());
                break;
        }
    }

    @Override
    public void visit(AssignInstruction insn) {
        statements.add(Statement.assign(Expr.var(insn.getReceiver().getIndex()),
                Expr.var(insn.getAssignee().getIndex())));
    }

    @Override
    public void visit(CastInstruction insn) {
        assign(Expr.var(insn.getValue().getIndex()), insn.getReceiver().getIndex());
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        Expr value = Expr.var(insn.getValue().getIndex());
        switch (insn.getTargetType()) {
            case INT:
                switch (insn.getSourceType()) {
                    case DOUBLE:
                    case FLOAT:
                        value = castToInteger(value);
                        break;
                    case LONG:
                        value = castFromLong(value);
                        break;
                    default:
                        break;
                }
                break;
            case LONG:
                switch (insn.getSourceType()) {
                    case INT:
                        value = castIntToLong(value);
                        break;
                    case FLOAT:
                    case DOUBLE:
                        value = castToLong(value);
                        break;
                    default:
                        break;
                }
                break;
            case FLOAT:
            case DOUBLE:
                if (insn.getSourceType() == NumericOperandType.LONG) {
                    value = castFromLong(value);
                }
                break;
            default:
                break;
        }
        assign(value, insn.getReceiver().getIndex());
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        Expr value = Expr.var(insn.getValue().getIndex());
        switch (insn.getDirection()) {
            case FROM_INTEGER:
                switch (insn.getTargetType()) {
                    case BYTE:
                        value = Expr.binary(BinaryOperation.BITWISE_AND, value, Expr.constant(0xFF));
                        break;
                    case SHORT:
                    case CHARACTER:
                        value = Expr.binary(BinaryOperation.BITWISE_AND, value, Expr.constant(0xFFFF));
                        break;
                }
                break;
            case TO_INTEGER:
                switch (insn.getTargetType()) {
                    case BYTE:
                        value = Expr.unary(UnaryOperation.BYTE_TO_INT, value);
                        break;
                    case SHORT:
                        value = Expr.unary(UnaryOperation.SHORT_TO_INT, value);
                        break;
                    case CHARACTER:
                        break;
                }
                break;
        }
        assign(value, insn.getReceiver().getIndex());
    }

    @Override
    public void visit(BranchingInstruction insn) {
        switch (insn.getCondition()) {
            case EQUAL:
                branch(compare(BinaryOperation.EQUALS, insn.getOperand()), insn.getConsequent(),
                        insn.getAlternative());
                break;
            case NOT_EQUAL:
                branch(compare(BinaryOperation.NOT_EQUALS, insn.getOperand()), insn.getConsequent(),
                        insn.getAlternative());
                break;
            case GREATER:
                branch(compare(BinaryOperation.GREATER, insn.getOperand()), insn.getConsequent(),
                        insn.getAlternative());
                break;
            case GREATER_OR_EQUAL:
                branch(compare(BinaryOperation.GREATER_OR_EQUALS, insn.getOperand()), insn.getConsequent(),
                        insn.getAlternative());
                break;
            case LESS:
                branch(compare(BinaryOperation.LESS, insn.getOperand()), insn.getConsequent(),
                        insn.getAlternative());
                break;
            case LESS_OR_EQUAL:
                branch(compare(BinaryOperation.LESS_OR_EQUALS, insn.getOperand()), insn.getConsequent(),
                        insn.getAlternative());
                break;
            case NOT_NULL:
                branch(Expr.binary(BinaryOperation.STRICT_NOT_EQUALS, Expr.var(insn.getOperand().getIndex()),
                        Expr.constant(null)), insn.getConsequent(), insn.getAlternative());
                break;
            case NULL:
                branch(Expr.binary(BinaryOperation.STRICT_EQUALS, Expr.var(insn.getOperand().getIndex()),
                        Expr.constant(null)), insn.getConsequent(), insn.getAlternative());
                break;
        }
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
        int a = insn.getFirstOperand().getIndex();
        int b = insn.getSecondOperand().getIndex();
        BasicBlock consequent = insn.getConsequent();
        BasicBlock alternative = insn.getAlternative();
        switch (insn.getCondition()) {
            case EQUAL:
                branch(Expr.binary(BinaryOperation.EQUALS, Expr.var(a), Expr.var(b)), consequent, alternative);
                break;
            case REFERENCE_EQUAL:
                branch(Expr.binary(BinaryOperation.STRICT_EQUALS, Expr.var(a), Expr.var(b)), consequent, alternative);
                break;
            case NOT_EQUAL:
                branch(Expr.binary(BinaryOperation.NOT_EQUALS, Expr.var(a), Expr.var(b)), consequent, alternative);
                break;
            case REFERENCE_NOT_EQUAL:
                branch(Expr.binary(BinaryOperation.STRICT_NOT_EQUALS, Expr.var(a), Expr.var(b)),
                        consequent, alternative);
                break;
        }
    }

    @Override
    public void visit(JumpInstruction insn) {
        Statement stmt = generateJumpStatement(insn.getTarget());
        if (stmt != null) {
            statements.add(stmt);
        }
    }

    @Override
    public void visit(SwitchInstruction insn) {
        SwitchStatement stmt = new SwitchStatement();
        stmt.setId("sblock" + (lastSwitchId++));
        stmt.setValue(Expr.var(insn.getCondition().getIndex()));
        Map<Integer, List<Integer>> switchMap = new HashMap<>();
        for (int i = 0; i < insn.getEntries().size(); ++i) {
            SwitchTableEntry entry = insn.getEntries().get(i);
            List<Integer> conditions = switchMap.get(entry.getTarget());
            if (conditions == null) {
                conditions = new ArrayList<>();
                switchMap.put(entry.getTarget().getIndex(), conditions);
            }
            conditions.add(entry.getCondition());
        }
        List<Integer> targets = new ArrayList<>(switchMap.keySet());
        Collections.sort(targets);
        for (int target : targets) {
            SwitchClause clause = new SwitchClause();
            List<Integer> conditionList = switchMap.get(target);
            int[] conditions = new int[conditionList.size()];
            for (int i = 0; i < conditionList.size(); ++i) {
                conditions[i] = conditionList.get(i);
            }
            clause.setConditions(conditions);
            clause.setStatement(generateJumpStatement(stmt, target));
            stmt.getClauses().add(clause);
        }
        stmt.setDefaultClause(generateJumpStatement(insn.getDefaultTarget()));
        statements.add(stmt);
    }

    @Override
    public void visit(ExitInstruction insn) {
        statements.add(Statement.exitFunction(insn.getValueToReturn() != null ?
                Expr.var(insn.getValueToReturn().getIndex()) : null));
    }

    @Override
    public void visit(RaiseInstruction insn) {
        ThrowStatement stmt = new ThrowStatement();
        stmt.setException(Expr.var(insn.getException().getIndex()));
        statements.add(stmt);
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        assign(Expr.createArray(insn.getItemType(), Expr.var(insn.getSize().getIndex())),
                insn.getReceiver().getIndex());
    }

    @Override
    public void visit(ConstructInstruction insn) {
        assign(Expr.createObject(insn.getType()), insn.getReceiver().getIndex());
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        Expr[] dimensionExprs = new Expr[insn.getDimensions().size()];
        for (int i = 0; i < dimensionExprs.length; ++i) {
            dimensionExprs[i] = Expr.var(insn.getDimensions().get(i).getIndex());
        }
        assign(Expr.createArray(insn.getItemType(), dimensionExprs), insn.getReceiver().getIndex());
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        if (insn.getInstance() != null) {
            statements.add(Statement.assign(Expr.var(insn.getReceiver().getIndex()),
                    Expr.qualify(Expr.var(insn.getInstance().getIndex()), insn.getField())));
        } else {
            Expr fieldExpr = Expr.qualify(Expr.staticClass(ValueType.object(insn.getField().getClassName())),
                    insn.getField());
            statements.add(Statement.assign(Expr.var(insn.getReceiver().getIndex()), fieldExpr));
        }
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        if (insn.getInstance() != null) {
            statements.add(Statement.assign(Expr.qualify(Expr.var(insn.getInstance().getIndex()), insn.getField()),
                    Expr.var(insn.getValue().getIndex())));
        } else {
            Expr fieldExpr = Expr.qualify(Expr.staticClass(ValueType.object(insn.getField().getClassName())),
                    insn.getField());
            statements.add(Statement.assign(fieldExpr, Expr.var(insn.getValue().getIndex())));
        }
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        assign(Expr.unary(UnaryOperation.LENGTH, Expr.var(insn.getArray().getIndex())), insn.getReceiver().getIndex());
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        UnwrapArrayExpr unwrapExpr = new UnwrapArrayExpr(insn.getElementType());
        unwrapExpr.setArray(Expr.var(insn.getArray().getIndex()));
        assign(unwrapExpr, insn.getReceiver().getIndex());
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        MethodDescriptor cloneMethodDesc = new MethodDescriptor("clone", ValueType.object("java.lang.Object"));
        MethodReference cloneMethod = new MethodReference("java.lang.Object", cloneMethodDesc);
        assign(Expr.invoke(cloneMethod, Expr.var(insn.getArray().getIndex()), new Expr[0]),
                insn.getReceiver().getIndex());
    }

    @Override
    public void visit(GetElementInstruction insn) {
        assign(Expr.subscript(Expr.var(insn.getArray().getIndex()), Expr.var(insn.getIndex().getIndex())),
                insn.getReceiver().getIndex());
    }

    @Override
    public void visit(PutElementInstruction insn) {
        statements.add(Statement.assign(Expr.subscript(Expr.var(insn.getArray().getIndex()),
                Expr.var(insn.getIndex().getIndex())), Expr.var(insn.getValue().getIndex())));
    }

    @Override
    public void visit(InvokeInstruction insn) {
        MethodReference method = findDeclaringClass(insn.getMethod());
        if (method == null) {
            throw new IllegalArgumentException("Method not found: " + insn.getMethod());
        }
        Expr[] exprArgs = new Expr[insn.getMethod().getParameterTypes().length];
        for (int i = 0; i < insn.getArguments().size(); ++i) {
            exprArgs[i] = Expr.var(insn.getArguments().get(i).getIndex());
        }
        Expr invocationExpr;
        if (insn.getInstance() != null) {
            if (insn.getType() == InvocationType.VIRTUAL) {
                invocationExpr = Expr.invoke(insn.getMethod(), Expr.var(insn.getInstance().getIndex()), exprArgs);
            } else {
                invocationExpr = Expr.invokeSpecial(method, Expr.var(insn.getInstance().getIndex()), exprArgs);
            }
        } else {
            invocationExpr = Expr.invokeStatic(method, exprArgs);
        }
        if (insn.getReceiver() != null) {
            assign(invocationExpr, insn.getReceiver().getIndex());
        } else {
            statements.add(Statement.assign(null, invocationExpr));
        }
    }

    public MethodReference findDeclaringClass(MethodReference method) {
        ClassHolder cls = classSource.getClassHolder(method.getClassName());
        while (cls != null && cls.getMethod(method.getDescriptor()) == null) {
            cls = cls.getParent() != null ? classSource.getClassHolder(cls.getParent()) : null;
        }
        return cls != null ? new MethodReference(cls.getName(), method.getDescriptor()) : null;
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        assign(Expr.instanceOf(Expr.var(insn.getValue().getIndex()), insn.getType()),
                insn.getReceiver().getIndex());
    }

    private void assign(Expr source, int target) {
        statements.add(Statement.assign(Expr.var(target), source));
    }

    private Expr castToInteger(Expr value) {
        return Expr.binary(BinaryOperation.BITWISE_OR, value, Expr.constant(0));
    }

    private Expr castToLong(Expr value) {
        return Expr.unary(UnaryOperation.NUM_TO_LONG, value);
    }

    private Expr castIntToLong(Expr value) {
        return Expr.unary(UnaryOperation.INT_TO_LONG, value);
    }

    private Expr castFromLong(Expr value) {
        return Expr.unary(UnaryOperation.LONG_TO_NUM, value);
    }

    private void binary(int first, int second, int result, BinaryOperation op) {
        assign(Expr.binary(op, Expr.var(first), Expr.var(second)), result);
    }

    private void intBinary(int first, int second, int result, BinaryOperation op) {
        assign(castToInteger(Expr.binary(op, Expr.var(first), Expr.var(second))), result);
    }

    private Statement generateJumpStatementWithoutPhis(BasicBlock target) {
        if (nextBlock == target) {
            return null;
        }
        Decompiler.Block block = blockMap[target.getIndex()];
        if (target.getIndex() == indexer.nodeAt(block.end)) {
            BreakStatement breakStmt = new BreakStatement();
            breakStmt.setTarget(block.statement);
            return breakStmt;
        } else {
            ContinueStatement contStmt = new ContinueStatement();
            contStmt.setTarget(block.statement);
            return contStmt;
        }
    }

    private Statement wrapWithPhis(Statement rawJump, BasicBlock target) {
        SequentialStatement seq = new SequentialStatement();
        for (Phi phi : target.getPhis()) {
            for (Incoming outgoing : phi.getIncomings()) {
                if (outgoing.getSource() == currentBlock) {
                    seq.getSequence().add(Statement.assign(Expr.var(outgoing.getPhi().getReceiver().getIndex()),
                            Expr.var(outgoing.getValue().getIndex())));
                }
            }
        }
        if (rawJump != null) {
            seq.getSequence().add(rawJump);
        }
        return !seq.getSequence().isEmpty() ? seq : null;
    }

    private Statement generateJumpStatement(BasicBlock target) {
        return wrapWithPhis(generateJumpStatementWithoutPhis(target), target);
    }

    private Statement generateJumpStatementWithoutPhis(SwitchStatement stmt, int target) {
        Statement body = generateJumpStatementWithoutPhis(program.basicBlockAt(target));
        if (body == null) {
            BreakStatement breakStmt = new BreakStatement();
            breakStmt.setTarget(stmt);
            body = breakStmt;
        }
        return body;
    }

    private Statement generateJumpStatement(SwitchStatement stmt, int target) {
        return wrapWithPhis(generateJumpStatementWithoutPhis(stmt, target), program.basicBlockAt(target));
    }

    private void branch(Expr condition, BasicBlock consequentBlock, BasicBlock alternativeBlock) {
        Statement consequent = generateJumpStatement(consequentBlock);
        Statement alternative = generateJumpStatement(alternativeBlock);
        if (consequent == null) {
            consequent = Statement.empty();
        }
        if (alternative == null) {
            alternative = Statement.empty();
        }
        statements.add(Statement.cond(condition, consequent, alternative));
    }

    private Expr compare(BinaryOperation op, Variable value) {
        return Expr.binary(op, Expr.var(value.getIndex()), Expr.constant(0));
    }
}
