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
package org.teavm.backend.wasm.generate.common.methods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.ast.ArrayFromDataExpr;
import org.teavm.ast.ArrayType;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BoundCheckExpr;
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
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.WhileStatement;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.generate.CachedExpression;
import org.teavm.backend.wasm.generate.ExpressionCache;
import org.teavm.backend.wasm.generate.TemporaryVariablePool;
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCatch;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmConversion;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmFloatBinary;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmSwitch;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.model.expression.WasmTry;
import org.teavm.backend.wasm.render.WasmTypeInference;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;

public abstract class BaseWasmGenerationVisitor implements StatementVisitor, ExprVisitor {
    private static final MethodReference MONITOR_ENTER_SYNC = new MethodReference(Object.class,
            "monitorEnterSync", Object.class, void.class);
    private static final MethodReference MONITOR_EXIT_SYNC = new MethodReference(Object.class,
            "monitorExitSync", Object.class, void.class);
    private static final MethodReference MONITOR_ENTER = new MethodReference(Object.class,
            "monitorEnter", Object.class, void.class);
    private static final MethodReference MONITOR_EXIT = new MethodReference(Object.class,
            "monitorExit", Object.class, void.class);

    private static final int SWITCH_TABLE_THRESHOLD = 256;
    private BaseWasmGenerationContext context;
    protected final MethodReference currentMethod;
    protected final WasmTypeInference typeInference;
    protected final WasmFunction function;
    private int firstVariable;
    private IdentifiedStatement currentContinueTarget;
    private IdentifiedStatement currentBreakTarget;
    private Map<IdentifiedStatement, WasmBlock> breakTargets = new HashMap<>();
    private Map<IdentifiedStatement, WasmBlock> continueTargets = new HashMap<>();
    private Set<WasmBlock> usedBlocks = new HashSet<>();
    protected final TemporaryVariablePool tempVars;
    protected final ExpressionCache exprCache;

    private boolean async;
    protected WasmExpression result;
    protected List<WasmExpression> resultConsumer;

    public BaseWasmGenerationVisitor(BaseWasmGenerationContext context, MethodReference currentMethod,
            WasmFunction function, int firstVariable, boolean async) {
        this.context = context;
        this.currentMethod = currentMethod;
        this.function = function;
        this.firstVariable = firstVariable;
        tempVars = new TemporaryVariablePool(function);
        exprCache = new ExpressionCache(tempVars);
        typeInference = new WasmTypeInference();
        this.async = async;
    }

    public void generate(Statement statement, List<WasmExpression> target) {
        resultConsumer = target;
        statement.acceptVisitor(this);
        resultConsumer = null;
    }

    protected void accept(Expr expr) {
        expr.acceptVisitor(this);
    }

    protected void accept(Statement statement) {
        statement.acceptVisitor(this);
    }

