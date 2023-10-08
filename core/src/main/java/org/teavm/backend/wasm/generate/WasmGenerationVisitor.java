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
package org.teavm.backend.wasm.generate;

import static org.teavm.model.lowlevel.ExceptionHandlingUtil.isManagedMethodCall;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.WhileStatement;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.backend.wasm.WasmHeap;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
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
import org.teavm.backend.wasm.model.expression.WasmIndirectCall;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat32;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat64;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmLoadInt64;
import org.teavm.backend.wasm.model.expression.WasmMemoryAccess;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat32;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat64;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.model.expression.WasmStoreInt64;
import org.teavm.backend.wasm.model.expression.WasmSwitch;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.backend.wasm.render.WasmTypeInference;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Address;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.classes.VirtualTable;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.CallSiteLocation;
import org.teavm.model.lowlevel.ExceptionHandlerDescriptor;
import org.teavm.model.lowlevel.ExceptionHandlingUtil;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.ExceptionHandling;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.ShadowStack;

class WasmGenerationVisitor implements StatementVisitor, ExprVisitor {
    private static final FieldReference MONITOR_FIELD = new FieldReference("java.lang.Object", "monitor");
    private static final MethodReference MONITOR_ENTER_SYNC = new MethodReference(Object.class,
            "monitorEnterSync", Object.class, void.class);
    private static final MethodReference MONITOR_EXIT_SYNC = new MethodReference(Object.class,
            "monitorExitSync", Object.class, void.class);
    private static final MethodReference MONITOR_ENTER = new MethodReference(Object.class,
            "monitorEnter", Object.class, void.class);
    private static final MethodReference MONITOR_EXIT = new MethodReference(Object.class,
            "monitorExit", Object.class, void.class);
    private static final MethodReference CATCH_METHOD = new MethodReference(ExceptionHandling.class,
            "catchException", Throwable.class);
    private static final MethodReference THROW_METHOD = new MethodReference(ExceptionHandling.class,
            "throwException", Throwable.class, void.class);
    private static final MethodReference THROW_CCE_METHOD = new MethodReference(ExceptionHandling.class,
            "throwClassCastException", void.class);
    private static final MethodReference THROW_NPE_METHOD = new MethodReference(ExceptionHandling.class,
            "throwNullPointerException", void.class);
    private static final MethodReference THROW_AIOOBE_METHOD = new MethodReference(ExceptionHandling.class,
            "throwArrayIndexOutOfBoundsException", void.class);

    private static final int SWITCH_TABLE_THRESHOLD = 256;
    private WasmGenerationContext context;
    private WasmClassGenerator classGenerator;
    private WasmTypeInference typeInference;
    private WasmFunction function;
    private MethodReference currentMethod;
    private int firstVariable;
    private IdentifiedStatement currentContinueTarget;
    private IdentifiedStatement currentBreakTarget;
    private Map<IdentifiedStatement, WasmBlock> breakTargets = new HashMap<>();
    private Map<IdentifiedStatement, WasmBlock> continueTargets = new HashMap<>();
    private Set<WasmBlock> usedBlocks = new HashSet<>();
    private TemporaryVariablePool tempVars;
    private ExpressionCache exprCache;

    private List<ExceptionHandlerDescriptor> handlers = new ArrayList<>();
    private WasmBlock lastTryBlock;
    private WasmBlock rethrowBlock;
    private List<WasmBlock> catchLabels = new ArrayList<>();

    private WasmLocal stackVariable;
    private BinaryWriter binaryWriter;
    private boolean async;
    private boolean managed;
    WasmExpression result;
    List<WasmExpression> resultConsumer;

    WasmGenerationVisitor(WasmGenerationContext context, WasmClassGenerator classGenerator,
            BinaryWriter binaryWriter, WasmFunction function, MethodReference currentMethod,
            int firstVariable, boolean async) {
        this.context = context;
        this.classGenerator = classGenerator;
        this.binaryWriter = binaryWriter;
        this.function = function;
        this.currentMethod = currentMethod;
        this.firstVariable = firstVariable;
        tempVars = new TemporaryVariablePool(function);
        exprCache = new ExpressionCache(tempVars);
        typeInference = new WasmTypeInference(context);
        this.async = async;
        this.managed = context.characteristics.isManaged(currentMethod);
    }

    void generate(Statement statement, List<WasmExpression> target) {
        var lastTargetSize = target.size();
        resultConsumer = target;
        statement.acceptVisitor(this);
        resultConsumer = null;
        if (rethrowBlock != null) {
            var body = target.subList(lastTargetSize, target.size());
            rethrowBlock.getBody().addAll(body);
            body.clear();
            target.add(rethrowBlock);
            var valueToReturn = WasmExpression.defaultValueOfType(function.getResult());
            if (valueToReturn != null) {
                target.add(new WasmReturn(valueToReturn));
            }
            if (!rethrowBlock.isTerminating()) {
                rethrowBlock.getBody().add(new WasmReturn());
            }
        }
    }

    private void accept(Expr expr) {
        expr.acceptVisitor(this);
    }

    private void accept(Statement statement) {
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
                        Class<?> type = convertType(expr.getType());
                        MethodReference method = new MethodReference(WasmRuntime.class, "remainder", type, type, type);
                        WasmCall call = new WasmCall(context.names.forMethod(method), false);

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
                Class<?> type = convertType(expr.getType());
                MethodReference method = new MethodReference(WasmRuntime.class, "compare", type, type, int.class);
                WasmCall call = new WasmCall(context.names.forMethod(method), false);

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
        WasmExpression first = result;
        accept(expr.getSecondOperand());
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
        result.setLocation(expr.getLocation());
    }

