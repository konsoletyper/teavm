/*
 *  Copyright 2013 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.javascript;

import java.util.*;
import org.teavm.common.GraphIndexer;
import org.teavm.javascript.ast.*;
import org.teavm.javascript.ast.BinaryOperation;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.instructions.InvocationType;

class StatementGenerator implements InstructionVisitor {
    private int lastSwitchId;
    final List<Statement> statements = new ArrayList<>();
    GraphIndexer indexer;
    BasicBlock nextBlock;
    BasicBlock currentBlock;
    Decompiler.Block[] blockMap;
    Program program;
    ClassHolderSource classSource;
    private NodeLocation currentLocation;
    boolean async;

    void setCurrentLocation(NodeLocation currentLocation) {
        this.currentLocation = currentLocation;
    }

    @Override
    public void visit(EmptyInstruction insn) {
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver());
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        assign(Expr.constant(null), insn.getReceiver());
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver());
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver());
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver());
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver());
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        assign(Expr.constant(insn.getConstant()), insn.getReceiver());
    }

    @Override
    public void visit(BinaryInstruction insn) {
        int first = insn.getFirstOperand().getIndex();
        int second = insn.getSecondOperand().getIndex();
        Variable result = insn.getReceiver();
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
                        insn.getReceiver());
                break;
            case LONG:
                assign(Expr.unary(UnaryOperation.NEGATE_LONG, Expr.var(insn.getOperand().getIndex())),
                        insn.getReceiver());
                break;
            default:
                assign(Expr.unary(UnaryOperation.NEGATE, Expr.var(insn.getOperand().getIndex())),
                        insn.getReceiver());
                break;
        }
    }

    @Override
    public void visit(AssignInstruction insn) {
        AssignmentStatement stmt = Statement.assign(Expr.var(insn.getReceiver().getIndex()),
                Expr.var(insn.getAssignee().getIndex()));
        stmt.getDebugNames().addAll(insn.getReceiver().getDebugNames());
        stmt.setLocation(currentLocation);
        statements.add(stmt);
    }

    @Override
    public void visit(CastInstruction insn) {
        assign(Expr.var(insn.getValue().getIndex()), insn.getReceiver());
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
                        value = castLongToInt(value);
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
        assign(value, insn.getReceiver());
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        Expr value = Expr.var(insn.getValue().getIndex());
        switch (insn.getDirection()) {
            case FROM_INTEGER:
                switch (insn.getTargetType()) {
                    case BYTE:
                        value = Expr.unary(UnaryOperation.INT_TO_BYTE, value);
                        break;
                    case SHORT:
                        value = Expr.unary(UnaryOperation.INT_TO_SHORT, value);
                        break;
                    case CHARACTER:
                        value = Expr.unary(UnaryOperation.INT_TO_CHAR, value);
                        break;
                }
                break;
            case TO_INTEGER:
                break;
        }
        assign(value, insn.getReceiver());
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
                branch(withLocation(Expr.binary(BinaryOperation.EQUALS, Expr.var(a), Expr.var(b))),
                        consequent, alternative);
                break;
            case REFERENCE_EQUAL:
                branch(withLocation(Expr.binary(BinaryOperation.STRICT_EQUALS, Expr.var(a), Expr.var(b))),
                        consequent, alternative);
                break;
            case NOT_EQUAL:
                branch(withLocation(Expr.binary(BinaryOperation.NOT_EQUALS, Expr.var(a), Expr.var(b))),
                        consequent, alternative);
                break;
            case REFERENCE_NOT_EQUAL:
                branch(withLocation(Expr.binary(BinaryOperation.STRICT_NOT_EQUALS, Expr.var(a), Expr.var(b))),
                        consequent, alternative);
                break;
        }
    }

    private Expr withLocation(Expr expr) {
        expr.setLocation(currentLocation);
        return expr;
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
            List<Integer> conditions = switchMap.get(entry.getTarget().getIndex());
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
            Statement jumpStmt = generateJumpStatement(stmt, target);
            if (jumpStmt != null) {
                clause.getBody().add(jumpStmt);
            }
            stmt.getClauses().add(clause);
        }
        Statement breakStmt = generateJumpStatement(insn.getDefaultTarget());
        if (breakStmt != null) {
            stmt.getDefaultClause().add(breakStmt);
        }
        statements.add(stmt);
    }

    @Override
    public void visit(ExitInstruction insn) {
        ReturnStatement stmt = Statement.exitFunction(insn.getValueToReturn() != null
                ? Expr.var(insn.getValueToReturn().getIndex()) : null);
        stmt.setLocation(currentLocation);
        statements.add(stmt);
    }

    @Override
    public void visit(RaiseInstruction insn) {
        ThrowStatement stmt = new ThrowStatement();
        stmt.setLocation(currentLocation);
        stmt.setException(Expr.var(insn.getException().getIndex()));
        statements.add(stmt);
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        assign(Expr.createArray(insn.getItemType(), Expr.var(insn.getSize().getIndex())), insn.getReceiver());
    }

    @Override
    public void visit(ConstructInstruction insn) {
        assign(Expr.createObject(insn.getType()), insn.getReceiver());
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        Expr[] dimensionExprs = new Expr[insn.getDimensions().size()];
        for (int i = 0; i < dimensionExprs.length; ++i) {
            dimensionExprs[i] = Expr.var(insn.getDimensions().get(i).getIndex());
        }
        assign(Expr.createArray(insn.getItemType(), dimensionExprs), insn.getReceiver());
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        if (insn.getInstance() != null) {
            AssignmentStatement stmt = Statement.assign(Expr.var(insn.getReceiver().getIndex()),
                    Expr.qualify(Expr.var(insn.getInstance().getIndex()), insn.getField()));
            stmt.setLocation(currentLocation);
            statements.add(stmt);
        } else {
            Expr fieldExpr = Expr.qualify(null, insn.getField());
            AssignmentStatement stmt = Statement.assign(Expr.var(insn.getReceiver().getIndex()), fieldExpr);
            stmt.setLocation(currentLocation);
            statements.add(stmt);
        }
    }

    @Override
    public void visit(PutFieldInstruction insn) {
        Expr right = Expr.var(insn.getValue().getIndex());
        Expr left;
        if (insn.getInstance() != null) {
            left = Expr.qualify(Expr.var(insn.getInstance().getIndex()), insn.getField());
        } else {
            left = Expr.qualify(null, insn.getField());
        }
        AssignmentStatement stmt = Statement.assign(left, right);
        stmt.setLocation(currentLocation);
        statements.add(stmt);
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        assign(Expr.unary(UnaryOperation.LENGTH, Expr.var(insn.getArray().getIndex())), insn.getReceiver());
    }

    @Override
    public void visit(UnwrapArrayInstruction insn) {
        UnwrapArrayExpr unwrapExpr = new UnwrapArrayExpr(insn.getElementType());
        unwrapExpr.setArray(Expr.var(insn.getArray().getIndex()));
        assign(unwrapExpr, insn.getReceiver());
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        MethodDescriptor cloneMethodDesc = new MethodDescriptor("clone", ValueType.object("java.lang.Object"));
        MethodReference cloneMethod = new MethodReference("java.lang.Object", cloneMethodDesc);
        assign(Expr.invoke(cloneMethod, Expr.var(insn.getArray().getIndex()), new Expr[0]), insn.getReceiver());
    }

    @Override
    public void visit(GetElementInstruction insn) {
        assign(Expr.subscript(Expr.var(insn.getArray().getIndex()), Expr.var(insn.getIndex().getIndex())),
                insn.getReceiver());
    }

    @Override
    public void visit(PutElementInstruction insn) {
        AssignmentStatement stmt = Statement.assign(Expr.subscript(Expr.var(insn.getArray().getIndex()),
                Expr.var(insn.getIndex().getIndex())), Expr.var(insn.getValue().getIndex()));
        stmt.setLocation(currentLocation);
        statements.add(stmt);
    }

    @Override
    public void visit(InvokeInstruction insn) {
        Expr[] exprArgs = new Expr[insn.getMethod().getParameterTypes().length];
        for (int i = 0; i < insn.getArguments().size(); ++i) {
            exprArgs[i] = Expr.var(insn.getArguments().get(i).getIndex());
        }
        InvocationExpr invocationExpr;
        if (insn.getInstance() != null) {
            if (insn.getType() == InvocationType.VIRTUAL) {
                invocationExpr = Expr.invoke(insn.getMethod(), Expr.var(insn.getInstance().getIndex()), exprArgs);
            } else {
                invocationExpr = Expr.invokeSpecial(insn.getMethod(),
                        Expr.var(insn.getInstance().getIndex()), exprArgs);
            }
        } else {
            invocationExpr = Expr.invokeStatic(insn.getMethod(), exprArgs);
        }
        AssignmentStatement stmt;
        if (insn.getReceiver() != null) {
            stmt = Statement.assign(Expr.var(insn.getReceiver().getIndex()), invocationExpr);
            stmt.getDebugNames().addAll(insn.getReceiver().getDebugNames());
        } else {
            stmt = Statement.assign(null, invocationExpr);
        }
        stmt.setLocation(currentLocation);
        stmt.setAsync(async);
        async = false;
        statements.add(stmt);
    }

    @Override
    public void visit(InvokeDynamicInstruction insn) {
        // InvokeDynamic should be eliminated at previous phases
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        assign(Expr.instanceOf(Expr.var(insn.getValue().getIndex()), insn.getType()), insn.getReceiver());
    }

    private void assign(Expr source, Variable target) {
        AssignmentStatement stmt = Statement.assign(Expr.var(target.getIndex()), source);
        stmt.setLocation(currentLocation);
        stmt.getDebugNames().addAll(target.getDebugNames());
        statements.add(stmt);
    }

    private Expr castToInteger(Expr value) {
        return Expr.binary(BinaryOperation.BITWISE_OR, value, Expr.constant(0));
    }

    private Expr castLongToInt(Expr value) {
        return Expr.unary(UnaryOperation.LONG_TO_INT, value);
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

    private void binary(int first, int second, Variable result, BinaryOperation op) {
        assign(Expr.binary(op, Expr.var(first), Expr.var(second)), result);
    }

    private void intBinary(int first, int second, Variable result, BinaryOperation op) {
        assign(castToInteger(Expr.binary(op, Expr.var(first), Expr.var(second))), result);
    }

    Statement generateJumpStatement(BasicBlock target) {
        if (nextBlock == target && blockMap[target.getIndex()] == null) {
            return null;
        }
        Decompiler.Block block = blockMap[target.getIndex()];
        if (block == null) {
            throw new IllegalStateException("Could not find block for basic block $" + target.getIndex());
        }
        if (target.getIndex() == indexer.nodeAt(block.end)) {
            BreakStatement breakStmt = new BreakStatement();
            breakStmt.setLocation(currentLocation);
            breakStmt.setTarget(block.statement);
            return breakStmt;
        } else {
            ContinueStatement contStmt = new ContinueStatement();
            contStmt.setLocation(currentLocation);
            contStmt.setTarget(block.statement);
            return contStmt;
        }
    }
    private Statement generateJumpStatement(SwitchStatement stmt, int target) {
        Statement body = generateJumpStatement(program.basicBlockAt(target));
        if (body == null) {
            BreakStatement breakStmt = new BreakStatement();
            breakStmt.setTarget(stmt);
            body = breakStmt;
        }
        return body;
    }

    private void branch(Expr condition, BasicBlock consequentBlock, BasicBlock alternativeBlock) {
        Statement consequent = generateJumpStatement(consequentBlock);
        Statement alternative = generateJumpStatement(alternativeBlock);
        statements.add(Statement.cond(condition,
                consequent != null ? Arrays.asList(consequent) : Collections.emptyList(),
                alternative != null ? Arrays.asList(alternative) : Collections.emptyList()));
    }

    private Expr compare(BinaryOperation op, Variable value) {
        Expr expr = Expr.binary(op, Expr.var(value.getIndex()), Expr.constant(0));
        expr.setLocation(currentLocation);
        return expr;
    }

    @Override
    public void visit(InitClassInstruction insn) {
        InitClassStatement stmt = Statement.initClass(insn.getClassName());
        stmt.setLocation(currentLocation);
        statements.add(stmt);
    }

    @Override
    public void visit(NullCheckInstruction insn) {
        assign(Expr.unary(UnaryOperation.NULL_CHECK, Expr.var(insn.getValue().getIndex())), insn.getReceiver());
    }

    @Override
    public void visit(MonitorEnterInstruction insn) {
        MonitorEnterStatement stmt = new MonitorEnterStatement();
        stmt.setLocation(currentLocation);
        stmt.setObjectRef(Expr.var(insn.getObjectRef().getIndex()));
        async = false;
        statements.add(stmt);
    }

    @Override
    public void visit(MonitorExitInstruction insn) {
        MonitorExitStatement stmt = new MonitorExitStatement();
        stmt.setLocation(currentLocation);
        stmt.setObjectRef(Expr.var(insn.getObjectRef().getIndex()));
        statements.add(stmt);
    }
}
