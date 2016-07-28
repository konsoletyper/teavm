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
package org.teavm.wasm.generate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.CastExpr;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.ExprVisitor;
import org.teavm.ast.GotoPartStatement;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.InvocationType;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.NewExpr;
import org.teavm.ast.NewMultiArrayExpr;
import org.teavm.ast.OperationType;
import org.teavm.ast.PrimitiveCastExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.SequentialStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.StatementVisitor;
import org.teavm.ast.SubscriptExpr;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.WhileStatement;
import org.teavm.wasm.model.WasmFunction;
import org.teavm.wasm.model.WasmLocal;
import org.teavm.wasm.model.expression.WasmBlock;
import org.teavm.wasm.model.expression.WasmBranch;
import org.teavm.wasm.model.expression.WasmBreak;
import org.teavm.wasm.model.expression.WasmCall;
import org.teavm.wasm.model.expression.WasmConditional;
import org.teavm.wasm.model.expression.WasmConversion;
import org.teavm.wasm.model.expression.WasmDrop;
import org.teavm.wasm.model.expression.WasmExpression;
import org.teavm.wasm.model.expression.WasmFloat32Constant;
import org.teavm.wasm.model.expression.WasmFloat64Constant;
import org.teavm.wasm.model.expression.WasmFloatBinary;
import org.teavm.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.wasm.model.expression.WasmFloatType;
import org.teavm.wasm.model.expression.WasmGetLocal;
import org.teavm.wasm.model.expression.WasmInt32Constant;
import org.teavm.wasm.model.expression.WasmInt64Constant;
import org.teavm.wasm.model.expression.WasmIntBinary;
import org.teavm.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.wasm.model.expression.WasmIntType;
import org.teavm.wasm.model.expression.WasmReturn;
import org.teavm.wasm.model.expression.WasmSetLocal;
import org.teavm.wasm.model.expression.WasmSwitch;

class WasmGenerationVisitor implements StatementVisitor, ExprVisitor {
    private int firstVariable;
    private WasmFunction function;
    private IdentifiedStatement currentContinueTarget;
    private IdentifiedStatement currentBreakTarget;
    private Map<IdentifiedStatement, WasmBlock> breakTargets = new HashMap<>();
    private Map<IdentifiedStatement, WasmBlock> continueTargets = new HashMap<>();
    private Set<WasmBlock> usedBlocks = new HashSet<>();
    WasmExpression result;

    public WasmGenerationVisitor(WasmFunction function, int firstVariable) {
        this.function = function;
        this.firstVariable = firstVariable;
    }

    @Override
    public void visit(BinaryExpr expr) {
        switch (expr.getOperation()) {
            case ADD:
                generateBinary(WasmIntBinaryOperation.ADD, WasmFloatBinaryOperation.ADD, expr);
                break;
            case SUBTRACT:
                generateBinary(WasmIntBinaryOperation.SUB, WasmFloatBinaryOperation.ADD, expr);
                break;
            case MULTIPLY:
                generateBinary(WasmIntBinaryOperation.MUL, WasmFloatBinaryOperation.ADD, expr);
                break;
            case DIVIDE:
                generateBinary(WasmIntBinaryOperation.DIV_SIGNED, WasmFloatBinaryOperation.DIV, expr);
                break;
            case MODULO: {
                switch (expr.getType()) {
                    case INT:
                    case LONG:
                        generateBinary(WasmIntBinaryOperation.REM_SIGNED, expr);
                        break;
                    default:
                        WasmCall call = new WasmCall("rt$remainder." + typeAsString(expr.getType()), false);
                        expr.getFirstOperand().acceptVisitor(this);
                        call.getArguments().add(result);
                        expr.getSecondOperand().acceptVisitor(this);
                        call.getArguments().add(result);
                        result = call;
                        break;
                }

                break;
            }
            case BITWISE_AND:
                generateBinary(WasmIntBinaryOperation.AND, expr);
                break;
            case BITWISE_OR:
                generateBinary(WasmIntBinaryOperation.OR, expr);
                break;
            case BITWISE_XOR:
                generateBinary(WasmIntBinaryOperation.XOR, expr);
                break;
            case EQUALS:
                generateBinary(WasmIntBinaryOperation.EQ, WasmFloatBinaryOperation.EQ, expr);
                break;
            case NOT_EQUALS:
                generateBinary(WasmIntBinaryOperation.NE, WasmFloatBinaryOperation.NE, expr);
                break;
            case GREATER:
                generateBinary(WasmIntBinaryOperation.GT_SIGNED, WasmFloatBinaryOperation.GT, expr);
                break;
            case GREATER_OR_EQUALS:
                generateBinary(WasmIntBinaryOperation.GE_SIGNED, WasmFloatBinaryOperation.GE, expr);
                break;
            case LESS:
                generateBinary(WasmIntBinaryOperation.LT_SIGNED, WasmFloatBinaryOperation.LT, expr);
                break;
            case LESS_OR_EQUALS:
                generateBinary(WasmIntBinaryOperation.LE_SIGNED, WasmFloatBinaryOperation.LE, expr);
                break;
            case LEFT_SHIFT:
                generateBinary(WasmIntBinaryOperation.SHL, expr);
                break;
            case RIGHT_SHIFT:
                generateBinary(WasmIntBinaryOperation.SHR_SIGNED, expr);
                break;
            case UNSIGNED_RIGHT_SHIFT:
                generateBinary(WasmIntBinaryOperation.SHR_UNSIGNED, expr);
                break;
            case COMPARE: {
                WasmCall call = new WasmCall("rt$compare." + typeAsString(expr.getType()), false);
                expr.getFirstOperand().acceptVisitor(this);
                call.getArguments().add(result);
                expr.getSecondOperand().acceptVisitor(this);
                call.getArguments().add(result);
                result = call;
                break;
            }
            case AND:
                generateAnd(expr);
                break;
            case OR:
                generateOr(expr);
                break;
        }
    }

