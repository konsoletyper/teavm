/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.ast.decompilation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.ast.ArrayType;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BinaryOperation;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.OperationType;
import org.teavm.ast.PrimitiveCastExpr;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.UnaryOperation;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.common.GraphIndexer;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolderSource;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.SwitchTableEntry;
import org.teavm.model.instructions.UnwrapArrayInstruction;

class StatementGenerator implements InstructionVisitor {
    private int lastSwitchId;
    final List<Statement> statements = new ArrayList<>();
    GraphIndexer indexer;
    BasicBlock nextBlock;
    BasicBlock currentBlock;
    Decompiler.Block[] blockMap;
    Program program;
    ClassHolderSource classSource;
    private TextLocation currentLocation;
    boolean async;

    void setCurrentLocation(TextLocation currentLocation) {
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
                binary(insn.getOperandType(), first, second, result, BinaryOperation.ADD);
                break;
            case SUBTRACT:
                binary(insn.getOperandType(), first, second, result, BinaryOperation.SUBTRACT);
                break;
            case MULTIPLY:
                binary(insn.getOperandType(), first, second, result, BinaryOperation.MULTIPLY);
                break;
            case DIVIDE:
                binary(insn.getOperandType(), first, second, result, BinaryOperation.DIVIDE);
                break;
            case MODULO:
                binary(insn.getOperandType(), first, second, result, BinaryOperation.MODULO);
                break;
            case COMPARE:
                binary(insn.getOperandType(), first, second, result, BinaryOperation.COMPARE);
                break;
            case AND:
                binary(insn.getOperandType(), first, second, result, BinaryOperation.BITWISE_AND);
                break;
            case OR:
                binary(insn.getOperandType(), first, second, result, BinaryOperation.BITWISE_OR);
                break;
            case XOR:
                binary(insn.getOperandType(), first, second, result, BinaryOperation.BITWISE_XOR);
                break;
            case SHIFT_LEFT:
                binary(insn.getOperandType(), first, second, result, BinaryOperation.LEFT_SHIFT);
                break;
            case SHIFT_RIGHT:
                binary(insn.getOperandType(), first, second, result, BinaryOperation.RIGHT_SHIFT);
                break;
            case SHIFT_RIGHT_UNSIGNED:
                binary(insn.getOperandType(), first, second, result, BinaryOperation.UNSIGNED_RIGHT_SHIFT);
                break;
        }
    }

    @Override
    public void visit(NegateInstruction insn) {
        assign(Expr.unary(UnaryOperation.NEGATE, mapOperandType(insn.getOperandType()),
                Expr.var(insn.getOperand().getIndex())), insn.getReceiver());
    }

    @Override
    public void visit(AssignInstruction insn) {
        AssignmentStatement stmt = Statement.assign(Expr.var(insn.getReceiver().getIndex()),
                Expr.var(insn.getAssignee().getIndex()));
        stmt.setLocation(currentLocation);
        statements.add(stmt);
    }

    @Override
    public void visit(CastInstruction insn) {
        assign(Expr.var(insn.getValue().getIndex()), insn.getReceiver());
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        PrimitiveCastExpr expr = new PrimitiveCastExpr();
        expr.setSource(mapOperandType(insn.getSourceType()));
        expr.setTarget(mapOperandType(insn.getTargetType()));
        expr.setValue(Expr.var(insn.getValue().getIndex()));
        assign(expr, insn.getReceiver());
    }

    @Override
    public void visit(CastIntegerInstruction insn) {
        Expr value = Expr.var(insn.getValue().getIndex());
        switch (insn.getDirection()) {
            case FROM_INTEGER:
                switch (insn.getTargetType()) {
                    case BYTE:
                        value = Expr.unary(UnaryOperation.INT_TO_BYTE, null, value);
                        break;
                    case SHORT:
                        value = Expr.unary(UnaryOperation.INT_TO_SHORT, null, value);
                        break;
                    case CHAR:
                        value = Expr.unary(UnaryOperation.INT_TO_CHAR, null, value);
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
                branch(compare(BinaryOperation.EQUALS, OperationType.INT, insn.getOperand()), insn.getConsequent(),
                        insn.getAlternative());
                break;
            case NOT_EQUAL:
                branch(compare(BinaryOperation.NOT_EQUALS, OperationType.INT, insn.getOperand()), insn.getConsequent(),
                        insn.getAlternative());
                break;
            case GREATER:
                branch(compare(BinaryOperation.GREATER, OperationType.INT, insn.getOperand()), insn.getConsequent(),
                        insn.getAlternative());
                break;
            case GREATER_OR_EQUAL:
                branch(compare(BinaryOperation.GREATER_OR_EQUALS, OperationType.INT, insn.getOperand()),
                        insn.getConsequent(), insn.getAlternative());
                break;
            case LESS:
                branch(compare(BinaryOperation.LESS, OperationType.INT, insn.getOperand()), insn.getConsequent(),
                        insn.getAlternative());
                break;
            case LESS_OR_EQUAL:
                branch(compare(BinaryOperation.LESS_OR_EQUALS, OperationType.INT, insn.getOperand()),
                        insn.getConsequent(), insn.getAlternative());
                break;
            case NOT_NULL:
                branch(Expr.binary(BinaryOperation.NOT_EQUALS, null, Expr.var(insn.getOperand().getIndex()),
                        Expr.constant(null)), insn.getConsequent(), insn.getAlternative());
                break;
            case NULL:
                branch(Expr.binary(BinaryOperation.EQUALS, null, Expr.var(insn.getOperand().getIndex()),
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
                branch(withLocation(Expr.binary(BinaryOperation.EQUALS, OperationType.INT, Expr.var(a), Expr.var(b))),
                        consequent, alternative);
                break;
            case REFERENCE_EQUAL:
                branch(withLocation(Expr.binary(BinaryOperation.EQUALS, null, Expr.var(a), Expr.var(b))),
                        consequent, alternative);
                break;
            case NOT_EQUAL:
                branch(withLocation(Expr.binary(BinaryOperation.NOT_EQUALS, OperationType.INT, Expr.var(a),
                        Expr.var(b))), consequent, alternative);
                break;
            case REFERENCE_NOT_EQUAL:
                branch(withLocation(Expr.binary(BinaryOperation.NOT_EQUALS, null, Expr.var(a), Expr.var(b))),
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
            List<Integer> conditions = switchMap.computeIfAbsent(entry.getTarget().getIndex(), k -> new ArrayList<>());
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
        assign(Expr.unary(UnaryOperation.LENGTH, null, Expr.var(insn.getArray().getIndex())), insn.getReceiver());
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
        Expr subscript = Expr.subscript(Expr.var(insn.getArray().getIndex()), Expr.var(insn.getIndex().getIndex()),
                map(insn.getType()));
        assign(subscript, insn.getReceiver());
    }

    @Override
    public void visit(PutElementInstruction insn) {
        Expr subscript = Expr.subscript(Expr.var(insn.getArray().getIndex()),
                Expr.var(insn.getIndex().getIndex()), map(insn.getType()));
        AssignmentStatement stmt = Statement.assign(subscript, Expr.var(insn.getValue().getIndex()));
        stmt.setLocation(currentLocation);
        statements.add(stmt);
    }

    private static ArrayType map(ArrayElementType type) {
        switch (type) {
            case BYTE:
                return ArrayType.BYTE;
            case SHORT:
                return ArrayType.SHORT;
            case CHAR:
                return ArrayType.SHORT;
            case INT:
                return ArrayType.INT;
            case LONG:
                return ArrayType.LONG;
            case FLOAT:
                return ArrayType.FLOAT;
            case DOUBLE:
                return ArrayType.DOUBLE;
            case OBJECT:
                return ArrayType.OBJECT;
        }
        throw new AssertionError();
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
        statements.add(stmt);
    }

    private void binary(NumericOperandType type, int first, int second, Variable result, BinaryOperation op) {
        assign(Expr.binary(op, mapOperandType(type), Expr.var(first), Expr.var(second)), result);
    }

    private static OperationType mapOperandType(NumericOperandType type) {
        switch (type) {
            case INT:
                return OperationType.INT;
            case LONG:
                return OperationType.LONG;
            case FLOAT:
                return OperationType.FLOAT;
            case DOUBLE:
                return OperationType.DOUBLE;
        }
        throw new IllegalArgumentException(type.toString());
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

    private Expr compare(BinaryOperation op, OperationType type, Variable value) {
        Expr expr = Expr.binary(op, type, Expr.var(value.getIndex()), Expr.constant(0));
        expr.setLocation(currentLocation);
        return expr;
    }

    @Override
    public void visit(InitClassInstruction insn) {
        InitClassStatement stmt = Statement.initClass(insn.getClassName());
        stmt.setLocation(currentLocation);
        stmt.setAsync(async);
        async = false;
        statements.add(stmt);
    }

    @Override
    public void visit(NullCheckInstruction insn) {
        assign(Expr.unary(UnaryOperation.NULL_CHECK, null, Expr.var(insn.getValue().getIndex())), insn.getReceiver());
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