    @Override
    public void visit(BinaryExpr expr) {
        switch (expr.getOperation()) {
            case ADD:
                generateBinary(WasmIntBinaryOperation.ADD, WasmFloatBinaryOperation.ADD, expr);
                break;
            case SUBTRACT:
                generateBinary(WasmIntBinaryOperation.SUB, WasmFloatBinaryOperation.SUB, expr);
                break;
            case MULTIPLY:
                generateBinary(WasmIntBinaryOperation.MUL, WasmFloatBinaryOperation.MUL, expr);
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
                        var type = convertType(expr.getType());
                        var method = new MethodReference(WasmRuntime.class, "remainder", type, type, type);
                        var call = new WasmCall(context.functions().forStaticMethod(method));

                        accept(expr.getFirstOperand());
                        call.getArguments().add(result);

                        accept(expr.getSecondOperand());
                        call.getArguments().add(result);

                        call.setLocation(expr.getLocation());
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
                var type = convertType(expr.getType());
                var method = new MethodReference(WasmRuntime.class, "compare", type, type, int.class);
                var call = new WasmCall(context.functions().forStaticMethod(method));

                accept(expr.getFirstOperand());
                call.getArguments().add(result);

                accept(expr.getSecondOperand());
                call.getArguments().add(result);

                call.setLocation(expr.getLocation());
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
        accept(expr.getFirstOperand());
        var first = result;
        accept(expr.getSecondOperand());
        var second = result;

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
        result.setLocation(expr.getLocation());
    }

    private void generateBinary(WasmIntBinaryOperation intOp, BinaryExpr expr) {
        accept(expr.getFirstOperand());
        var first = result;
        accept(expr.getSecondOperand());
        var second = result;

        if (expr.getType() == OperationType.LONG) {
            switch (expr.getOperation()) {
                case LEFT_SHIFT:
                case RIGHT_SHIFT:
                case UNSIGNED_RIGHT_SHIFT:
                    second = new WasmConversion(WasmNumType.INT32, WasmNumType.INT64, false, second);
                    break;
                default:
                    break;
            }
        }

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

        result.setLocation(expr.getLocation());
    }

    private Class<?> convertType(OperationType type) {
        switch (type) {
            case INT:
                return int.class;
            case LONG:
                return long.class;
            case FLOAT:
                return float.class;
            case DOUBLE:
                return double.class;
        }
        throw new AssertionError(type.toString());
    }

    private void generateAnd(BinaryExpr expr) {
        var block = new WasmBlock(false);
        block.setType(WasmType.INT32);

        accept(expr.getFirstOperand());
        var branch = new WasmBranch(negate(result), block);
        branch.setResult(new WasmInt32Constant(0));
        branch.setLocation(expr.getLocation());
        branch.getResult().setLocation(expr.getLocation());
        block.getBody().add(new WasmDrop(branch));

        accept(expr.getSecondOperand());
        block.getBody().add(result);

        block.setLocation(expr.getLocation());

        result = block;
    }

    private void generateOr(BinaryExpr expr) {
        var block = new WasmBlock(false);
        block.setType(WasmType.INT32);

        accept(expr.getFirstOperand());
        var branch = new WasmBranch(result, block);
        branch.setResult(new WasmInt32Constant(1));
        branch.setLocation(expr.getLocation());
        branch.getResult().setLocation(expr.getLocation());
        block.getBody().add(new WasmDrop(branch));

        accept(expr.getSecondOperand());
        block.getBody().add(result);

        block.setLocation(expr.getLocation());

        result = block;
    }

    @Override
    public void visit(UnaryExpr expr) {
        switch (expr.getOperation()) {
            case INT_TO_BYTE:
                accept(expr.getOperand());
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                        result, new WasmInt32Constant(24));
                result.setLocation(expr.getLocation());
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_SIGNED,
                        result, new WasmInt32Constant(24));
                result.setLocation(expr.getLocation());
                break;
            case INT_TO_SHORT:
                accept(expr.getOperand());
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                        result, new WasmInt32Constant(16));
                result.setLocation(expr.getLocation());
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_SIGNED,
                        result, new WasmInt32Constant(16));
                result.setLocation(expr.getLocation());
                break;
            case INT_TO_CHAR:
                accept(expr.getOperand());
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                        result, new WasmInt32Constant(16));
                result.setLocation(expr.getLocation());
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_UNSIGNED,
                        result, new WasmInt32Constant(16));
                result.setLocation(expr.getLocation());
                break;
            case LENGTH:
                accept(expr.getOperand());
                result = generateArrayLength(result);
                break;
            case NOT:
                accept(expr.getOperand());
                result = negate(result);
                break;
            case NEGATE:
                accept(expr.getOperand());
                switch (expr.getType()) {
                    case INT:
                        result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB,
                                new WasmInt32Constant(0), result);
                        result.setLocation(expr.getLocation());
                        break;
                    case LONG:
                        result = new WasmIntBinary(WasmIntType.INT64, WasmIntBinaryOperation.SUB,
                                new WasmInt64Constant(0), result);
                        result.setLocation(expr.getLocation());
                        break;
                    case FLOAT:
                        result = new WasmFloatBinary(WasmFloatType.FLOAT32, WasmFloatBinaryOperation.SUB,
                                new WasmFloat32Constant(0), result);
                        result.setLocation(expr.getLocation());
                        break;
                    case DOUBLE:
                        result = new WasmFloatBinary(WasmFloatType.FLOAT64, WasmFloatBinaryOperation.SUB,
                                new WasmFloat64Constant(0), result);
                        result.setLocation(expr.getLocation());
                        break;
                }
                break;
            case NULL_CHECK:
                if (!isManaged()) {
                    expr.getOperand().acceptVisitor(this);
                } else {
                    result = nullCheck(expr.getOperand(), expr.getLocation());
                }
                break;
        }
    }

    protected abstract boolean isManaged();

    protected abstract boolean isManagedCall(MethodReference method);

    private WasmExpression nullCheck(Expr value, TextLocation location) {
        var block = new WasmBlock(false);
        block.setLocation(location);

        accept(value);
        result.acceptVisitor(typeInference);
        block.setType(typeInference.getResult());
        var cachedValue = exprCache.create(result, typeInference.getResult(), location, block.getBody());

        var check = new WasmBranch(cachedValue.expr(), block);
        check.setResult(cachedValue.expr());
        block.getBody().add(new WasmDrop(check));

        var callSiteId = generateCallSiteId(location);
        callSiteId.generateRegister(block.getBody(), location);
        generateThrowNPE(location, block.getBody());
        callSiteId.generateThrow(block.getBody(), location);

        cachedValue.release();
        return block;
    }

    protected abstract void generateThrowNPE(TextLocation location, List<WasmExpression> target);

    protected abstract WasmExpression generateArrayLength(WasmExpression array);

    @Override
    public void visit(AssignmentStatement statement) {
        Expr left = statement.getLeftValue();
        if (left == null) {
            if (statement.getRightValue() instanceof InvocationExpr) {
                var invocation = (InvocationExpr) statement.getRightValue();
                invocation(invocation, resultConsumer, invocation.getMethod().getReturnType() != ValueType.VOID);
            } else {
                accept(statement.getRightValue());
                result.acceptVisitor(typeInference);
                if (typeInference.getResult() != null) {
                    result = new WasmDrop(result);
                    result.setLocation(statement.getLocation());
                }
                resultConsumer.add(result);
                result = null;
            }
        } else if (left instanceof VariableExpr) {
            var varExpr = (VariableExpr) left;
            var local = function.getLocalVariables().get(varExpr.getIndex() - firstVariable);
            accept(statement.getRightValue());
            var setLocal = new WasmSetLocal(local, result);
            setLocal.setLocation(statement.getLocation());
            resultConsumer.add(setLocal);
        } else if (left instanceof QualificationExpr) {
            var lhs = (QualificationExpr) left;
            storeField(lhs.getQualified(), lhs.getField(), statement.getRightValue(), statement.getLocation());
        } else if (left instanceof SubscriptExpr) {
            var lhs = (SubscriptExpr) left;
            storeArrayItem(lhs, statement.getRightValue());
        } else {
            throw new UnsupportedOperationException("This expression is not supported yet");
        }
    }

    protected abstract void storeField(Expr qualified, FieldReference field, Expr value, TextLocation location);

    private void storeArrayItem(SubscriptExpr leftValue, Expr rightValue) {
        leftValue.getArray().acceptVisitor(this);
        var array = result;
        leftValue.getIndex().acceptVisitor(this);
        var index = result;
        rightValue.acceptVisitor(this);
        var value = result;
        resultConsumer.add(storeArrayItem(array, index, value, leftValue.getType()));
    }

    protected abstract WasmExpression storeArrayItem(WasmExpression array, WasmExpression index, WasmExpression value,
            ArrayType type);

    @Override
    public void visit(ConditionalExpr expr) {
        accept(expr.getCondition());
        var conditional = new WasmConditional(forCondition(result));

        accept(expr.getConsequent());
        conditional.getThenBlock().getBody().add(result);
        result.acceptVisitor(typeInference);
        var thenType = typeInference.getResult();
        conditional.getThenBlock().setType(thenType);

        accept(expr.getAlternative());
        conditional.getElseBlock().getBody().add(result);
        result.acceptVisitor(typeInference);
        var elseType = typeInference.getResult();
        conditional.getElseBlock().setType(elseType);

        assert thenType == elseType;
        conditional.setType(thenType);

        result = conditional;
    }

    @Override
    public void visit(SequentialStatement statement) {
        for (var part : statement.getSequence()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(ConstantExpr expr) {
        if (expr.getValue() == null) {
            result = nullLiteral(expr);
        } else if (expr.getValue() instanceof Integer) {
            result = new WasmInt32Constant((Integer) expr.getValue());
        } else if (expr.getValue() instanceof Long) {
            result = new WasmInt64Constant((Long) expr.getValue());
        } else if (expr.getValue() instanceof Float) {
            result = new WasmFloat32Constant((Float) expr.getValue());
        } else if (expr.getValue() instanceof Double) {
            result = new WasmFloat64Constant((Double) expr.getValue());
        } else if (expr.getValue() instanceof String) {
            result = stringLiteral((String) expr.getValue());
        } else if (expr.getValue() instanceof ValueType) {
            result = classLiteral((ValueType) expr.getValue());
        } else {
            throw new IllegalArgumentException("Constant unsupported: " + expr.getValue());
        }
        result.setLocation(expr.getLocation());
    }

    protected abstract WasmExpression nullLiteral(Expr expr);

    protected abstract WasmExpression stringLiteral(String s);

    protected abstract WasmExpression classLiteral(ValueType type);

    @Override
    public void visit(ConditionalStatement statement) {
        accept(statement.getCondition());
        var conditional = new WasmConditional(forCondition(result));
        visitMany(statement.getConsequent(), conditional.getThenBlock().getBody());
        visitMany(statement.getAlternative(), conditional.getElseBlock().getBody());
        resultConsumer.add(conditional);
    }

    @Override
    public void visit(VariableExpr expr) {
        result = new WasmGetLocal(localVar(expr.getIndex()));
    }

    protected WasmLocal localVar(int index) {
        return function.getLocalVariables().get(index - firstVariable);
    }

    @Override
    public void visit(SwitchStatement statement) {
        int min = statement.getClauses().stream()
                .flatMapToInt(clause -> Arrays.stream(clause.getConditions()))
                .min().orElse(0);
        int max = statement.getClauses().stream()
                .flatMapToInt(clause -> Arrays.stream(clause.getConditions()))
                .max().orElse(0);

        var defaultBlock = new WasmBlock(false);
        breakTargets.put(statement, defaultBlock);
        var oldBreakTarget = currentBreakTarget;
        currentBreakTarget = statement;

        var wrapper = new WasmBlock(false);
        accept(statement.getValue());
        var condition = result;
        var initialWrapper = wrapper;

        var clauses = statement.getClauses();
        var targets = new WasmBlock[clauses.size()];
        for (int i = 0; i < clauses.size(); i++) {
            var clause = clauses.get(i);
            var caseBlock = new WasmBlock(false);
            caseBlock.getBody().add(wrapper);
            targets[i] = wrapper;

            visitMany(clause.getBody(), caseBlock.getBody());
            wrapper = caseBlock;
        }

        defaultBlock.getBody().add(wrapper);
        visitMany(statement.getDefaultClause(), defaultBlock.getBody());
        var defaultTarget = wrapper;
        wrapper = defaultBlock;

        if ((long) max - (long) min >= SWITCH_TABLE_THRESHOLD) {
            translateSwitchToBinarySearch(statement, condition, initialWrapper, defaultTarget, targets);
        } else {
            translateSwitchToWasmSwitch(statement, condition, initialWrapper, defaultTarget, targets, min, max);
        }

        breakTargets.remove(statement);
        currentBreakTarget = oldBreakTarget;

        resultConsumer.add(wrapper);
    }

    private void translateSwitchToBinarySearch(SwitchStatement statement, WasmExpression condition,
            WasmBlock initialWrapper, WasmBlock defaultTarget, WasmBlock[] targets) {
        var entries = new ArrayList<TableEntry>();
        for (int i = 0; i < statement.getClauses().size(); i++) {
            var clause = statement.getClauses().get(i);
            for (int label : clause.getConditions()) {
                entries.add(new TableEntry(label, targets[i]));
            }
        }
        entries.sort(Comparator.comparingInt(entry -> entry.label));

        var cachedCondition = exprCache.create(condition, WasmType.INT32, statement.getValue().getLocation(),
                initialWrapper.getBody());

        generateBinarySearch(entries, 0, entries.size() - 1, initialWrapper, defaultTarget, cachedCondition);

        cachedCondition.release();
    }

    private void generateBinarySearch(List<TableEntry> entries, int lower, int upper, WasmBlock consumer,
            WasmBlock defaultTarget, CachedExpression testValue) {
        if (upper - lower == 0) {
            int label = entries.get(lower).label;
            var condition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ,
                    testValue.expr(), new WasmInt32Constant(label));
            var conditional = new WasmConditional(condition);
            consumer.getBody().add(conditional);

            conditional.getThenBlock().getBody().add(new WasmBreak(entries.get(lower).target));
            conditional.getElseBlock().getBody().add(new WasmBreak(defaultTarget));
        } else if (upper - lower <= 0) {
            consumer.getBody().add(new WasmBreak(defaultTarget));
        } else {
            int mid = (upper + lower) / 2;
            int label = entries.get(mid).label;
            var condition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED,
                    testValue.expr(), new WasmInt32Constant(label));
            var conditional = new WasmConditional(condition);
            consumer.getBody().add(conditional);

            generateBinarySearch(entries, mid + 1, upper, conditional.getThenBlock(), defaultTarget, testValue);
            generateBinarySearch(entries, lower, mid, conditional.getElseBlock(), defaultTarget, testValue);
        }
    }

    static class TableEntry {
        final int label;
        final WasmBlock target;

        TableEntry(int label, WasmBlock target) {
            this.label = label;
            this.target = target;
        }
    }

    private void translateSwitchToWasmSwitch(SwitchStatement statement, WasmExpression condition,
            WasmBlock initialWrapper, WasmBlock defaultTarget, WasmBlock[] targets, int min, int max) {
        if (min != 0) {
            condition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB, condition,
                    new WasmInt32Constant(min));
        }

        var wasmSwitch = new WasmSwitch(condition, initialWrapper);
        initialWrapper.getBody().add(wasmSwitch);
        wasmSwitch.setDefaultTarget(defaultTarget);

        var expandedTargets = new WasmBlock[max - min + 1];
        for (int i = 0; i < statement.getClauses().size(); i++) {
            var clause = statement.getClauses().get(i);
            for (int label : clause.getConditions()) {
                expandedTargets[label - min] = targets[i];
            }
        }

        for (var target : expandedTargets) {
            wasmSwitch.getTargets().add(target != null ? target : wasmSwitch.getDefaultTarget());
        }
    }

    @Override
    public void visit(WhileStatement statement) {
        var wrapper = new WasmBlock(false);
        var loop = new WasmBlock(true);

        continueTargets.put(statement, loop);
        breakTargets.put(statement, wrapper);
        var oldBreakTarget = currentBreakTarget;
        var oldContinueTarget = currentContinueTarget;
        currentBreakTarget = statement;
        currentContinueTarget = statement;

        if (statement.getCondition() != null) {
            accept(statement.getCondition());
            loop.getBody().add(new WasmBranch(negate(result), wrapper));
            usedBlocks.add(wrapper);
        }

        visitMany(statement.getBody(), loop.getBody());
        loop.getBody().add(new WasmBreak(loop));

        currentBreakTarget = oldBreakTarget;
        currentContinueTarget = oldContinueTarget;
        continueTargets.remove(statement);
        breakTargets.remove(statement);

        if (usedBlocks.contains(wrapper)) {
            wrapper.getBody().add(loop);
            resultConsumer.add(wrapper);
        } else {
            resultConsumer.add(loop);
        }
    }

    protected WasmExpression invocation(InvocationExpr expr, List<WasmExpression> resultConsumer, boolean willDrop) {
        var callSiteId = generateCallSiteId(expr.getLocation());
        if (needsCallSiteId() && isManagedCall(expr.getMethod())) {
            var invocation = generateInvocation(expr, callSiteId);
            var type = mapType(expr.getMethod().getReturnType());

            List<WasmExpression> targetList;
            WasmBlock block;
            if (resultConsumer != null) {
                targetList = resultConsumer;
                result = null;
                block = null;
            } else {
                block = new WasmBlock(false);
                block.setType(type);
                block.setLocation(expr.getLocation());
                targetList = block.getBody();
                result = block;
            }

            if (expr.getArguments().isEmpty()) {
                callSiteId.generateRegister(targetList, expr.getLocation());
            }

            WasmLocal resultVar = null;
            if (!willDrop) {
                if (type != null) {
                    resultVar = tempVars.acquire(type);
                    var setLocal = new WasmSetLocal(resultVar, invocation);
                    setLocal.setLocation(expr.getLocation());
                    targetList.add(setLocal);
                } else {
                    targetList.add(invocation);
                }
            } else if (type == null) {
                targetList.add(invocation);
            } else {
                var drop = new WasmDrop(invocation);
                drop.setLocation(expr.getLocation());
                targetList.add(drop);
            }

            callSiteId.checkHandlerId(targetList, expr.getLocation());
            if (resultVar != null) {
                var getLocal = new WasmGetLocal(resultVar);
                getLocal.setLocation(expr.getLocation());
                targetList.add(getLocal);
                tempVars.release(resultVar);
            }

            return block;
        } else {
            var resultExpr = generateInvocation(expr, null);
            return trivialInvocation(resultExpr, resultConsumer, expr.getLocation(), willDrop);
        }
    }

    private WasmExpression trivialInvocation(WasmExpression resultExpr, List<WasmExpression> resultConsumer,
            TextLocation location, boolean willDrop) {
        if (resultConsumer != null) {
            if (willDrop) {
                var drop = new WasmDrop(resultExpr);
                drop.setLocation(location);
                resultConsumer.add(drop);
            } else {
                resultConsumer.add(resultExpr);
            }
            result = null;
            return null;
        } else {
            return resultExpr;
        }
    }

    protected abstract CallSiteIdentifier generateCallSiteId(TextLocation location);

    protected void acceptWithType(Expr expr, ValueType type) {
        accept(expr);
    }

    protected WasmExpression generateInvocation(InvocationExpr expr, CallSiteIdentifier callSiteId) {
        if (expr.getType() == InvocationType.STATIC || expr.getType() == InvocationType.SPECIAL) {
            var method = context.classes().resolve(expr.getMethod());
            var reference = method != null ? method.getReference() : expr.getMethod();
            var function = expr.getType() == InvocationType.STATIC
                    ? context.functions().forStaticMethod(reference)
                    : context.functions().forInstanceMethod(reference);

            var call = new WasmCall(function);
            var arguments = expr.getArguments();
            for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
                var argument = arguments.get(i);
                var type = expr.getType() == InvocationType.STATIC
                        ? reference.parameterType(i)
                        : i == 0 ? ValueType.object(reference.getClassName()) : reference.parameterType(i - 1);
                acceptWithType(argument, type);
                call.getArguments().add(result);
            }
            if (expr.getType() == InvocationType.SPECIAL) {
                var firstArg = call.getArguments().get(0);
                call.getArguments().set(0, mapFirstArgumentForCall(firstArg, function, expr.getMethod()));
            }
            if (callSiteId != null) {
                callSiteId.addToLastArg(call.getArguments());
            }
            call.setLocation(expr.getLocation());
            return call;
        } else if (expr.getType() == InvocationType.CONSTRUCTOR) {
            var block = new WasmBlock(false);
            var resultType = mapType(ValueType.object(expr.getMethod().getClassName()));
            block.setType(resultType);

            var tmp = tempVars.acquire(resultType);
            allocateObject(expr.getMethod().getClassName(), expr.getLocation(), tmp, block.getBody());

            var function = context.functions().forInstanceMethod(expr.getMethod());
            var call = new WasmCall(function);
            call.getArguments().add(new WasmGetLocal(tmp));
            var arguments = expr.getArguments();
            for (int i = 0; i < arguments.size(); i++) {
                var argument = arguments.get(i);
                acceptWithType(argument, expr.getMethod().parameterType(i));
                call.getArguments().add(result);
            }
            if (callSiteId != null) {
                callSiteId.addToLastArg(call.getArguments());
            }
            block.getBody().add(call);

            block.getBody().add(new WasmGetLocal(tmp));
            tempVars.release(tmp);

            return block;
        } else {
            var reference = expr.getMethod();
            var instanceType = ValueType.object(expr.getMethod().getClassName());
            acceptWithType(expr.getArguments().get(0), instanceType);
            var instanceWasmType = mapType(instanceType);
            var instance = result;
            var block = new WasmBlock(false);
            block.setType(mapType(reference.getReturnType()));

            var instanceVar = tempVars.acquire(instanceWasmType);
            block.getBody().add(new WasmSetLocal(instanceVar, instance));
            instance = new WasmGetLocal(instanceVar);

            var arguments = new ArrayList<WasmExpression>();
            arguments.add(instance);
            for (int i = 1; i < expr.getArguments().size(); ++i) {
                acceptWithType(expr.getArguments().get(i), expr.getMethod().parameterType(i - 1));
                arguments.add(result);
            }
            if (callSiteId != null) {
                callSiteId.addToLastArg(arguments);
            }
            var call = generateVirtualCall(instanceVar, reference, arguments);

            block.getBody().add(call);
            tempVars.release(instanceVar);
            return block;
        }
    }

    protected WasmExpression mapFirstArgumentForCall(WasmExpression argument, WasmFunction function,
            MethodReference method) {
        return argument;
    }

    protected abstract WasmExpression generateVirtualCall(
            WasmLocal instance,
            MethodReference method,
            List<WasmExpression> arguments
    );

    private boolean needsCallSiteId() {
        return isManaged();
    }

    @Override
    public void visit(BlockStatement statement) {
        var block = new WasmBlock(false);

        if (statement.getId() != null) {
            breakTargets.put(statement, block);
        }

        visitMany(statement.getBody(), block.getBody());

        if (statement.getId() != null) {
            breakTargets.remove(statement);
        }

        resultConsumer.add(block);
    }


    @Override
    public void visit(BreakStatement statement) {
        var target = statement.getTarget();
        if (target == null) {
            target = currentBreakTarget;
        }
        var wasmTarget = breakTargets.get(target);
        usedBlocks.add(wasmTarget);
        var br = new WasmBreak(wasmTarget);
        br.setLocation(statement.getLocation());
        resultConsumer.add(br);
    }

    @Override
    public void visit(ContinueStatement statement) {
        var target = statement.getTarget();
        if (target == null) {
            target = currentContinueTarget;
        }
        var wasmTarget = continueTargets.get(target);
        usedBlocks.add(wasmTarget);
        var br = new WasmBreak(wasmTarget);
        br.setLocation(statement.getLocation());
        resultConsumer.add(br);
    }

    @Override
    public void visit(NewExpr expr) {
        var block = new WasmBlock(false);
        block.setLocation(expr.getLocation());
        block.setType(mapType(ValueType.object(expr.getConstructedClass())));
        var callSiteId = generateCallSiteId(expr.getLocation());
        callSiteId.generateRegister(block.getBody(), expr.getLocation());
        allocateObject(expr.getConstructedClass(), expr.getLocation(), null, block.getBody());
        if (block.getBody().size() == 1) {
            result = block.getBody().get(0);
        } else {
            result = block;
        }
    }

    protected abstract void allocateObject(String className, TextLocation location, WasmLocal local,
            List<WasmExpression> target);

    @Override
    public void visit(NewArrayExpr expr) {
        var block = new WasmBlock(false);
        block.setType(mapType(ValueType.arrayOf(expr.getType())));

        var callSiteId = generateCallSiteId(expr.getLocation());
        callSiteId.generateRegister(block.getBody(), expr.getLocation());

        accept(expr.getLength());
        var length = result;
        allocateArray(expr.getType(), length, expr.getLocation(), null, block.getBody());

        if (block.getBody().size() == 1) {
            result = block.getBody().get(0);
        } else {
            result = block;
        }
    }

    protected abstract void allocateArray(ValueType itemType, WasmExpression length, TextLocation location,
            WasmLocal local, List<WasmExpression> target);

    protected abstract WasmExpression allocateMultiArray(List<WasmExpression> target, ValueType itemType,
            List<WasmExpression> dimensions, TextLocation location);

    @Override
    public void visit(ArrayFromDataExpr expr) {
        var type = expr.getType();

        var arrayType = ArrayType.OBJECT;
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    arrayType = ArrayType.BYTE;
                    break;
                case SHORT:
                    arrayType = ArrayType.SHORT;
                    break;
                case CHARACTER:
                    arrayType = ArrayType.CHAR;
                    break;
                case INTEGER:
                    arrayType = ArrayType.INT;
                    break;
                case LONG:
                    arrayType = ArrayType.LONG;
                    break;
                case FLOAT:
                    arrayType = ArrayType.FLOAT;
                    break;
                case DOUBLE:
                    arrayType = ArrayType.DOUBLE;
                    break;
            }
        }

        var wasmArrayType = mapType(ValueType.arrayOf(expr.getType()));
        var block = new WasmBlock(false);
        block.setType(wasmArrayType);
        var callSiteId = generateCallSiteId(expr.getLocation());
        callSiteId.generateRegister(block.getBody(), expr.getLocation());

        var array = tempVars.acquire(wasmArrayType);
        allocateArray(expr.getType(), new WasmInt32Constant(expr.getData().size()), expr.getLocation(), array,
                block.getBody());

        for (int i = 0; i < expr.getData().size(); ++i) {
            expr.getData().get(i).acceptVisitor(this);
            var arrayData = unwrapArray(new WasmGetLocal(array));
            block.getBody().add(storeArrayItem(arrayData, new WasmInt32Constant(i), result, arrayType));
        }

        block.getBody().add(new WasmGetLocal(array));
        block.setLocation(expr.getLocation());
        tempVars.release(array);

        result = block;
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        var block = new WasmBlock(false);

        var callSiteId = generateCallSiteId(expr.getLocation());
        callSiteId.generateRegister(block.getBody(), expr.getLocation());

        var wasmDimensions = new ArrayList<WasmExpression>();
        var arrayType = expr.getType();
        for (var dimension : expr.getDimensions()) {
            accept(dimension);
            wasmDimensions.add(result);
            arrayType = ValueType.arrayOf(arrayType);
        }
        block.setType(mapType(arrayType));
        var call = allocateMultiArray(block.getBody(), expr.getType(), wasmDimensions, expr.getLocation());
        block.getBody().add(call);

        if (block.getBody().size() == 1) {
            result = block.getBody().get(0);
        } else {
            result = block;
        }
    }

    @Override
    public void visit(ReturnStatement statement) {
        if (statement.getResult() != null) {
            acceptWithType(statement.getResult(), currentMethod.getReturnType());
            result = forceType(result, currentMethod.getReturnType());
        } else {
            result = null;
        }
        var wasmStatement = new WasmReturn(result);
        wasmStatement.setLocation(statement.getLocation());
        resultConsumer.add(wasmStatement);
    }

    protected WasmExpression forceType(WasmExpression expression, ValueType type) {
        return expression;
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        acceptWithType(expr.getExpr(), expr.getType());

        var block = new WasmBlock(false);
        block.setType(WasmType.INT32);
        block.setLocation(expr.getLocation());

        result.acceptVisitor(typeInference);
        var cachedObject = exprCache.create(result, typeInference.getResult(), expr.getLocation(), block.getBody());

        var ifNull = new WasmBranch(genIsZero(cachedObject.expr()), block);
        ifNull.setResult(new WasmInt32Constant(0));
        block.getBody().add(new WasmDrop(ifNull));

        block.getBody().add(generateInstanceOf(cachedObject.expr(), expr.getType()));

        cachedObject.release();

        result = block;
    }

    protected abstract WasmExpression generateInstanceOf(WasmExpression expression, ValueType type);

    @Override
    public void visit(ThrowStatement statement) {
        var callSiteId = generateCallSiteId(statement.getLocation());
        callSiteId.generateRegister(resultConsumer, statement.getLocation());

        accept(statement.getException());
        generateThrow(result, statement.getLocation(), resultConsumer);
        callSiteId.generateThrow(resultConsumer, statement.getLocation());
    }

    protected abstract void generateThrow(WasmExpression expression, TextLocation location,
            List<WasmExpression> target);

    @Override
    public void visit(CastExpr expr) {
        var block = new WasmBlock(false);
        var wasmTargetType = mapType(expr.getTarget());
        block.setType(wasmTargetType);
        block.setLocation(expr.getLocation());

        acceptWithType(expr.getValue(), expr.getTarget());
        result.acceptVisitor(typeInference);
        var wasmSourceType = typeInference.getResult();
        var valueToCast = exprCache.create(result, wasmSourceType, expr.getLocation(), block.getBody());

        var nullCheck = new WasmBranch(genIsZero(valueToCast.expr()), block);
        nullCheck.setResult(valueToCast.expr());
        block.getBody().add(new WasmDrop(nullCheck));

        var supertypeCall = generateInstanceOf(valueToCast.expr(), expr.getTarget());

        var breakIfPassed = new WasmBranch(supertypeCall, block);
        breakIfPassed.setResult(valueToCast.expr());
        block.getBody().add(new WasmDrop(breakIfPassed));

        var callSiteId = generateCallSiteId(expr.getLocation());
        callSiteId.generateRegister(block.getBody(), expr.getLocation());
        generateThrowCCE(expr.getLocation(), block.getBody());
        callSiteId.generateThrow(block.getBody(), expr.getLocation());

        valueToCast.release();
        result = generateCast(block, wasmTargetType);
    }

    protected abstract WasmExpression generateCast(WasmExpression value, WasmType targetType);

    @Override
    public void visit(InitClassStatement statement) {
        if (needsClassInitializer(statement.getClassName())) {
            var callSiteId = generateCallSiteId(statement.getLocation());
            callSiteId.generateRegister(resultConsumer, statement.getLocation());
            resultConsumer.add(generateClassInitializer(statement.getClassName(), statement.getLocation()));
            callSiteId.checkHandlerId(resultConsumer, statement.getLocation());
        }
    }

    protected abstract boolean needsClassInitializer(String className);

    protected abstract WasmExpression generateClassInitializer(String className, TextLocation location);

    @Override
    public void visit(PrimitiveCastExpr expr) {
        accept(expr.getValue());
        result = new WasmConversion(WasmGeneratorUtil.mapType(expr.getSource()),
                WasmGeneratorUtil.mapType(expr.getTarget()), true, result);
        result.setLocation(expr.getLocation());
    }

    @Override
    public void visit(TryCatchStatement statement) {
        var tryCatchStatements = new ArrayList<TryCatchStatement>();
        while (true) {
            if (statement.getProtectedBody().size() != 1) {
                break;
            }
            var next = statement.getProtectedBody().get(0);
            if (!(next instanceof TryCatchStatement)) {
                break;
            }
            tryCatchStatements.add(statement);
            statement = (TryCatchStatement) next;
        }
        tryCatchStatements.add(statement);

        generateTry(tryCatchStatements, statement.getProtectedBody());
    }

    protected void generateTry(List<TryCatchStatement> tryCatchStatements, List<Statement> protectedBody) {
        var innerCatchBlock = new WasmBlock(false);

        var catchBlocks = new ArrayList<WasmBlock>();
        for (int i = 0; i < tryCatchStatements.size(); ++i) {
            catchBlocks.add(new WasmBlock(false));
        }
        var outerCatchBlock = catchBlocks.get(0);

        var tryBlock = new WasmTry();
        visitMany(protectedBody, tryBlock.getBody());
        if (!tryBlock.isTerminating()) {
            tryBlock.getBody().add(new WasmBreak(outerCatchBlock));
        }
        var catchClause = new WasmCatch(context.getExceptionTag());
        tryBlock.getCatches().add(catchClause);
        innerCatchBlock.getBody().add(tryBlock);

        var throwableType = mapType(ValueType.object("java.lang.Throwable"));
        var obj = exprCache.create(peekException(), throwableType, null, innerCatchBlock.getBody());
        var currentBlock = innerCatchBlock;
        boolean catchesAll = false;
        for (int i = tryCatchStatements.size() - 1; i >= 0; --i) {
            var tryCatch = tryCatchStatements.get(i);
            var catchBlock = catchBlocks.get(i);
            if (tryCatch.getExceptionType() != null && !tryCatch.getExceptionType().equals(Throwable.class.getName())) {
                var exceptionType = ValueType.object(tryCatch.getExceptionType());
                var isMatched = generateInstanceOf(obj.expr(), exceptionType);
                innerCatchBlock.getBody().add(new WasmBranch(isMatched, currentBlock));
            } else {
                innerCatchBlock.getBody().add(new WasmBreak(currentBlock));
                catchesAll = true;
            }
            currentBlock = catchBlock;
        }
        if (!catchesAll) {
            innerCatchBlock.getBody().add(new WasmThrow(context.getExceptionTag()));
        }
        obj.release();

        currentBlock = innerCatchBlock;
        for (int i = tryCatchStatements.size() - 1; i >= 0; --i) {
            var tryCatch = tryCatchStatements.get(i);
            var catchBlock = catchBlocks.get(i);
            catchBlock.getBody().add(currentBlock);

            var catchLocal = tryCatch.getExceptionVariable() != null
                    ? localVar(tryCatch.getExceptionVariable())
                    : null;
            catchException(null, catchBlock.getBody(), catchLocal);
            visitMany(tryCatch.getHandler(), catchBlock.getBody());
            if (!catchBlock.isTerminating() && catchBlock != outerCatchBlock) {
                catchBlock.getBody().add(new WasmBreak(outerCatchBlock));
            }
            currentBlock = catchBlock;
        }

        resultConsumer.add(outerCatchBlock);
    }

    protected abstract WasmExpression peekException();

    protected abstract void catchException(TextLocation location, List<WasmExpression> target, WasmLocal local);

    private void visitMany(List<Statement> statements, List<WasmExpression> target) {
        var oldTarget = resultConsumer;
        resultConsumer = target;
        for (var part : statements) {
            accept(part);
        }
        resultConsumer = oldTarget;
    }

    @Override
    public void visit(GotoPartStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        var call = new WasmCall(context.functions().forStaticMethod(async ? MONITOR_ENTER : MONITOR_ENTER_SYNC));
        call.setLocation(statement.getLocation());
        statement.getObjectRef().acceptVisitor(this);
        call.getArguments().add(result);

        var callSiteId = generateCallSiteId(statement.getLocation());
        callSiteId.generateRegister(resultConsumer, statement.getLocation());
        resultConsumer.add(call);
        callSiteId.checkHandlerId(resultConsumer, statement.getLocation());
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        var call = new WasmCall(context.functions().forStaticMethod(async ? MONITOR_EXIT : MONITOR_EXIT_SYNC));
        call.setLocation(statement.getLocation());
        statement.getObjectRef().acceptVisitor(this);
        call.getArguments().add(result);

        var callSiteId = generateCallSiteId(statement.getLocation());
        callSiteId.generateRegister(resultConsumer, statement.getLocation());
        resultConsumer.add(call);
        callSiteId.checkHandlerId(resultConsumer, statement.getLocation());
    }

    @Override
    public void visit(BoundCheckExpr expr) {
        if (!isManaged()) {
            expr.getIndex().acceptVisitor(this);
            return;
        }

        var block = new WasmBlock(false);
        block.setType(WasmType.INT32);
        block.setLocation(expr.getLocation());

        accept(expr.getIndex());
        var index = exprCache.create(result, WasmType.INT32, expr.getLocation(), block.getBody());

        if (expr.getArray() != null) {
            var condBlock = block;
            if (expr.isLower()) {
                condBlock = new WasmBlock(false);
                block.getBody().add(condBlock);

                var lowerCond = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                        index.expr(), new WasmInt32Constant(0));
                var lowerBranch = new WasmBranch(lowerCond, condBlock);
                condBlock.getBody().add(lowerBranch);
            }

            accept(expr.getArray());
            var upperBound = generateArrayLength(result);
            var upperCond = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                    index.expr(), upperBound);
            var upperBranch = new WasmBranch(upperCond, block);
            upperBranch.setResult(index.expr());
            condBlock.getBody().add(new WasmDrop(upperBranch));
        } else if (expr.isLower()) {
            var lowerCond = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GE_SIGNED,
                    index.expr(), new WasmInt32Constant(0));
            var lowerBranch = new WasmBranch(lowerCond, block);
            lowerBranch.setResult(index.expr());
            block.getBody().add(new WasmDrop(lowerBranch));
        }

        var callSiteId = generateCallSiteId(expr.getLocation());
        callSiteId.generateRegister(block.getBody(), expr.getLocation());
        generateThrowAIOOBE(expr.getLocation(), block.getBody());
        callSiteId.generateThrow(block.getBody(), expr.getLocation());

        result = block;
    }

    protected abstract void generateThrowAIOOBE(TextLocation location, List<WasmExpression> target);

    protected abstract void generateThrowCCE(TextLocation location, List<WasmExpression> target);

    private static WasmExpression negate(WasmExpression expr) {
        if (expr instanceof WasmIntBinary) {
            var binary = (WasmIntBinary) expr;
            if (binary.getType() == WasmIntType.INT32 && binary.getOperation() == WasmIntBinaryOperation.XOR) {
                if (isOne(binary.getFirst())) {
                    var result = binary.getSecond();
                    if (result.getLocation() == null && expr.getLocation() != null) {
                        result.setLocation(expr.getLocation());
                    }
                    return result;
                }
                if (isOne(binary.getSecond())) {
                    var result = binary.getFirst();
                    if (result.getLocation() == null && expr.getLocation() != null) {
                        result.setLocation(expr.getLocation());
                    }
                    return result;
                }
            }

            var negatedOp = negate(binary.getOperation());
            if (negatedOp != null) {
                var result = new WasmIntBinary(binary.getType(), negatedOp, binary.getFirst(), binary.getSecond());
                result.setLocation(expr.getLocation());
                return result;
            }
        } else if (expr instanceof WasmFloatBinary) {
            var binary = (WasmFloatBinary) expr;
            var negatedOp = negate(binary.getOperation());
            if (negatedOp != null) {
                var result = new WasmFloatBinary(binary.getType(), negatedOp, binary.getFirst(), binary.getSecond());
                result.setLocation(expr.getLocation());
                return result;
            }
        }

        var result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ, expr, new WasmInt32Constant(0));
        result.setLocation(expr.getLocation());
        return result;
    }

    private static boolean isOne(WasmExpression expression) {
        return expression instanceof WasmInt32Constant && ((WasmInt32Constant) expression).getValue() == 1;
    }

    private static boolean isZero(WasmExpression expression) {
        return expression instanceof WasmInt32Constant && ((WasmInt32Constant) expression).getValue() == 0;
    }

    private boolean isBoolean(WasmExpression expression) {
        if (expression instanceof WasmIntBinary) {
            WasmIntBinary binary = (WasmIntBinary) expression;
            switch (binary.getOperation()) {
                case EQ:
                case NE:
                case LT_SIGNED:
                case LT_UNSIGNED:
                case LE_SIGNED:
                case LE_UNSIGNED:
                case GT_SIGNED:
                case GT_UNSIGNED:
                case GE_SIGNED:
                case GE_UNSIGNED:
                    return true;
                default:
                    return false;
            }
        } else if (expression instanceof WasmFloatBinary) {
            WasmFloatBinary binary = (WasmFloatBinary) expression;
            switch (binary.getOperation()) {
                case EQ:
                case NE:
                case LT:
                case LE:
                case GT:
                case GE:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    private WasmExpression forCondition(WasmExpression expression) {
        if (expression instanceof WasmIntBinary) {
            WasmIntBinary binary = (WasmIntBinary) expression;
            switch (binary.getOperation()) {
                case EQ:
                    if (isZero(binary.getFirst()) && isBoolean(binary.getSecond())) {
                        return negate(binary.getSecond());
                    } else if (isZero(binary.getSecond()) && isBoolean(binary.getFirst())) {
                        return negate(binary.getFirst());
                    }
                    break;
                case NE:
                    if (isZero(binary.getFirst()) && isBoolean(binary.getSecond())) {
                        return binary.getSecond();
                    } else if (isZero(binary.getSecond()) && isBoolean(binary.getFirst())) {
                        return binary.getFirst();
                    }
                    break;
                default:
                    break;
            }
        }
        return expression;
    }

    private static WasmIntBinaryOperation negate(WasmIntBinaryOperation op) {
        switch (op) {
            case EQ:
                return WasmIntBinaryOperation.NE;
            case NE:
                return WasmIntBinaryOperation.EQ;
            case LT_SIGNED:
                return WasmIntBinaryOperation.GE_SIGNED;
            case LT_UNSIGNED:
                return WasmIntBinaryOperation.GE_UNSIGNED;
            case LE_SIGNED:
                return WasmIntBinaryOperation.GT_SIGNED;
            case LE_UNSIGNED:
                return WasmIntBinaryOperation.GT_UNSIGNED;
            case GT_SIGNED:
                return WasmIntBinaryOperation.LE_SIGNED;
            case GT_UNSIGNED:
                return WasmIntBinaryOperation.LE_UNSIGNED;
            case GE_SIGNED:
                return WasmIntBinaryOperation.LT_SIGNED;
            case GE_UNSIGNED:
                return WasmIntBinaryOperation.LT_UNSIGNED;
            default:
                return null;
        }
    }

    private static WasmFloatBinaryOperation negate(WasmFloatBinaryOperation op) {
        switch (op) {
            case EQ:
                return WasmFloatBinaryOperation.NE;
            case NE:
                return WasmFloatBinaryOperation.EQ;
            case LT:
                return WasmFloatBinaryOperation.GE;
            case LE:
                return WasmFloatBinaryOperation.GT;
            case GT:
                return WasmFloatBinaryOperation.LE;
            case GE:
                return WasmFloatBinaryOperation.LT;
            default:
                return null;
        }
    }

    private WasmExpression genIsZero(WasmExpression value) {
        return new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ, value);
    }

    protected abstract WasmType mapType(ValueType type);

    protected WasmExpression unwrapArray(WasmExpression array) {
        return array;
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        accept(expr.getArray());
        result = unwrapArray(result);
        result.setLocation(expr.getLocation());
    }

    protected abstract class CallSiteIdentifier {
        public abstract void generateRegister(List<WasmExpression> consumer, TextLocation location);

        public final void addToLastArg(List<WasmExpression> args) {
            if (args.isEmpty()) {
                return;
            }
            var arg = args.get(args.size() - 1);
            var block = new WasmBlock(false);
            arg.acceptVisitor(typeInference);
            block.setType(typeInference.getResult());
            block.setLocation(arg.getLocation());
            block.getBody().add(arg);
            args.set(args.size() - 1, block);
            generateRegister(block.getBody(), arg.getLocation());
        }

        public abstract void checkHandlerId(List<WasmExpression> target, TextLocation location);

        public abstract void generateThrow(List<WasmExpression> target, TextLocation location);
    }
}