    private void generateBinary(WasmIntBinaryOperation intOp, WasmFloatBinaryOperation floatOp, BinaryExpr expr) {
        expr.getFirstOperand().acceptVisitor(this);
        WasmExpression first = result;
        expr.getSecondOperand().acceptVisitor(this);
        WasmExpression second = result;

        if (expr.getType() == null) {
            result = new WasmIntBinary(WasmIntType.INT32, intOp, first, second);
        } else {
            switch (expr.getType()) {
                case INT:
                    result = new WasmIntBinary(WasmIntType.INT32, intOp, first, second);
                    break;
                case LONG:
                    result = new WasmIntBinary(WasmIntType.INT64, intOp, first, second);
                    break;
                case FLOAT:
                    result = new WasmFloatBinary(WasmFloatType.FLOAT32, floatOp, first, second);
                    break;
                case DOUBLE:
                    result = new WasmFloatBinary(WasmFloatType.FLOAT64, floatOp, first, second);
                    break;
            }
        }
    }

    private void generateBinary(WasmIntBinaryOperation intOp, BinaryExpr expr) {
        expr.getFirstOperand().acceptVisitor(this);
        WasmExpression first = result;
        expr.getSecondOperand().acceptVisitor(this);
        WasmExpression second = result;

        switch (expr.getType()) {
            case INT:
                result = new WasmIntBinary(WasmIntType.INT32, intOp, first, second);
                break;
            case LONG:
                result = new WasmIntBinary(WasmIntType.INT64, intOp, first, second);
                break;
            case FLOAT:
            case DOUBLE:
                throw new AssertionError("Can't translate operation " + intOp + " for type " + expr.getType());
        }
    }

    private String typeAsString(OperationType type) {
        switch (type) {
            case INT:
                return "i32";
            case LONG:
                return "i64";
            case FLOAT:
                return "float";
            case DOUBLE:
                return "double";
        }
        throw new AssertionError(type.toString());
    }

    private void generateAnd(BinaryExpr expr) {
        WasmBlock block = new WasmBlock(false);

        expr.getFirstOperand().acceptVisitor(this);
        WasmBranch branch = new WasmBranch(negate(result), block);
        branch.setResult(new WasmInt32Constant(0));
        block.getBody().add(branch);

        expr.getSecondOperand().acceptVisitor(this);
        block.getBody().add(result);

        result = block;
    }

    private void generateOr(BinaryExpr expr) {
        WasmBlock block = new WasmBlock(false);

        expr.getFirstOperand().acceptVisitor(this);
        WasmBranch branch = new WasmBranch(result, block);
        branch.setResult(new WasmInt32Constant(1));
        block.getBody().add(branch);

        expr.getSecondOperand().acceptVisitor(this);
        block.getBody().add(result);

        result = block;
    }