    private void generateBinary(WasmIntBinaryOperation intOp, BinaryExpr expr) {
        accept(expr.getFirstOperand());
        WasmExpression first = result;
        accept(expr.getSecondOperand());
        WasmExpression second = result;

        if (expr.getType() == OperationType.LONG) {
            switch (expr.getOperation()) {
                case LEFT_SHIFT:
                case RIGHT_SHIFT:
                case UNSIGNED_RIGHT_SHIFT:
                    second = new WasmConversion(WasmType.INT32, WasmType.INT64, false, second);
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
        WasmBlock block = new WasmBlock(false);
        block.setType(WasmType.INT32);

        accept(expr.getFirstOperand());
        WasmBranch branch = new WasmBranch(negate(result), block);
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
        WasmBlock block = new WasmBlock(false);
        block.setType(WasmType.INT32);

        accept(expr.getFirstOperand());
        WasmBranch branch = new WasmBranch(result, block);
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
                if (!managed) {
                    expr.getOperand().acceptVisitor(this);
                } else {
                    result = nullCheck(expr.getOperand(), expr.getLocation());
                }
                break;
        }
    }

    private WasmExpression nullCheck(Expr value, TextLocation location) {
        var block = new WasmBlock(false);
        block.setType(WasmType.INT32);
        block.setLocation(location);


        accept(value);
        var cachedValue = exprCache.create(result, WasmType.INT32, location, block.getBody());

        var check = new WasmBranch(cachedValue.expr(), block);
        check.setResult(cachedValue.expr());
        block.getBody().add(new WasmDrop(check));

        var callSiteId = generateCallSiteId(location);
        block.getBody().add(generateRegisterCallSite(callSiteId, location));

        var call = new WasmCall(context.names.forMethod(THROW_NPE_METHOD));
        block.getBody().add(call);

        var target = throwJumpTarget();
        var breakExpr = new WasmBreak(target);
        if (target != rethrowBlock) {
            breakExpr.setResult(generateGetHandlerId(callSiteId, location));
            block.getBody().add(new WasmDrop(breakExpr));
        } else {
            block.getBody().add(breakExpr);
        }

        cachedValue.release();
        return block;
    }

    private WasmExpression generateArrayLength(WasmExpression array) {
        int sizeOffset = classGenerator.getFieldOffset(new FieldReference(RuntimeArray.class.getName(), "size"));
        var length = new WasmLoadInt32(4, array, WasmInt32Subtype.INT32);
        length.setOffset(sizeOffset);
        length.setLocation(array.getLocation());
        return length;
    }

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
            VariableExpr varExpr = (VariableExpr) left;
            WasmLocal local = function.getLocalVariables().get(varExpr.getIndex() - firstVariable);
            accept(statement.getRightValue());
            var setLocal = new WasmSetLocal(local, result);
            setLocal.setLocation(statement.getLocation());
            resultConsumer.add(setLocal);
        } else if (left instanceof QualificationExpr) {
            QualificationExpr lhs = (QualificationExpr) left;
            storeField(lhs.getQualified(), lhs.getField(), statement.getRightValue(), statement.getLocation());
        } else if (left instanceof SubscriptExpr) {
            SubscriptExpr lhs = (SubscriptExpr) left;
            storeArrayItem(lhs, statement.getRightValue());
        } else {
            throw new UnsupportedOperationException("This expression is not supported yet");
        }
    }

    private void storeField(Expr qualified, FieldReference field, Expr value, TextLocation location) {
        WasmExpression address = getAddress(qualified, field, location);
        accept(value);
        if (field.equals(MONITOR_FIELD)) {
            storeMonitor(address, result, location);
            return;
        }

        ValueType type = context.getFieldType(field);
        WasmMemoryAccess resultExpr;
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    resultExpr = new WasmStoreInt32(1, address, result, WasmInt32Subtype.INT8);
                    break;
                case SHORT:
                    resultExpr = new WasmStoreInt32(2, address, result, WasmInt32Subtype.INT16);
                    break;
                case CHARACTER:
                    resultExpr = new WasmStoreInt32(2, address, result, WasmInt32Subtype.UINT16);
                    break;
                case INTEGER:
                    resultExpr = new WasmStoreInt32(4, address, result, WasmInt32Subtype.INT32);
                    break;
                case LONG:
                    resultExpr = new WasmStoreInt64(8, address, result, WasmInt64Subtype.INT64);
                    break;
                case FLOAT:
                    resultExpr = new WasmStoreFloat32(4, address, result);
                    break;
                case DOUBLE:
                    resultExpr = new WasmStoreFloat64(8, address, result);
                    break;
                default:
                    throw new AssertionError(type.toString());
            }
        } else {
            resultExpr = new WasmStoreInt32(4, address, result, WasmInt32Subtype.INT32);
        }

        resultExpr.setOffset(getOffset(qualified, field));
        var result = (WasmExpression) resultExpr;
        result.setLocation(location);
        resultConsumer.add(result);
    }

    private void storeMonitor(WasmExpression address, WasmExpression value, TextLocation location) {
        value = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_UNSIGNED, value,
                new WasmInt32Constant(1));
        value = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.OR, value,
                new WasmInt32Constant(0x80000000));
        var store = new WasmStoreInt32(4, address, value, WasmInt32Subtype.INT32);
        store.setLocation(location);
        store.setOffset(4);
        resultConsumer.add(store);
    }

    private void storeArrayItem(SubscriptExpr leftValue, Expr rightValue) {
        leftValue.getArray().acceptVisitor(this);
        WasmExpression array = result;
        leftValue.getIndex().acceptVisitor(this);
        WasmExpression index = result;
        rightValue.acceptVisitor(this);
        WasmExpression value = result;
        resultConsumer.add(storeArrayItem(getArrayElementPointer(array, index, leftValue.getType()), value,
                leftValue.getType()));
    }

    private static WasmExpression storeArrayItem(WasmExpression array, WasmExpression value, ArrayType type) {
        switch (type) {
            case BYTE:
                return new WasmStoreInt32(1, array, value, WasmInt32Subtype.INT8);
            case SHORT:
                return new WasmStoreInt32(2, array, value, WasmInt32Subtype.INT16);
            case CHAR:
                return new WasmStoreInt32(2, array, value, WasmInt32Subtype.UINT16);
            case INT:
            case OBJECT:
                return new WasmStoreInt32(4, array, value, WasmInt32Subtype.INT32);
            case LONG:
                return new WasmStoreInt64(8, array, value, WasmInt64Subtype.INT64);
            case FLOAT:
                return new WasmStoreFloat32(4, array, value);
            case DOUBLE:
                return new WasmStoreFloat64(8, array, value);
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void visit(ConditionalExpr expr) {
        accept(expr.getCondition());
        WasmConditional conditional = new WasmConditional(forCondition(result));

        accept(expr.getConsequent());
        conditional.getThenBlock().getBody().add(result);
        result.acceptVisitor(typeInference);
        WasmType thenType = typeInference.getResult();
        conditional.getThenBlock().setType(thenType);

        accept(expr.getAlternative());
        conditional.getElseBlock().getBody().add(result);
        result.acceptVisitor(typeInference);
        WasmType elseType = typeInference.getResult();
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
            result = new WasmInt32Constant(0);
        } else if (expr.getValue() instanceof Integer) {
            result = new WasmInt32Constant((Integer) expr.getValue());
        } else if (expr.getValue() instanceof Long) {
            result = new WasmInt64Constant((Long) expr.getValue());
        } else if (expr.getValue() instanceof Float) {
            result = new WasmFloat32Constant((Float) expr.getValue());
        } else if (expr.getValue() instanceof Double) {
            result = new WasmFloat64Constant((Double) expr.getValue());
        } else if (expr.getValue() instanceof String) {
            String str = (String) expr.getValue();
            result = new WasmInt32Constant(context.getStringPool().getStringPointer(str));
        } else if (expr.getValue() instanceof ValueType) {
            int pointer = classGenerator.getClassPointer((ValueType) expr.getValue());
            result = new WasmInt32Constant(pointer);
        } else {
            throw new IllegalArgumentException("Constant unsupported: " + expr.getValue());
        }
    }

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

    private WasmLocal localVar(int index) {
        return function.getLocalVariables().get(index - firstVariable);
    }

    @Override
    public void visit(SubscriptExpr expr) {
        WasmExpression ptr = getArrayElementPointer(expr);
        switch (expr.getType()) {
            case BYTE:
                result = new WasmLoadInt32(1, ptr, WasmInt32Subtype.INT8);
                break;
            case SHORT:
                result = new WasmLoadInt32(2, ptr, WasmInt32Subtype.INT16);
                break;
            case CHAR:
                result = new WasmLoadInt32(2, ptr, WasmInt32Subtype.UINT16);
                break;
            case INT:
            case OBJECT:
                result = new WasmLoadInt32(4, ptr, WasmInt32Subtype.INT32);
                break;
            case LONG:
                result = new WasmLoadInt64(8, ptr, WasmInt64Subtype.INT64);
                break;
            case FLOAT:
                result = new WasmLoadFloat32(4, ptr);
                break;
            case DOUBLE:
                result = new WasmLoadFloat64(8, ptr);
                break;
        }
    }

    private WasmExpression getArrayElementPointer(SubscriptExpr expr) {
        expr.getArray().acceptVisitor(this);
        WasmExpression array = result;
        expr.getIndex().acceptVisitor(this);
        WasmExpression index = result;
        return getArrayElementPointer(array, index, expr.getType());
    }

    private WasmExpression getArrayElementPointer(WasmExpression array, WasmExpression index, ArrayType type) {
        int size = -1;
        switch (type) {
            case BYTE:
                size = 0;
                break;
            case SHORT:
            case CHAR:
                size = 1;
                break;
            case INT:
            case FLOAT:
            case OBJECT:
                size = 2;
                break;
            case LONG:
            case DOUBLE:
                size = 3;
                break;
        }

        int base = BinaryWriter.align(classGenerator.getClassSize(RuntimeArray.class.getName()), 1 << size);
        array = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, array, new WasmInt32Constant(base));
        if (size != 0) {
            index = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, index,
                    new WasmInt32Constant(size));
        }

        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, array, index);
    }

    @Override
    public void visit(SwitchStatement statement) {
        int min = statement.getClauses().stream()
                .flatMapToInt(clause -> Arrays.stream(clause.getConditions()))
                .min().orElse(0);
        int max = statement.getClauses().stream()
                .flatMapToInt(clause -> Arrays.stream(clause.getConditions()))
                .max().orElse(0);

        WasmBlock defaultBlock = new WasmBlock(false);
        breakTargets.put(statement, defaultBlock);
        IdentifiedStatement oldBreakTarget = currentBreakTarget;
        currentBreakTarget = statement;

        WasmBlock wrapper = new WasmBlock(false);
        accept(statement.getValue());
        WasmExpression condition = result;
        WasmBlock initialWrapper = wrapper;

        List<SwitchClause> clauses = statement.getClauses();
        WasmBlock[] targets = new WasmBlock[clauses.size()];
        for (int i = 0; i < clauses.size(); i++) {
            SwitchClause clause = clauses.get(i);
            WasmBlock caseBlock = new WasmBlock(false);
            caseBlock.getBody().add(wrapper);
            targets[i] = wrapper;

            visitMany(clause.getBody(), caseBlock.getBody());
            wrapper = caseBlock;
        }

        defaultBlock.getBody().add(wrapper);
        visitMany(statement.getDefaultClause(), defaultBlock.getBody());
        WasmBlock defaultTarget = wrapper;
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
        List<TableEntry> entries = new ArrayList<>();
        for (int i = 0; i < statement.getClauses().size(); i++) {
            SwitchClause clause = statement.getClauses().get(i);
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
            WasmConditional conditional = new WasmConditional(condition);
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
            WasmConditional conditional = new WasmConditional(condition);
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

        WasmSwitch wasmSwitch = new WasmSwitch(condition, initialWrapper);
        initialWrapper.getBody().add(wasmSwitch);
        wasmSwitch.setDefaultTarget(defaultTarget);

        WasmBlock[] expandedTargets = new WasmBlock[max - min + 1];
        for (int i = 0; i < statement.getClauses().size(); i++) {
            SwitchClause clause = statement.getClauses().get(i);
            for (int label : clause.getConditions()) {
                expandedTargets[label - min] = targets[i];
            }
        }

        for (WasmBlock target : expandedTargets) {
            wasmSwitch.getTargets().add(target != null ? target : wasmSwitch.getDefaultTarget());
        }
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        accept(expr.getArray());
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

    @Override
    public void visit(InvocationExpr expr) {
        result = invocation(expr, null, false);
    }

    private WasmExpression invocation(InvocationExpr expr, List<WasmExpression> resultConsumer, boolean willDrop) {
        if (expr.getMethod().getClassName().equals(ShadowStack.class.getName())) {
            switch (expr.getMethod().getName()) {
                case "allocStack":
                    generateAllocStack(expr.getArguments().get(0));
                    result.setLocation(expr.getLocation());
                    if (resultConsumer != null) {
                        resultConsumer.add(result);
                        return null;
                    } else {
                        return result;
                    }
                case "releaseStack":
                    generateReleaseStack();
                    result.setLocation(expr.getLocation());
                    if (resultConsumer != null) {
                        resultConsumer.add(result);
                        return null;
                    } else {
                        return result;
                    }
                case "registerGCRoot":
                    generateRegisterGcRoot(expr.getArguments().get(0), expr.getArguments().get(1));
                    result.setLocation(expr.getLocation());
                    if (resultConsumer != null) {
                        resultConsumer.add(result);
                        return null;
                    } else {
                        return result;
                    }
                case "removeGCRoot":
                    generateRemoveGcRoot(expr.getArguments().get(0));
                    result.setLocation(expr.getLocation());
                    if (resultConsumer != null) {
                        resultConsumer.add(result);
                        return null;
                    } else {
                        return result;
                    }
            }
        }

        var intrinsic = context.getIntrinsic(expr.getMethod());
        if (intrinsic != null) {
            var resultExpr = intrinsic.apply(expr, intrinsicManager);
            return trivialInvocation(resultExpr, resultConsumer, expr.getLocation(), willDrop);
        }

        var callSiteId = generateCallSiteId(expr.getLocation());
        if (needsCallSiteId() && isManagedMethodCall(context.characteristics, expr.getMethod())) {
            var invocation = generateInvocation(expr, callSiteId);
            var type = WasmGeneratorUtil.mapType(expr.getMethod().getReturnType());

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
                targetList.add(generateRegisterCallSite(callSiteId, expr.getLocation()));
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

            checkHandlerId(targetList, callSiteId, expr.getLocation());
            if (resultVar != null) {
                var getLocal = new WasmGetLocal(resultVar);
                getLocal.setLocation(expr.getLocation());
                targetList.add(getLocal);
                tempVars.release(resultVar);
            }

            return block;
        } else {
            var resultExpr = generateInvocation(expr, -1);
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

    private int generateCallSiteId(TextLocation location) {
        var callSiteLocations = CallSiteLocation.fromTextLocation(location, currentMethod);
        var callSite = new CallSiteDescriptor(context.callSites.size(), callSiteLocations);
        var reverseHandlers = new ArrayList<>(handlers);
        Collections.reverse(reverseHandlers);
        callSite.getHandlers().addAll(reverseHandlers);
        context.callSites.add(callSite);
        return callSite.getId();
    }

    private void checkHandlerId(List<WasmExpression> target, int callSiteId, TextLocation location) {
        var jumpTarget = throwJumpTarget();
        if (jumpTarget == rethrowBlock) {
            var handlerId = generateGetHandlerId(callSiteId, location);
            var br = new WasmBranch(handlerId, throwJumpTarget());
            target.add(br);
        } else {
            var handlerVar = tempVars.acquire(WasmType.INT32);
            var handlerId = generateGetHandlerId(callSiteId, location);
            var saveHandler = new WasmSetLocal(handlerVar, handlerId);
            saveHandler.setLocation(location);
            target.add(saveHandler);
            var br = new WasmBranch(new WasmGetLocal(handlerVar), throwJumpTarget());
            br.setResult(new WasmGetLocal(handlerVar));
            var dropBr = new WasmDrop(br);
            dropBr.setLocation(location);
            target.add(dropBr);
            tempVars.release(handlerVar);
        }
    }

    private WasmBlock throwJumpTarget() {
        return lastTryBlock != null ? lastTryBlock : rethrowBlock();
    }

    private WasmExpression generateInvocation(InvocationExpr expr, int callSiteId) {
        if (expr.getType() == InvocationType.STATIC || expr.getType() == InvocationType.SPECIAL) {
            MethodReader method = context.getClassSource().resolve(expr.getMethod());
            MethodReference reference = method != null ? method.getReference() : expr.getMethod();
            String methodName = context.names.forMethod(reference);

            WasmCall call = new WasmCall(methodName);
            if (context.getImportedMethod(reference) != null) {
                call.setImported(true);
            }
            for (Expr argument : expr.getArguments()) {
                accept(argument);
                call.getArguments().add(result);
            }
            addCallSiteIdToLastArg(callSiteId, call.getArguments());
            call.setLocation(expr.getLocation());
            return call;
        } else if (expr.getType() == InvocationType.CONSTRUCTOR) {
            WasmBlock block = new WasmBlock(false);
            block.setType(WasmType.INT32);

            WasmLocal tmp = tempVars.acquire(WasmType.INT32);
            if (callSiteId >= 0) {
                generateRegisterCallSite(callSiteId, expr.getLocation());
            }
            block.getBody().add(new WasmSetLocal(tmp, allocateObject(expr.getMethod().getClassName(),
                    expr.getLocation())));

            String methodName = context.names.forMethod(expr.getMethod());
            WasmCall call = new WasmCall(methodName);
            call.getArguments().add(new WasmGetLocal(tmp));
            for (Expr argument : expr.getArguments()) {
                accept(argument);
                call.getArguments().add(result);
            }
            addCallSiteIdToLastArg(callSiteId, call.getArguments());
            block.getBody().add(call);

            block.getBody().add(new WasmGetLocal(tmp));
            tempVars.release(tmp);

            return block;
        } else {
            MethodReference reference = expr.getMethod();
            accept(expr.getArguments().get(0));
            WasmExpression instance = result;
            WasmBlock block = new WasmBlock(false);
            block.setType(WasmGeneratorUtil.mapType(reference.getReturnType()));

            WasmLocal instanceVar = tempVars.acquire(WasmType.INT32);
            block.getBody().add(new WasmSetLocal(instanceVar, instance));
            instance = new WasmGetLocal(instanceVar);

            int vtableOffset = classGenerator.getClassSize(RuntimeClass.class.getName());
            VirtualTable vtable = context.getVirtualTableProvider().lookup(reference.getClassName());
            if (vtable != null) {
                vtable = vtable.findMethodContainer(reference.getDescriptor());
            }
            if (vtable == null) {
                return new WasmUnreachable();
            }
            int vtableIndex = vtable.getMethods().indexOf(reference.getDescriptor());
            if (vtable.getParent() != null) {
                vtableIndex += vtable.getParent().size();
            }
            var classRef = getReferenceToClass(instance);
            var methodIndex = new WasmLoadInt32(4, classRef, WasmInt32Subtype.INT32);
            methodIndex.setOffset(vtableIndex * 4 + vtableOffset);

            WasmIndirectCall call = new WasmIndirectCall(methodIndex);
            call.getParameterTypes().add(WasmType.INT32);
            for (int i = 0; i < expr.getMethod().parameterCount(); ++i) {
                call.getParameterTypes().add(WasmGeneratorUtil.mapType(expr.getMethod().parameterType(i)));
            }
            if (expr.getMethod().getReturnType() != ValueType.VOID) {
                call.setReturnType(WasmGeneratorUtil.mapType(expr.getMethod().getReturnType()));
            }

            call.getArguments().add(instance);
            for (int i = 1; i < expr.getArguments().size(); ++i) {
                accept(expr.getArguments().get(i));
                call.getArguments().add(result);
            }
            addCallSiteIdToLastArg(callSiteId, call.getArguments());

            block.getBody().add(call);
            tempVars.release(instanceVar);
            return block;
        }
    }

    private void addCallSiteIdToLastArg(int callSiteId, List<WasmExpression> args) {
        if (args.isEmpty() || callSiteId < 0) {
            return;
        }
        var arg = args.get(args.size() - 1);
        var block = new WasmBlock(false);
        arg.acceptVisitor(typeInference);
        block.setType(typeInference.getResult());
        block.setLocation(arg.getLocation());
        block.getBody().add(arg);
        args.set(args.size() - 1, block);
        block.getBody().add(generateRegisterCallSite(callSiteId, arg.getLocation()));
    }

    private boolean needsCallSiteId() {
        return managed;
    }

    private void generateAllocStack(Expr sizeExpr) {
        if (stackVariable != null) {
            throw new IllegalStateException("Call to ShadowStack.allocStack must be done only once");
        }
        stackVariable = tempVars.acquire(WasmType.INT32);
        stackVariable.setName("__stack__");
        InvocationExpr expr = new InvocationExpr();
        expr.setType(InvocationType.SPECIAL);
        expr.setMethod(new MethodReference(WasmRuntime.class, "allocStack", int.class, Address.class));
        expr.getArguments().add(sizeExpr);
        expr.acceptVisitor(this);

        result = new WasmSetLocal(stackVariable, result);
    }

    private void generateReleaseStack() {
        if (stackVariable == null) {
            throw new IllegalStateException("Call to ShadowStack.releaseStack must be dominated by "
                    + "Mutator.allocStack");
        }

        int offset = classGenerator.getFieldOffset(new FieldReference(WasmHeap.class.getName(), "stack"));
        WasmExpression oldValue = new WasmGetLocal(stackVariable);
        oldValue = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB, oldValue,
                new WasmInt32Constant(4));
        result = new WasmStoreInt32(4, new WasmInt32Constant(offset), oldValue, WasmInt32Subtype.INT32);
    }

    private WasmExpression generateRegisterCallSite(int callSite, TextLocation location) {
        return generateRegisterCallSite(new WasmInt32Constant(callSite), location);
    }

    private WasmExpression generateRegisterCallSite(WasmExpression callSite, TextLocation location) {
        var result = new WasmStoreInt32(4, new WasmGetLocal(stackVariable), callSite, WasmInt32Subtype.INT32);
        result.setLocation(location);
        return result;
    }

    private WasmExpression generateGetHandlerId(int callSite, TextLocation location) {
        WasmExpression handlerId = new WasmLoadInt32(4, new WasmGetLocal(stackVariable), WasmInt32Subtype.INT32);
        if (callSite > 0) {
            handlerId = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB, handlerId,
                    new WasmInt32Constant(callSite));
        }
        handlerId.setLocation(location);
        return handlerId;
    }

    private void generateRegisterGcRoot(Expr slotExpr, Expr gcRootExpr) {
        if (stackVariable == null) {
            throw new IllegalStateException("Call to ShadowStack.registerGCRoot must be dominated by "
                    + "Mutator.allocStack");
        }

        slotExpr.acceptVisitor(this);
        WasmExpression slotOffset = getSlotOffset(result);
        WasmExpression address = new WasmGetLocal(stackVariable);
        if (!(slotOffset instanceof WasmInt32Constant)) {
            address = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, address, slotOffset);
        }

        gcRootExpr.acceptVisitor(this);
        WasmExpression gcRoot = result;

        WasmStoreInt32 store = new WasmStoreInt32(4, address, gcRoot, WasmInt32Subtype.INT32);
        if (slotOffset instanceof WasmInt32Constant) {
            store.setOffset(((WasmInt32Constant) slotOffset).getValue());
        }
        result = store;
    }

    private void generateRemoveGcRoot(Expr slotExpr) {
        if (stackVariable == null) {
            throw new IllegalStateException("Call to ShadowStack.removeGCRoot must be dominated by "
                    + "Mutator.allocStack");
        }

        slotExpr.acceptVisitor(this);
        WasmExpression slotOffset = getSlotOffset(result);
        WasmExpression address = new WasmGetLocal(stackVariable);
        if (!(slotOffset instanceof WasmInt32Constant)) {
            address = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, address, slotOffset);
        }

        WasmStoreInt32 store = new WasmStoreInt32(4, address, new WasmInt32Constant(0), WasmInt32Subtype.INT32);
        if (slotOffset instanceof WasmInt32Constant) {
            store.setOffset(((WasmInt32Constant) slotOffset).getValue());
        }
        result = store;
    }

    private WasmExpression getSlotOffset(WasmExpression slot) {
        if (slot instanceof WasmInt32Constant) {
            int slotConstant = ((WasmInt32Constant) slot).getValue();
            return new WasmInt32Constant((slotConstant << 2) + 4);
        } else {
            slot = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, slot, new WasmInt32Constant(2));
            slot = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, slot, new WasmInt32Constant(4));
            return slot;
        }
    }

    @Override
    public void visit(BlockStatement statement) {
        WasmBlock block = new WasmBlock(false);

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
    public void visit(QualificationExpr expr) {
        WasmExpression address = getAddress(expr.getQualified(), expr.getField(), expr.getLocation());
        if (expr.getField().equals(MONITOR_FIELD)) {
            result = getMonitor(address, expr.getLocation());
            return;
        }

        ValueType type = context.getFieldType(expr.getField());
        WasmMemoryAccess resultExpr;
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    resultExpr = new WasmLoadInt32(1, address, WasmInt32Subtype.INT8);
                    break;
                case SHORT:
                    resultExpr = new WasmLoadInt32(2, address, WasmInt32Subtype.INT16);
                    break;
                case CHARACTER:
                    resultExpr = new WasmLoadInt32(2, address, WasmInt32Subtype.UINT16);
                    break;
                case INTEGER:
                    resultExpr = new WasmLoadInt32(4, address, WasmInt32Subtype.INT32);
                    break;
                case LONG:
                    resultExpr = new WasmLoadInt64(8, address, WasmInt64Subtype.INT64);
                    break;
                case FLOAT:
                    resultExpr = new WasmLoadFloat32(4, address);
                    break;
                case DOUBLE:
                    resultExpr = new WasmLoadFloat64(8, address);
                    break;
                default:
                    throw new AssertionError(type.toString());
            }
        } else {
            resultExpr = new WasmLoadInt32(4, address, WasmInt32Subtype.INT32);
        }

        resultExpr.setOffset(getOffset(expr.getQualified(), expr.getField()));
        result = (WasmExpression) resultExpr;
    }

    private WasmExpression getMonitor(WasmExpression address, TextLocation location) {
        var block = new WasmBlock(false);
        block.setType(WasmType.INT32);
        block.setLocation(location);

        var tmp = tempVars.acquire(WasmType.INT32);
        var monitor = new WasmLoadInt32(4, address, WasmInt32Subtype.INT32);
        monitor.setOffset(4);
        block.getBody().add(new WasmSetLocal(tmp, monitor));

        var isMonitor = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.AND,
                new WasmGetLocal(tmp), new WasmInt32Constant(0x80000000));
        var shiftMonitor = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                new WasmGetLocal(tmp), new WasmInt32Constant(1));
        var cond = new WasmConditional(isMonitor);
        cond.setType(WasmType.INT32);
        cond.getThenBlock().getBody().add(shiftMonitor);
        cond.getElseBlock().getBody().add(new WasmInt32Constant(0));
        block.getBody().add(cond);

        tempVars.release(tmp);
        return block;
    }

    private WasmExpression getAddress(Expr qualified, FieldReference field, TextLocation location) {
        if (qualified == null) {
            int offset = classGenerator.getFieldOffset(field);
            WasmExpression result = new WasmInt32Constant(offset);
            result.setLocation(location);
            return result;
        } else {
            accept(qualified);
            return result;
        }
    }

    private int getOffset(Expr qualified, FieldReference field) {
        if (qualified == null) {
            return 0;
        }
        return classGenerator.getFieldOffset(field);
    }

    @Override
    public void visit(BreakStatement statement) {
        IdentifiedStatement target = statement.getTarget();
        if (target == null) {
            target = currentBreakTarget;
        }
        WasmBlock wasmTarget = breakTargets.get(target);
        usedBlocks.add(wasmTarget);
        var br = new WasmBreak(wasmTarget);
        br.setLocation(statement.getLocation());
        resultConsumer.add(br);
    }

    @Override
    public void visit(ContinueStatement statement) {
        IdentifiedStatement target = statement.getTarget();
        if (target == null) {
            target = currentContinueTarget;
        }
        WasmBlock wasmTarget = continueTargets.get(target);
        usedBlocks.add(wasmTarget);
        var br = new WasmBreak(wasmTarget);
        br.setLocation(statement.getLocation());
        resultConsumer.add(br);
    }

    @Override
    public void visit(NewExpr expr) {
        var block = new WasmBlock(false);
        block.setLocation(expr.getLocation());
        block.setType(WasmType.INT32);
        var callSiteId = generateCallSiteId(expr.getLocation());
        block.getBody().add(generateRegisterCallSite(callSiteId, expr.getLocation()));
        block.getBody().add(allocateObject(expr.getConstructedClass(), expr.getLocation()));
        result = block;
    }

    private WasmExpression allocateObject(String className, TextLocation location) {
        int tag = classGenerator.getClassPointer(ValueType.object(className));
        String allocName = context.names.forMethod(new MethodReference(Allocator.class, "allocate",
                RuntimeClass.class, Address.class));
        WasmCall call = new WasmCall(allocName);
        call.getArguments().add(new WasmInt32Constant(tag));
        call.setLocation(location);
        return call;
    }

    @Override
    public void visit(NewArrayExpr expr) {
        var block = new WasmBlock(false);
        block.setType(WasmType.INT32);

        var callSiteId = generateCallSiteId(expr.getLocation());
        block.getBody().add(generateRegisterCallSite(callSiteId, expr.getLocation()));

        ValueType type = expr.getType();

        int classPointer = classGenerator.getClassPointer(ValueType.arrayOf(type));
        String allocName = context.names.forMethod(new MethodReference(Allocator.class, "allocateArray",
                RuntimeClass.class, int.class, Address.class));
        WasmCall call = new WasmCall(allocName);
        call.getArguments().add(new WasmInt32Constant(classPointer));
        accept(expr.getLength());
        call.getArguments().add(result);
        call.setLocation(expr.getLocation());
        block.getBody().add(call);

        result = block;
    }

    @Override
    public void visit(ArrayFromDataExpr expr) {
        ValueType type = expr.getType();

        ArrayType arrayType = ArrayType.OBJECT;
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

        WasmBlock block = new WasmBlock(false);
        block.setType(WasmType.INT32);
        var callSiteId = generateCallSiteId(expr.getLocation());
        block.getBody().add(generateRegisterCallSite(callSiteId, expr.getLocation()));

        WasmLocal array = tempVars.acquire(WasmType.INT32);
        int classPointer = classGenerator.getClassPointer(ValueType.arrayOf(type));
        String allocName = context.names.forMethod(new MethodReference(Allocator.class, "allocateArray",
                RuntimeClass.class, int.class, Address.class));
        WasmCall call = new WasmCall(allocName);
        call.getArguments().add(new WasmInt32Constant(classPointer));
        call.getArguments().add(new WasmInt32Constant(expr.getData().size()));
        call.setLocation(expr.getLocation());
        block.getBody().add(new WasmSetLocal(array, call));

        for (int i = 0; i < expr.getData().size(); ++i) {
            WasmExpression ptr = getArrayElementPointer(new WasmGetLocal(array), new WasmInt32Constant(i), arrayType);
            expr.getData().get(i).acceptVisitor(this);
            block.getBody().add(storeArrayItem(ptr, result, arrayType));
        }

        block.getBody().add(new WasmGetLocal(array));
        block.setLocation(expr.getLocation());
        tempVars.release(array);

        result = block;
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        ValueType type = expr.getType();

        WasmBlock block = new WasmBlock(false);
        block.setType(WasmType.INT32);

        int dimensionList = -1;
        for (Expr dimension : expr.getDimensions()) {
            int dimensionAddress = binaryWriter.append(DataPrimitives.INT.createValue());
            if (dimensionList < 0) {
                dimensionList = dimensionAddress;
            }
            accept(dimension);
            block.getBody().add(new WasmStoreInt32(4, new WasmInt32Constant(dimensionAddress), result,
                    WasmInt32Subtype.INT32));
        }

        int classPointer = classGenerator.getClassPointer(type);
        String allocName = context.names.forMethod(new MethodReference(Allocator.class, "allocateMultiArray",
                RuntimeClass.class, Address.class, int.class, RuntimeArray.class));
        WasmCall call = new WasmCall(allocName);
        call.getArguments().add(new WasmInt32Constant(classPointer));
        call.getArguments().add(new WasmInt32Constant(dimensionList));
        call.getArguments().add(new WasmInt32Constant(expr.getDimensions().size()));
        call.setLocation(expr.getLocation());

        block.getBody().add(call);
        result = block;
    }

    @Override
    public void visit(ReturnStatement statement) {
        if (statement.getResult() != null) {
            accept(statement.getResult());
        } else {
            result = null;
        }
        var wasmStatement = new WasmReturn(result);
        wasmStatement.setLocation(statement.getLocation());
        resultConsumer.add(wasmStatement);
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        var type = expr.getType();
        if (type instanceof ValueType.Object) {
            var className = ((ValueType.Object) type).getClassName();
            if (!context.characteristics.isManaged(className)) {
                expr.getExpr().acceptVisitor(this);
                return;
            }
        }

        classGenerator.getClassPointer(type);

        accept(expr.getExpr());

        WasmBlock block = new WasmBlock(false);
        block.setType(WasmType.INT32);
        block.setLocation(expr.getLocation());

        var cachedObject = exprCache.create(result, WasmType.INT32, expr.getLocation(), block.getBody());

        var ifNull = new WasmBranch(genIsZero(cachedObject.expr()), block);
        ifNull.setResult(new WasmInt32Constant(0));
        block.getBody().add(new WasmDrop(ifNull));

        WasmCall supertypeCall = new WasmCall(context.names.forSupertypeFunction(expr.getType()));
        WasmExpression classRef = new WasmLoadInt32(4, cachedObject.expr(), WasmInt32Subtype.INT32);
        classRef = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, classRef, new WasmInt32Constant(3));
        supertypeCall.getArguments().add(classRef);
        block.getBody().add(supertypeCall);

        cachedObject.release();

        result = block;
    }

    @Override
    public void visit(ThrowStatement statement) {
        var callSiteId = generateCallSiteId(statement.getLocation());
        resultConsumer.add(generateRegisterCallSite(callSiteId, statement.getLocation()));

        accept(statement.getException());
        var call = new WasmCall(context.names.forMethod(THROW_METHOD), result);
        call.setLocation(statement.getLocation());
        resultConsumer.add(call);

        var target = throwJumpTarget();
        var breakExpr = new WasmBreak(target);
        breakExpr.setLocation(statement.getLocation());
        if (target != rethrowBlock) {
            breakExpr.setResult(generateGetHandlerId(callSiteId, statement.getLocation()));
        }
        resultConsumer.add(breakExpr);
    }

    @Override
    public void visit(CastExpr expr) {
        var type = expr.getTarget();
        if (type instanceof ValueType.Object) {
            var className = ((ValueType.Object) type).getClassName();
            if (!context.characteristics.isManaged(className)) {
                expr.getValue().acceptVisitor(this);
                return;
            }
        }

        classGenerator.getClassPointer(type);

        var block = new WasmBlock(false);
        block.setType(WasmType.INT32);
        block.setLocation(expr.getLocation());

        accept(expr.getValue());
        var valueToCast = exprCache.create(result, WasmType.INT32, expr.getLocation(), block.getBody());

        var nullCheck = new WasmBranch(genIsZero(valueToCast.expr()), block);
        nullCheck.setResult(valueToCast.expr());
        block.getBody().add(new WasmDrop(nullCheck));

        var supertypeCall = new WasmCall(context.names.forSupertypeFunction(expr.getTarget()));
        WasmExpression classRef = new WasmLoadInt32(4, valueToCast.expr(), WasmInt32Subtype.INT32);
        classRef = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, classRef,
                new WasmInt32Constant(3));
        supertypeCall.getArguments().add(classRef);

        var breakIfPassed = new WasmBranch(supertypeCall, block);
        breakIfPassed.setResult(valueToCast.expr());
        block.getBody().add(new WasmDrop(breakIfPassed));

        var callSiteId = generateCallSiteId(expr.getLocation());
        block.getBody().add(generateRegisterCallSite(callSiteId, expr.getLocation()));

        var call = new WasmCall(context.names.forMethod(THROW_CCE_METHOD));
        block.getBody().add(call);

        var target = throwJumpTarget();
        var breakExpr = new WasmBreak(target);
        if (target != rethrowBlock) {
            breakExpr.setResult(generateGetHandlerId(callSiteId, expr.getLocation()));
        }
        block.getBody().add(breakExpr);

        valueToCast.release();
        result = block;
    }

    @Override
    public void visit(InitClassStatement statement) {
        if (classGenerator.hasClinit(statement.getClassName())) {
            var call = new WasmCall(context.names.forClassInitializer(statement.getClassName()));
            call.setLocation(statement.getLocation());

            var callSiteId = generateCallSiteId(statement.getLocation());
            resultConsumer.add(generateRegisterCallSite(callSiteId, statement.getLocation()));
            resultConsumer.add(call);
            checkHandlerId(resultConsumer, callSiteId, statement.getLocation());
        }
    }

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

        int firstId = handlers.size();

        var innerCatchBlock = new WasmBlock(false);
        var bodyBlock = new WasmBlock(false);
        bodyBlock.setType(WasmType.INT32);

        var isTopMostTryCatch = lastTryBlock == null;
        if (isTopMostTryCatch) {
            catchLabels.add(rethrowBlock());
        }

        var catchBlocks = new ArrayList<WasmBlock>();
        for (int i = 0; i < tryCatchStatements.size(); ++i) {
            var tryCatch = tryCatchStatements.get(i);
            handlers.add(new ExceptionHandlerDescriptor(firstId + i, tryCatch.getExceptionType()));
            catchBlocks.add(new WasmBlock(false));
        }
        var outerCatchBlock = catchBlocks.get(0);
        catchLabels.addAll(catchBlocks.subList(1, catchBlocks.size()));
        catchLabels.add(innerCatchBlock);

        var lastTryBlockBackup = lastTryBlock;
        lastTryBlock = bodyBlock;
        visitMany(statement.getProtectedBody(), bodyBlock.getBody());
        lastTryBlock = lastTryBlockBackup;
        handlers.subList(firstId, handlers.size()).clear();

        if (!bodyBlock.isTerminating()) {
            bodyBlock.getBody().add(new WasmBreak(outerCatchBlock));
        }
        var currentBlock = innerCatchBlock;
        var handlerIdExpr = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB,
                bodyBlock, new WasmInt32Constant(1));
        var switchExpr = new WasmSwitch(handlerIdExpr, outerCatchBlock);
        switchExpr.getTargets().addAll(catchLabels);
        innerCatchBlock.getBody().add(switchExpr);

        catchLabels.subList(catchLabels.size() - tryCatchStatements.size(), catchLabels.size()).clear();
        if (isTopMostTryCatch) {
            catchLabels.remove(catchLabels.size() - 1);
            assert catchLabels.isEmpty();
        }

        for (int i = tryCatchStatements.size() - 1; i >= 0; --i) {
            var tryCatch = tryCatchStatements.get(i);
            var catchBlock = catchBlocks.get(i);
            catchBlock.getBody().add(currentBlock);
            var catchMethodName = context.names.forMethod(CATCH_METHOD);
            var catchCall = new WasmCall(catchMethodName);
            var catchWrapper = tryCatch.getExceptionVariable() != null
                    ? new WasmSetLocal(localVar(tryCatch.getExceptionVariable()), catchCall)
                    : new WasmDrop(catchCall);
            catchBlock.getBody().add(catchWrapper);
            visitMany(tryCatch.getHandler(), catchBlock.getBody());
            if (!catchBlock.isTerminating() && catchBlock != outerCatchBlock) {
                catchBlock.getBody().add(new WasmBreak(outerCatchBlock));
            }
            currentBlock = catchBlock;
        }

        resultConsumer.add(outerCatchBlock);
    }

    private WasmBlock rethrowBlock() {
        if (rethrowBlock == null) {
            rethrowBlock = new WasmBlock(false);
        }
        return rethrowBlock;
    }

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
        WasmCall call = new WasmCall(context.names.forMethod(async ? MONITOR_ENTER : MONITOR_ENTER_SYNC));
        call.setLocation(statement.getLocation());
        statement.getObjectRef().acceptVisitor(this);
        call.getArguments().add(result);

        var callSiteId = generateCallSiteId(statement.getLocation());
        resultConsumer.add(generateRegisterCallSite(callSiteId, statement.getLocation()));
        resultConsumer.add(call);
        checkHandlerId(resultConsumer, callSiteId, statement.getLocation());
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        var call = new WasmCall(context.names.forMethod(async ? MONITOR_EXIT : MONITOR_EXIT_SYNC));
        call.setLocation(statement.getLocation());
        statement.getObjectRef().acceptVisitor(this);
        call.getArguments().add(result);

        var callSiteId = generateCallSiteId(statement.getLocation());
        resultConsumer.add(generateRegisterCallSite(callSiteId, statement.getLocation()));
        resultConsumer.add(call);
        checkHandlerId(resultConsumer, callSiteId, statement.getLocation());
    }

    @Override
    public void visit(BoundCheckExpr expr) {
        if (!managed) {
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
        block.getBody().add(generateRegisterCallSite(callSiteId, expr.getLocation()));
        block.getBody().add(new WasmCall(context.names.forMethod(THROW_AIOOBE_METHOD)));
        var br = new WasmBreak(throwJumpTarget());
        if (br.getTarget() != rethrowBlock) {
            br.setResult(generateGetHandlerId(callSiteId, expr.getLocation()));
        }
        block.getBody().add(br);

        result = block;
    }

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

    private WasmIntrinsicManager intrinsicManager = new WasmIntrinsicManager() {
        @Override
        public WasmExpression generate(Expr expr) {
            accept(expr);
            return result;
        }

        @Override
        public BinaryWriter getBinaryWriter() {
            return binaryWriter;
        }

        @Override
        public WasmStringPool getStringPool() {
            return context.getStringPool();
        }

        @Override
        public Diagnostics getDiagnostics() {
            return context.getDiagnostics();
        }

        @Override
        public NameProvider getNames() {
            return context.names;
        }

        @Override
        public WasmLocal getTemporary(WasmType type) {
            return tempVars.acquire(type);
        }

        @Override
        public void releaseTemporary(WasmLocal local) {
            tempVars.release(local);
        }

        @Override
        public int getStaticField(FieldReference field) {
            return classGenerator.getFieldOffset(field);
        }

        @Override
        public int getClassPointer(ValueType type) {
            return classGenerator.getClassPointer(type);
        }

        @Override
        public int getFunctionPointer(String name) {
            return classGenerator.getFunctionPointer(name);
        }

        @Override
        public boolean isManagedMethodCall(MethodReference method) {
            return needsCallSiteId() && ExceptionHandlingUtil.isManagedMethodCall(context.characteristics, method);
        }

        @Override
        public int generateCallSiteId(TextLocation location) {
            return WasmGenerationVisitor.this.generateCallSiteId(location);
        }

        @Override
        public WasmExpression generateRegisterCallSite(int callSite, TextLocation location) {
            return WasmGenerationVisitor.this.generateRegisterCallSite(callSite, location);
        }
    };

    private WasmExpression getReferenceToClass(WasmExpression instance) {
        var classIndex = new WasmLoadInt32(4, instance, WasmInt32Subtype.INT32);
        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, classIndex,
                new WasmInt32Constant(3));
    }

    private WasmExpression genIsZero(WasmExpression value) {
        return new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ, value);
    }
}