    @Override
    public void visit(UnaryExpr expr) {
        switch (expr.getOperation()) {
            case INT_TO_BYTE:
                expr.getOperand().acceptVisitor(this);
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                        result, new WasmInt32Constant(24));
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_SIGNED,
                        result, new WasmInt32Constant(24));
                break;
            case INT_TO_SHORT:
                expr.getOperand().acceptVisitor(this);
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                        result, new WasmInt32Constant(16));
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_SIGNED,
                        result, new WasmInt32Constant(16));
                break;
            case INT_TO_CHAR:
                expr.getOperand().acceptVisitor(this);
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                        result, new WasmInt32Constant(16));
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_UNSIGNED,
                        result, new WasmInt32Constant(16));
                break;
            case LENGTH:
                result = new WasmInt32Constant(0);
                break;
            case NOT:
                expr.getOperand().acceptVisitor(this);
                result = negate(result);
                break;
            case NEGATE:
                expr.getOperand().acceptVisitor(this);
                switch (expr.getType()) {
                    case INT:
                        result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB,
                                new WasmInt32Constant(0), result);
                        break;
                    case LONG:
                        result = new WasmIntBinary(WasmIntType.INT64, WasmIntBinaryOperation.SUB,
                                new WasmInt64Constant(0), result);
                        break;
                    case FLOAT:
                        result = new WasmFloatBinary(WasmFloatType.FLOAT32, WasmFloatBinaryOperation.SUB,
                                new WasmFloat32Constant(0), result);
                        break;
                    case DOUBLE:
                        result = new WasmFloatBinary(WasmFloatType.FLOAT64, WasmFloatBinaryOperation.SUB,
                                new WasmFloat64Constant(0), result);
                        break;
                }
                break;
            case NULL_CHECK:
                expr.getOperand().acceptVisitor(this);
                break;
        }
    }

    @Override
    public void visit(AssignmentStatement statement) {
        Expr left = statement.getLeftValue();
        if (left == null) {
            statement.getRightValue().acceptVisitor(this);
            result = new WasmDrop(result);
        } else if (left instanceof VariableExpr) {
            VariableExpr varExpr = (VariableExpr) left;
            WasmLocal local = function.getLocalVariables().get(varExpr.getIndex() - firstVariable);
            statement.getRightValue().acceptVisitor(this);
            result = new WasmSetLocal(local, result);
        } else {
            throw new UnsupportedOperationException("This expression is not supported yet");
        }
    }

    @Override
    public void visit(ConditionalExpr expr) {
        expr.getCondition().acceptVisitor(this);
        WasmConditional conditional = new WasmConditional(result);
        expr.getConsequent().acceptVisitor(this);
        conditional.getThenBlock().getBody().add(result);
        expr.getAlternative().acceptVisitor(this);
        conditional.getThenBlock().getBody().add(result);
        result = conditional;
    }

    @Override
    public void visit(SequentialStatement statement) {
        WasmBlock block = new WasmBlock(false);
        for (Statement part : statement.getSequence()) {
            part.acceptVisitor(this);
            block.getBody().add(result);
        }
        result = block;
    }

    @Override
    public void visit(ConstantExpr expr) {
        if (expr.getValue() == null) {
            result = new WasmInt32Constant(0);
        } else if (expr.getValue() instanceof Integer) {
            result = new WasmInt32Constant((Integer) expr.getValue());
        } else if (expr.getValue() instanceof Long) {
            result = new WasmInt64Constant((Long) expr.getValue());
        } else if (expr.getValue() instanceof Float) {
            result = new WasmFloat32Constant((Float) expr.getValue());
        } else if (expr.getValue() instanceof Double) {
            result = new WasmFloat64Constant((Double) expr.getValue());
        } else {
            throw new IllegalArgumentException("Constant unsupported: " + expr.getValue());
        }
    }

    @Override
    public void visit(ConditionalStatement statement) {
        statement.getCondition().acceptVisitor(this);
        WasmConditional conditional = new WasmConditional(result);
        for (Statement part : statement.getConsequent()) {
            part.acceptVisitor(this);
            conditional.getThenBlock().getBody().add(result);
        }
        for (Statement part : statement.getAlternative()) {
            part.acceptVisitor(this);
            conditional.getElseBlock().getBody().add(result);
        }
        result = conditional;
    }

    @Override
    public void visit(VariableExpr expr) {
        result = new WasmGetLocal(function.getLocalVariables().get(expr.getIndex() - firstVariable));
    }

    @Override
    public void visit(SubscriptExpr expr) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public void visit(SwitchStatement statement) {
        List<WasmBlock> wrappers = new ArrayList<>();

        WasmBlock wrapper = new WasmBlock(false);
        statement.getValue().acceptVisitor(this);
        WasmSwitch wasmSwitch = new WasmSwitch(result, wrapper);
        wrapper.getBody().add(wasmSwitch);

        WasmBlock defaultBlock = new WasmBlock(false);
        defaultBlock.getBody().add(wrapper);
        for (Statement part : statement.getDefaultClause()) {
            part.acceptVisitor(this);
            defaultBlock.getBody().add(result);
        }
        wrapper = defaultBlock;

        for (SwitchClause clause : statement.getClauses()) {
            WasmBlock caseBlock = new WasmBlock(false);
            caseBlock.getBody().add(wrapper);
            wasmSwitch.getTargets().add(wrapper);
            for (Statement part : clause.getBody()) {
                part.acceptVisitor(this);
                caseBlock.getBody().add(result);
            }
            wrappers.add(caseBlock);
            wrapper = caseBlock;
        }

        for (WasmBlock nestedWrapper : wrappers) {
            nestedWrapper.getBody().add(new WasmBreak(wrapper));
        }

        result = wrapper;
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        expr.getArray().acceptVisitor(this);
    }

    @Override
    public void visit(WhileStatement statement) {
        WasmBlock wrapper = new WasmBlock(false);
        WasmBlock loop = new WasmBlock(true);

        continueTargets.put(statement, loop);
        breakTargets.put(statement, wrapper);
        IdentifiedStatement oldBreakTarget = currentBreakTarget;
        IdentifiedStatement oldContinueTarget = currentContinueTarget;
        currentBreakTarget = statement;
        currentContinueTarget = statement;

        if (statement.getCondition() != null) {
            statement.getCondition().acceptVisitor(this);
            loop.getBody().add(new WasmBranch(result, wrapper));
            usedBlocks.add(wrapper);
        }

        for (Statement part : statement.getBody()) {
            part.acceptVisitor(this);
            loop.getBody().add(result);
        }

        currentBreakTarget = oldBreakTarget;
        currentContinueTarget = oldContinueTarget;
        continueTargets.remove(statement);
        breakTargets.remove(statement);

        if (usedBlocks.contains(wrapper)) {
            wrapper.getBody().add(loop);
            result = wrapper;
        } else {
            result = loop;
        }
    }

    @Override
    public void visit(InvocationExpr expr) {
        if (expr.getType() == InvocationType.STATIC || expr.getType() == InvocationType.SPECIAL) {
            String methodName = WasmMangling.mangleMethod(expr.getMethod());
            WasmCall call = new WasmCall(methodName);
            for (Expr argument : expr.getArguments()) {
                argument.acceptVisitor(this);
                call.getArguments().add(result);
            }
            result = call;
        }
    }

    @Override
    public void visit(BlockStatement statement) {
        WasmBlock block = new WasmBlock(false);

        if (statement.getId() != null) {
            breakTargets.put(statement, block);
        }

        for (Statement part : statement.getBody()) {
            part.acceptVisitor(this);
            block.getBody().add(result);
        }

        if (statement.getId() != null) {
            breakTargets.remove(statement);
        }

        result = block;
    }

    @Override
    public void visit(QualificationExpr expr) {
    }

    @Override
    public void visit(BreakStatement statement) {
        IdentifiedStatement target = statement.getTarget();
        if (target == null) {
            target = currentBreakTarget;
        }
        WasmBlock wasmTarget = breakTargets.get(target);
        usedBlocks.add(wasmTarget);
        result = new WasmBreak(wasmTarget);
    }

    @Override
    public void visit(NewExpr expr) {
    }

    @Override
    public void visit(ContinueStatement statement) {
        IdentifiedStatement target = statement.getTarget();
        if (target == null) {
            target = currentContinueTarget;
        }
        WasmBlock wasmTarget = continueTargets.get(target);
        usedBlocks.add(wasmTarget);
        result = new WasmBreak(wasmTarget);
    }

    @Override
    public void visit(NewArrayExpr expr) {
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
    }

    @Override
    public void visit(ReturnStatement statement) {
        if (statement.getResult() != null) {
            statement.getResult().acceptVisitor(this);
        } else {
            result = null;
        }
        result = new WasmReturn(result);
    }

    @Override
    public void visit(InstanceOfExpr expr) {

    }

    @Override
    public void visit(ThrowStatement statement) {

    }

    @Override
    public void visit(CastExpr expr) {
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(InitClassStatement statement) {
    }

    @Override
    public void visit(PrimitiveCastExpr expr) {
        expr.getValue().acceptVisitor(this);
        result = new WasmConversion(WasmGeneratorUtil.mapType(expr.getSource()),
                WasmGeneratorUtil.mapType(expr.getTarget()), true, result);

    }

    @Override
    public void visit(TryCatchStatement statement) {
    }

    @Override
    public void visit(GotoPartStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
    }

    @Override
    public void visit(MonitorExitStatement statement) {
    }

    private WasmExpression negate(WasmExpression expr) {
        if (expr instanceof WasmIntBinary) {
            WasmIntBinary binary = (WasmIntBinary) expr;
            if (binary.getType() == WasmIntType.INT32 && binary.getOperation() == WasmIntBinaryOperation.XOR) {
                if (isOne(binary.getFirst())) {
                    return binary.getSecond();
                }
                if (isOne(binary.getSecond())) {
                    return binary.getFirst();
                }
            }
        }
        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.XOR, expr, new WasmInt32Constant(1));
    }

    private boolean isOne(WasmExpression expression) {
        return expression instanceof WasmInt32Constant && ((WasmInt32Constant) expression).getValue() == 1;
    }
}
