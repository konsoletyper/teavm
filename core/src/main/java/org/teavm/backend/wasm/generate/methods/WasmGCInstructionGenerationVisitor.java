/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.wasm.generate.methods;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
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
import org.teavm.backend.wasm.generate.TemporaryVariablePool;
import org.teavm.backend.wasm.generate.ValueCache;
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfo;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmExpressionToInstructionConverter;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCastCondition;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmInt64Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
import org.teavm.backend.wasm.model.expression.WasmSignedType;
import org.teavm.backend.wasm.model.instruction.WasmBlockInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCatchClause;
import org.teavm.backend.wasm.model.instruction.WasmFragmentBuilder;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmTryInstruction;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.backend.wasm.types.PreciseTypeInference;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmGCInstructionGenerationVisitor implements StatementVisitor, ExprVisitor {
    private static final MethodReference MONITOR_ENTER_SYNC = new MethodReference(Object.class,
            "monitorEnterSync", Object.class, void.class);
    private static final MethodReference MONITOR_EXIT_SYNC = new MethodReference(Object.class,
            "monitorExitSync", Object.class, void.class);
    private static final MethodReference MONITOR_ENTER = new MethodReference(Object.class,
            "monitorEnter", Object.class, void.class);
    private static final MethodReference MONITOR_EXIT = new MethodReference(Object.class,
            "monitorExit", Object.class, void.class);

    private static final int SWITCH_TABLE_THRESHOLD = 256;

    private WasmGCGenerationContext context;
    private final MethodReference currentMethod;
    private final WasmFunction function;
    private int firstVariable;
    private boolean async;
    private TemporaryVariablePool tempVars;
    private ValueCache valueCache;
    private WasmBlockInstruction returnBlock;
    private WasmInstructionBuilder builder;
    private WasmType expectedType;
    private PreciseTypeInference types;
    private Set<MethodReference> asyncSplitMethods;
    private Map<IdentifiedStatement, WasmInstructionList> breakTargets = new HashMap<>();
    private Map<IdentifiedStatement, WasmInstructionList> continueTargets = new HashMap<>();
    private IdentifiedStatement currentBreakTarget;
    private IdentifiedStatement currentContinueTarget;
    private int blockLevel;

    private WasmGCGenerationVisitor compatIntrinsicVisitor;

    public WasmGCInstructionGenerationVisitor(WasmGCGenerationContext context, MethodReference currentMethod,
            WasmFunction function, int firstVariable, boolean async, PreciseTypeInference types,
            Set<MethodReference> asyncSplitMethods) {
        this.context = context;
        this.currentMethod = currentMethod;
        this.function = function;
        this.firstVariable = firstVariable;
        tempVars = new TemporaryVariablePool(function);
        valueCache = new ValueCache(tempVars);
        this.async = async;
        this.types = types;
        this.asyncSplitMethods = asyncSplitMethods;

        compatIntrinsicVisitor = new WasmGCGenerationVisitor(context, currentMethod, function, firstVariable, async,
                types, asyncSplitMethods);
    }

    public void setReturnBlock(WasmBlockInstruction returnBlock) {
        this.returnBlock = returnBlock;
    }

    public void generate(Statement statement, WasmInstructionList target) {
        builder = target.builder();
        statement.acceptVisitor(this);
        builder = null;
    }

    private void accept(Expr expr) {
        expr.acceptVisitor(this);
    }

    private void accept(Expr expr, WasmInstructionBuilder target) {
        var oldBuilder = builder;
        builder = target;
        expr.acceptVisitor(this);
        builder = oldBuilder;
    }

    private void accept(Expr expr, WasmInstructionBuilder target, WasmType expectedType) {
        var oldExpectedType = this.expectedType;
        var oldBuilder = builder;
        builder = target;
        this.expectedType = expectedType;
        expr.acceptVisitor(this);
        builder = oldBuilder;
        this.expectedType = oldExpectedType;
    }

    private void accept(Statement statement) {
        statement.acceptVisitor(this);
    }

    @Override
    public void visit(BinaryExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
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
                        builder.pushLocation(expr.getLocation());
                        accept(expr.getFirstOperand());
                        accept(expr.getSecondOperand());
                        var type = convertType(expr.getType());
                        var method = new MethodReference(WasmRuntime.class, "remainder", type, type, type);
                        builder.call(context.functions().forStaticMethod(method));
                        builder.popLocation();
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
                if (expr.getType() == null) {
                    builder.pushLocation(expr.getLocation());
                    generateReferenceEq(expr);
                    builder.popLocation();
                } else {
                    generateBinary(WasmIntBinaryOperation.EQ, WasmFloatBinaryOperation.EQ, expr);
                }
                break;
            case NOT_EQUALS:
                if (expr.getType() == null) {
                    builder.pushLocation(expr.getLocation());
                    generateReferenceEq(expr);
                    builder.negate();
                    builder.popLocation();
                } else {
                    generateBinary(WasmIntBinaryOperation.NE, WasmFloatBinaryOperation.NE, expr);
                }
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
            case COMPARE_GREATER: {
                builder.pushLocation(expr.getLocation());
                accept(expr.getFirstOperand());
                accept(expr.getSecondOperand());
                var type = convertType(expr.getType());
                var method = new MethodReference(WasmRuntime.class, "compare", type, type, int.class);
                builder.call(context.functions().forStaticMethod(method));
                builder.popLocation();
                break;
            }
            case COMPARE_LESS: {
                builder.pushLocation(expr.getLocation());
                accept(expr.getFirstOperand());
                accept(expr.getSecondOperand());
                var type = convertType(expr.getType());
                var name = expr.getType() == OperationType.INT || expr.getType() == OperationType.LONG
                        ? "compare" : "compareLess";
                var method = new MethodReference(WasmRuntime.class, name, type, type, int.class);
                builder.call(context.functions().forStaticMethod(method));
                builder.popLocation();
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

    private void generateReferenceEq(BinaryExpr expr) {
        if (isNull(expr.getFirstOperand())) {
            accept(expr.getSecondOperand());
            if (!builder.isTerminating()) {
                var typeStack = builder.typeInference.typeStack;
                if (!typeStack.isEmpty() && typeStack.get(typeStack.size() - 1) == WasmType.INT32) {
                    builder.i32Const(0);
                    builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ);
                } else {
                    builder.isNull();
                }
            }
        } else if (isNull(expr.getSecondOperand())) {
            accept(expr.getFirstOperand());
            if (!builder.isTerminating()) {
                var typeStack = builder.typeInference.typeStack;
                if (!typeStack.isEmpty() && typeStack.get(typeStack.size() - 1) == WasmType.INT32) {
                    builder.i32Const(0);
                    builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ);
                } else {
                    builder.isNull();
                }
            }
        } else {
            accept(expr.getFirstOperand());
            accept(expr.getSecondOperand());
            if (!builder.isTerminating()) {
                var typeStack = builder.typeInference.typeStack;
                if (typeStack.size() >= 2 && typeStack.get(typeStack.size() - 2) == WasmType.INT32) {
                    builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ);
                } else {
                    builder.refEqual();
                }
            }
        }
    }

    private boolean isNull(Expr expr) {
        if (!(expr instanceof ConstantExpr)) {
            return false;
        }
        return ((ConstantExpr) expr).getValue() == null;
    }

    private void generateBinary(WasmIntBinaryOperation intOp, WasmFloatBinaryOperation floatOp, BinaryExpr expr) {
        builder.pushLocation(expr.getLocation());
        accept(expr.getFirstOperand());
        accept(expr.getSecondOperand());
        if (expr.getType() == null) {
            builder.intBinary(WasmIntType.INT32, intOp);
        } else {
            switch (expr.getType()) {
                case INT:
                    builder.intBinary(WasmIntType.INT32, intOp);
                    break;
                case LONG:
                    builder.intBinary(WasmIntType.INT64, intOp);
                    break;
                case FLOAT:
                    builder.floatBinary(WasmFloatType.FLOAT32, floatOp);
                    break;
                case DOUBLE:
                    builder.floatBinary(WasmFloatType.FLOAT64, floatOp);
                    break;
            }
        }
        builder.popLocation();
    }

    private void generateBinary(WasmIntBinaryOperation intOp, BinaryExpr expr) {
        builder.pushLocation(expr.getLocation());
        accept(expr.getFirstOperand());
        accept(expr.getSecondOperand());

        if (expr.getType() == OperationType.LONG) {
            switch (expr.getOperation()) {
                case LEFT_SHIFT:
                case RIGHT_SHIFT:
                case UNSIGNED_RIGHT_SHIFT:
                    builder.convert(WasmNumType.INT32, WasmNumType.INT64, false);
                    break;
                default:
                    break;
            }
        }

        switch (expr.getType()) {
            case INT:
                builder.intBinary(WasmIntType.INT32, intOp);
                break;
            case LONG:
                builder.intBinary(WasmIntType.INT64, intOp);
                break;
            case FLOAT:
            case DOUBLE:
                throw new IllegalStateException("Can't translate operation " + intOp + " for type " + expr.getType());
        }
        builder.popLocation();
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
        var first = new WasmInstructionList().builder();
        first.pushLocation(expr.getLocation());
        accept(expr.getFirstOperand(), first);
        first.popLocation();
        if (first.isTerminating()) {
            builder.transferFrom(first);
            return;
        }

        builder.pushLocation(expr.getLocation());
        var block = builder.block(WasmType.INT32);

        block
                .i32Const(0)
                .transferFrom(first)
                .negate()
                .branch(block)
                .drop();

        accept(expr.getSecondOperand(), block);
        builder.popLocation();
    }

    private void generateOr(BinaryExpr expr) {
        var first = new WasmInstructionList().builder();
        first.pushLocation(expr.getLocation());
        accept(expr.getFirstOperand(), first);
        if (first.isTerminating()) {
            builder.transferFrom(first);
            return;
        }

        builder.pushLocation(expr.getLocation());
        var block = builder.block(WasmType.INT32);

        block
                .i32Const(1)
                .transferFrom(first)
                .branch(block)
                .drop();

        accept(expr.getSecondOperand(), block);
        builder.popLocation();
    }

    @Override
    public void visit(UnaryExpr expr) {
        builder.pushLocation(expr.getLocation());
        switch (expr.getOperation()) {
            case INT_TO_BYTE:
                accept(expr.getOperand());
                builder
                        .i32Const(24)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL)
                        .i32Const(24)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_SIGNED);
                break;
            case INT_TO_SHORT:
                accept(expr.getOperand());
                builder
                        .i32Const(16)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL)
                        .i32Const(16)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_SIGNED);
                break;
            case INT_TO_CHAR:
                accept(expr.getOperand());
                builder
                        .i32Const(16)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL)
                        .i32Const(16)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHR_UNSIGNED);
                break;
            case LENGTH:
                accept(expr.getOperand());
                builder.arrayLength();
                break;
            case NOT:
                accept(expr.getOperand());
                builder.negate();
                break;
            case NEGATE:
                switch (expr.getType()) {
                    case INT:
                        builder.i32Const(0);
                        break;
                    case LONG:
                        builder.i64Const(0L);
                        break;
                    case FLOAT:
                        builder.f32Const(0f);
                        break;
                    case DOUBLE:
                        builder.f64Const(0.0);
                        break;
                }
                accept(expr.getOperand());
                switch (expr.getType()) {
                    case INT:
                        builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
                        break;
                    case LONG:
                        builder.intBinary(WasmIntType.INT64, WasmIntBinaryOperation.SUB);
                        break;
                    case FLOAT:
                        builder.floatBinary(WasmFloatType.FLOAT32, WasmFloatBinaryOperation.SUB);
                        break;
                    case DOUBLE:
                        builder.floatBinary(WasmFloatType.FLOAT64, WasmFloatBinaryOperation.SUB);
                        break;
                }
                break;
            case NULL_CHECK:
                builder.pushLocation(expr.getLocation());
                accept(expr.getOperand());
                generateNullCheck();
                builder.popLocation();
                break;
        }
        builder.popLocation();
    }

    private void generateNullCheck() {
        var typeStack = builder.typeInference.typeStack;
        if (typeStack.isEmpty()) {
            return;
        }
        var topType = typeStack.get(typeStack.size() - 1);
        if (topType == WasmType.INT32) {
            return;
        }

        var valueType = (WasmType.Reference) topType;

        var blockBuilder = builder.block(context.functionTypes().of(valueType, valueType).asBlock());
        blockBuilder.nullBranch(WasmNullCondition.NOT_NULL, blockBuilder);
        generateThrowNpe(blockBuilder);
    }

    @Override
    public void visit(ConditionalExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());
        accept(expr.getCondition());
        WasmType resultType = null;
        if (expr.getVariableIndex() >= 0) {
            var javaType = types.typeOf(expr.getVariableIndex());
            if (javaType != null) {
                resultType = context.typeMapper().mapType(javaType.valueType);
            }
        }
        if (resultType == null && expr.getConsequent().getVariableIndex() >= 0
                && expr.getConsequent().getVariableIndex() == expr.getAlternative().getVariableIndex()) {
            var javaType = types.typeOf(expr.getConsequent().getVariableIndex());
            if (javaType != null) {
                resultType = context.typeMapper().mapType(javaType.valueType);
            }
        }

        var cond = builder.conditional(resultType);
        var thenBuilder = cond.getThenBlock().builder();
        accept(expr.getConsequent(), thenBuilder, expectedType);

        var elseBuilder = cond.getElseBlock().builder();
        accept(expr.getAlternative(), elseBuilder, expectedType);

        if (resultType == null && !thenBuilder.typeInference.typeStack.isEmpty()) {
            var thenStack = thenBuilder.typeInference.typeStack;
            resultType = thenStack.get(thenStack.size() - 1);
            cond.setType(resultType != null ? resultType.asBlock() : null);
        }

        builder.popLocation();
    }

    @Override
    public void visit(ConstantExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        if (expr.getValue() == null) {
            WasmType type = expectedType;
            if (type == WasmType.INT32) {
                builder.i32Const(0);
            } else {
                if (expr.getVariableIndex() >= 0) {
                    var javaType = types.typeOf(expr.getVariableIndex());
                    if (javaType != null) {
                        type = context.typeMapper().mapType(javaType.valueType);
                    }
                }
                if (type instanceof WasmType.Reference) {
                    builder.nullConst((WasmType.Reference) type);
                } else {
                    var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
                    builder.nullConst(objectClass.getType());
                }
            }
        } else if (expr.getValue() instanceof Integer) {
            builder.i32Const((Integer) expr.getValue());
        } else if (expr.getValue() instanceof Long) {
            builder.i64Const((Long) expr.getValue());
        } else if (expr.getValue() instanceof Float) {
            builder.f32Const((Float) expr.getValue());
        } else if (expr.getValue() instanceof Double) {
            builder.f64Const((Double) expr.getValue());
        } else if (expr.getValue() instanceof String) {
            var stringConstant = context.strings().getStringConstant((String) expr.getValue());
            builder.getGlobal(stringConstant.global);
        } else if (expr.getValue() instanceof ValueType) {
            emitClassLiteral((ValueType) expr.getValue());
        } else {
            throw new IllegalArgumentException("Constant unsupported: " + expr.getValue());
        }
    }

    private void emitClassInfoLiteral(WasmInstructionBuilder builder, ValueType type) {
        var degree = 0;
        if (type instanceof ValueType.Array) {
            var itemType = ((ValueType.Array) type).getItemType();
            if (!(itemType instanceof ValueType.Primitive)) {
                while (type instanceof ValueType.Array) {
                    type = ((ValueType.Array) type).getItemType();
                    ++degree;
                }
            }
        }
        builder.getGlobal(context.classInfoProvider().getClassInfo(type).getPointer());
        while (degree-- > 0) {
            builder.call(context.classInfoProvider().getGetArrayClassFunction());
        }
    }

    private void emitClassLiteral(ValueType type) {
        emitClassInfoLiteral(builder, type);
        builder.call(context.classInfoProvider().reflectionTypes().classInfo().classObjectFunction());
    }

    @Override
    public void visit(VariableExpr expr) {
        var local = function.getLocalVariables().get(expr.getIndex() - firstVariable);
        builder.getLocal(local);
    }

    @Override
    public void visit(SubscriptExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());
        accept(expr.getArray());
        if (builder.isTerminating()) {
            builder.popLocation();
            return;
        }

        var typeStack = builder.typeInference.typeStack;
        WasmArray wasmArray = null;
        if (!typeStack.isEmpty()) {
            var arrayType = typeStack.get(typeStack.size() - 1);
            if (arrayType instanceof WasmType.CompositeReference) {
                var composite = ((WasmType.CompositeReference) arrayType).composite;
                if (composite instanceof WasmArray) {
                    wasmArray = (WasmArray) composite;
                }
            }
        }

        accept(expr.getIndex(), builder, WasmType.INT32);
        if (wasmArray != null) {
            switch (expr.getType()) {
                case BYTE:
                    builder.arrayGet(wasmArray, WasmSignedType.SIGNED);
                    break;
                case SHORT:
                    builder.arrayGet(wasmArray, WasmSignedType.SIGNED);
                    break;
                case CHAR:
                    builder.arrayGet(wasmArray, WasmSignedType.UNSIGNED);
                    break;
                default:
                    builder.arrayGet(wasmArray);
                    break;
            }
            if (expr.getType() == ArrayType.OBJECT && expr.getVariableIndex() >= 0) {
                var targetType = types.typeOf(expr.getVariableIndex());
                if (targetType != null) {
                    var wasmTargetType = (WasmType.Reference) context.typeMapper().mapType(targetType.valueType);
                    if (!isExtern(wasmTargetType)) {
                        builder.cast(wasmTargetType);
                    }
                }
            }
        } else {
            builder.unreachable();
        }
        builder.popLocation();
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());
        accept(expr.getArray());
        if (!builder.isTerminating()) {
            var typeStack = builder.typeInference.typeStack;
            var arrayType = typeStack.get(typeStack.size() - 1);
            if (arrayType instanceof WasmType.CompositeReference) {
                var arrayStruct = (WasmStructure) ((WasmType.CompositeReference) arrayType).composite;
                builder.structGet(arrayStruct, WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
            }
        }
        builder.popLocation();
    }

    @Override
    public void visit(InvocationExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());
        generateInvocation(expr);
        builder.popLocation();
    }

    @Override
    public void visit(QualificationExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());
        if (expr.getQualified() == null) {
            var global = context.classInfoProvider().getStaticFieldLocation(expr.getField());
            builder.getGlobal(global);
        } else {
            var cls = context.classes().get(expr.getField().getClassName());
            if (cls == null) {
                builder.unreachable();
                builder.popLocation();
                return;
            }
            var classInfo = context.classInfoProvider().getClassInfo(expr.getField().getClassName());
            accept(expr.getQualified(), builder, context.typeMapper().mapType(
                    ValueType.object(expr.getField().getClassName())));
            if (classInfo.isHeapStructure()) {
                var offset = context.classInfoProvider().getHeapFieldOffset(expr.getField());
                var fieldReader = cls.getField(expr.getField().getFieldName());
                if (fieldReader != null) {
                    loadHeapField(fieldReader.getType(), offset);
                }
            } else {
                loadNormalField(expr, classInfo);
            }
        }
        builder.popLocation();
    }

    private void loadNormalField(QualificationExpr expr, WasmGCClassInfo classInfo) {
        var fieldIndex = context.classInfoProvider().getFieldIndex(expr.getField());
        if (fieldIndex < 0) {
            builder.unreachable();
            return;
        }
        var struct = classInfo.getStructure();
        var cls = context.classes().get(expr.getField().getClassName());
        if (cls != null) {
            var field = cls.getField(expr.getField().getFieldName());
            if (field != null) {
                var fieldType = field.getType();
                if (fieldType instanceof ValueType.Primitive) {
                    switch (((ValueType.Primitive) fieldType).getKind()) {
                        case BOOLEAN:
                        case CHARACTER:
                            builder.structGet(struct, fieldIndex, WasmSignedType.UNSIGNED);
                            return;
                        case BYTE:
                        case SHORT:
                            builder.structGet(struct, fieldIndex, WasmSignedType.SIGNED);
                            return;
                        default:
                            break;
                    }
                }
            }
        }
        builder.structGet(struct, fieldIndex);
    }

    private void loadHeapField(ValueType type, int offset) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    builder.loadI32(1, offset, WasmInt32Subtype.INT8);
                    return;
                case CHARACTER:
                    builder.loadI32(2, offset, WasmInt32Subtype.UINT16);
                    return;
                case SHORT:
                    builder.loadI32(2, offset, WasmInt32Subtype.INT16);
                    return;
                case INTEGER:
                    builder.loadI32(4, offset, WasmInt32Subtype.INT32);
                    return;
                case LONG:
                    builder.loadI64(8, offset, WasmInt64Subtype.INT64);
                    return;
                case FLOAT:
                    builder.loadF32(4, offset);
                    return;
                case DOUBLE:
                    builder.loadF64(8, offset);
                    return;
                default:
                    break;
            }
        }
        builder.loadI32(4, offset, WasmInt32Subtype.INT32);
    }

    @Override
    public void visit(NewExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());
        var tmp = tempVars.acquire(context.typeMapper().mapType(ValueType.object(expr.getConstructedClass())));
        allocateObject(expr.getConstructedClass(), tmp);
        builder.getLocal(tmp);
        tempVars.release(tmp);
        builder.popLocation();
    }

    @Override
    public void visit(NewArrayExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());
        emitClassInfoLiteral(builder, expr.getType());
        accept(expr.getLength(), builder, WasmType.INT32);
        builder.call(context.classInfoProvider().getArrayConstructor(expr.getType()));
        builder.popLocation();
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());
        emitClassInfoLiteral(builder, ((ValueType.Array) expr.getType()).getItemType());
        for (var dimension : expr.getDimensions()) {
            accept(dimension, builder, WasmType.INT32);
        }
        builder.call(context.classInfoProvider().getMultiArrayConstructor(expr.getDimensions().size()));
        builder.popLocation();
    }

    @Override
    public void visit(ArrayFromDataExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());

        var classInfoType = context.classInfoProvider().reflectionTypes().classInfo();
        var classInfo = context.classInfoProvider().getClassInfo(ValueType.arrayOf(expr.getType()));

        int depth = 1;
        var itemType = expr.getType();
        while (itemType instanceof ValueType.Array) {
            itemType = ((ValueType.Array) itemType).getItemType();
        }
        builder.getGlobal(context.classInfoProvider().getClassInfo(itemType).getPointer());
        for (var i = 0; i < depth; ++i) {
            builder.call(context.classInfoProvider().getGetArrayClassFunction());
        }
        builder.structGet(classInfoType.structure(), classInfoType.vtableIndex());

        builder.nullConst(WasmType.EQ);

        var wasmArrayType = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.arrayOf(expr.getType()));
        var wasmArrayStruct = (WasmStructure) wasmArrayType.composite;
        var wasmArrayDataType = (WasmType.CompositeReference) wasmArrayStruct.getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        var wasmArray = (WasmArray) wasmArrayDataType.composite;

        var elementType = wasmArray.getElementType().asUnpackedType();
        for (var item : expr.getData()) {
            accept(item, builder, elementType);
        }
        builder
                .arrayNewFixed(wasmArray, expr.getData().size())
                .structNew(classInfo.getStructure());

        builder.popLocation();
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());

        var type = expr.getType();
        if (canCastNatively(type)) {
            var wasmType = context.classInfoProvider().getClassInfo(type).getStructure().getNonNullReference();
            accept(expr.getExpr(), builder, context.typeMapper().mapType(type));
            builder.test(wasmType);
        } else {
            var block = builder.block(WasmType.INT32);
            accept(expr.getExpr(), block);
            if (block.isTerminating()) {
                builder.transferFrom(block);
                builder.popLocation();
                return;
            }

            var typeStack = block.typeInference.typeStack;
            var topType = typeStack.isEmpty() ? null : typeStack.get(typeStack.size() - 1);

            if (topType instanceof WasmType.Reference) {
                // Get the class ref and call supertype check
                var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
                var vtStruct = objectClass.getVirtualTableStructure();

                var innerBlock = block.block(context.functionTypes().of(null, topType).asBlock());
                innerBlock
                        .nullBranch(WasmNullCondition.NULL, innerBlock)
                        .structGet(objectClass.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET)
                        .structGet(vtStruct, WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);
                emitClassInfoLiteral(innerBlock, type);
                innerBlock
                        .call(context.supertypeFunctions().getIsSupertypeFunction(type))
                        .breakTo(block);
            }
            block.i32Const(0);

            builder.popLocation();
            return;
        }

        builder.popLocation();
    }

    private boolean canCastNatively(ValueType type) {
        if (type instanceof ValueType.Array) {
            return true;
        }
        if (!(type instanceof ValueType.Object)) {
            return false;
        }
        var className = ((ValueType.Object) type).getClassName();
        var cls = context.classes().get(className);
        if (cls == null) {
            return false;
        }
        return !cls.hasModifier(ElementModifier.INTERFACE);
    }

    @Override
    public void visit(CastExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());

        if (expr.getTarget() instanceof ValueType.Object) {
            var className = ((ValueType.Object) expr.getTarget()).getClassName();
            if (context.classInfoProvider().getClassInfo(className).isHeapStructure()) {
                accept(expr.getValue(), builder, WasmType.INT32);
                builder.popLocation();
                return;
            }
        }

        accept(expr.getValue(), builder, context.typeMapper().mapType(expr.getTarget()));
        if (builder.isTerminating()) {
            builder.popLocation();
            return;
        }

        var sourceTypeStack = builder.typeInference.typeStack;
        var sourceType = sourceTypeStack.isEmpty() ? null
                : sourceTypeStack.get(sourceTypeStack.size() - 1);
        if (!(sourceType instanceof WasmType.Reference)) {
            builder.popLocation();
            return;
        }
        var sourceRef = (WasmType.Reference) sourceType;

        var targetType = (WasmType.Reference) context.typeMapper().mapType(expr.getTarget());
        WasmStructure targetStruct = null;
        if (targetType instanceof WasmType.CompositeReference) {
            var composite = ((WasmType.CompositeReference) targetType).composite;
            if (composite instanceof WasmStructure) {
                targetStruct = (WasmStructure) composite;
            }
        }

        var canInsertCast = true;
        if (targetStruct != null && sourceRef instanceof WasmType.CompositeReference) {
            var sourceComposite = (WasmType.CompositeReference) sourceRef;
            var sourceStruct = sourceComposite.composite instanceof WasmStructure
                    ? (WasmStructure) sourceComposite.composite : null;
            if (sourceStruct != null) {
                if (targetStruct.isSupertypeOf(sourceStruct)) {
                    canInsertCast = false;
                } else if (!sourceStruct.isSupertypeOf(targetStruct)) {
                    generateThrowCce(builder);
                    builder.popLocation();
                    return;
                }
            }
        }

        if (!expr.isWeak() && context.isStrict()) {
            if (canCastNatively(expr.getTarget())) {
                if (canInsertCast) {
                    var block = builder.block(context.functionTypes().of(targetType, sourceRef).asBlock());
                    block.castBranch(WasmCastCondition.SUCCESS, sourceRef, targetType, block);
                    generateThrowCce(block);
                }
            } else {
                var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
                var vtStruct = objectClass.getVirtualTableStructure();
                var srcValue = valueCache.create(sourceType, builder);
                var block = builder.block(context.functionTypes().of(null, sourceType).asBlock());
                block
                        .nullBranch(WasmNullCondition.NULL, block)
                        .structGet(objectClass.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET)
                        .structGet(vtStruct, WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);
                emitClassInfoLiteral(block, expr.getTarget());
                block
                        .call(context.supertypeFunctions().getIsSupertypeFunction(expr.getTarget()))
                        .branch(block);
                generateThrowCce(block);
                builder
                        .append(srcValue)
                        .cast(targetType);
                srcValue.release();
                if (canInsertCast) {
                    builder.cast(targetType);
                }
            }
        } else if (canInsertCast) {
            builder.cast(targetType);
        }

        builder.popLocation();
    }

    @Override
    public void visit(PrimitiveCastExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());
        accept(expr.getValue());
        builder.nonTrapConvert(WasmGeneratorUtil.mapType(expr.getSource()),
                WasmGeneratorUtil.mapType(expr.getTarget()), true);
        builder.popLocation();
    }

    @Override
    public void visit(BoundCheckExpr expr) {
        if (builder.isTerminating()) {
            return;
        }
        builder.pushLocation(expr.getLocation());
        accept(expr.getIndex());

        var indexValue = valueCache.create(WasmType.INT32, builder);
        builder.drop();
        var block = builder.block();

        if (expr.getArray() != null) {
            var condBlock = block;
            if (expr.isLower()) {
                condBlock = block.block();
                condBlock
                        .append(indexValue)
                        .i32Const(0)
                        .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED)
                        .branch(condBlock.list);
            }

            condBlock.append(indexValue);
            accept(expr.getArray(), condBlock);
            condBlock
                    .arrayLength()
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED)
                    .branch(block.list);
        } else if (expr.isLower()) {
            block
                    .append(indexValue)
                    .i32Const(0)
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.GE_SIGNED)
                    .branch(block.list);
        }

        generateThrowAioobe(block);
        builder.append(indexValue);
        indexValue.release();
        builder.popLocation();
    }

    @Override
    public void visit(AssignmentStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        var left = statement.getLeftValue();
        if (left == null) {
            if (statement.getRightValue() instanceof InvocationExpr) {
                var invocation = (InvocationExpr) statement.getRightValue();
                builder.pushLocation(statement.getLocation());
                generateInvocation(invocation);
                if (invocation.getMethod().getReturnType() != ValueType.VOID
                        || invocation.getType() == InvocationType.CONSTRUCTOR) {
                    builder.drop();
                }
                builder.popLocation();
            } else {
                builder.pushLocation(statement.getLocation());
                accept(statement.getRightValue());
                builder.drop();
                builder.popLocation();
            }
        } else if (left instanceof VariableExpr) {
            var varExpr = (VariableExpr) left;
            var local = function.getLocalVariables().get(varExpr.getIndex() - firstVariable);
            builder.pushLocation(statement.getLocation());
            accept(statement.getRightValue(), builder, local.getType());
            builder.setLocal(local);
            builder.popLocation();
        } else if (left instanceof QualificationExpr) {
            var lhs = (QualificationExpr) left;
            builder.pushLocation(statement.getLocation());
            storeField(lhs.getQualified(), lhs.getField(), statement.getRightValue());
            builder.popLocation();
        } else if (left instanceof SubscriptExpr) {
            builder.pushLocation(statement.getLocation());
            var lhs = (SubscriptExpr) left;
            storeArrayItem(lhs, statement.getRightValue());
            builder.popLocation();
        } else {
            throw new UnsupportedOperationException("This expression is not supported yet");
        }
    }

    private void storeField(Expr qualified, FieldReference field, Expr value) {
        if (qualified == null) {
            var global = context.classInfoProvider().getStaticFieldLocation(field);
            accept(value, builder, global.getType());
            builder.setGlobal(global);
        } else {
            var cls = context.classes().get(field.getClassName());
            if (cls == null) {
                builder.unreachable();
                return;
            }
            var fieldReader = cls.getField(field.getFieldName());
            if (fieldReader == null) {
                builder.unreachable();
                return;
            }
            accept(qualified, builder, context.typeMapper().mapType(ValueType.object(field.getClassName())));
            accept(value, builder, context.typeMapper().mapType(fieldReader.getType()));
            var classInfo = context.classInfoProvider().getClassInfo(field.getClassName());
            if (!classInfo.isHeapStructure()) {
                storeNormalField(classInfo, field);
            } else {
                storeHeapField(field);
            }
        }
    }

    private void storeNormalField(WasmGCClassInfo classInfo, FieldReference fieldRef) {
        var fieldIndex = context.classInfoProvider().getFieldIndex(fieldRef);
        if (fieldIndex >= 0) {
            builder.structSet(classInfo.getStructure(), fieldIndex);
        } else {
            builder.unreachable();
        }
    }

    private void storeHeapField(FieldReference field) {
        var cls = context.classes().get(field.getClassName());
        var type = cls.getField(field.getFieldName()).getType();
        var offset = context.classInfoProvider().getHeapFieldOffset(field);
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    builder.storeI32(1, offset, WasmInt32Subtype.INT8);
                    return;
                case CHARACTER:
                    builder.storeI32(2, offset, WasmInt32Subtype.UINT16);
                    return;
                case SHORT:
                    builder.storeI32(2, offset, WasmInt32Subtype.INT16);
                    return;
                case INTEGER:
                    builder.storeI32(4, offset, WasmInt32Subtype.INT32);
                    return;
                case LONG:
                    builder.storeI64(8, offset, WasmInt64Subtype.INT64);
                    return;
                case FLOAT:
                    builder.storeF32(4, offset);
                    return;
                case DOUBLE:
                    builder.storeF64(8, offset);
                    return;
                default:
                    break;
            }
        }
        builder.storeI32(4, offset, WasmInt32Subtype.INT32);
    }

    protected void storeArrayItem(SubscriptExpr subscriptExpr, Expr value) {
        subscriptExpr.getArray().acceptVisitor(this);
        accept(subscriptExpr.getIndex(), builder, WasmType.INT32);
        value.acceptVisitor(this);

        WasmArray array;
        switch (subscriptExpr.getType()) {
            case BYTE:
                array = context.classInfoProvider().getPrimitiveArrayType(WasmStorageType.INT8);
                break;
            case SHORT:
            case CHAR:
                array = context.classInfoProvider().getPrimitiveArrayType(WasmStorageType.INT16);
                break;
            case INT:
                array = context.classInfoProvider().getPrimitiveArrayType(WasmType.INT32.asStorage());
                break;
            case LONG:
                array = context.classInfoProvider().getPrimitiveArrayType(WasmType.INT64.asStorage());
                break;
            case FLOAT:
                array = context.classInfoProvider().getPrimitiveArrayType(WasmType.FLOAT32.asStorage());
                break;
            case DOUBLE:
                array = context.classInfoProvider().getPrimitiveArrayType(WasmType.FLOAT64.asStorage());
                break;
            default:
                array = context.classInfoProvider().getObjectArrayType();
                break;
        }
        builder.arraySet(array);
    }

    @Override
    public void visit(SequentialStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        for (var part : statement.getSequence()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(ConditionalStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        accept(statement.getCondition());
        ++blockLevel;
        var cond = builder.conditional();
        var thenBuilder = cond.getThenBlock().builder();
        var oldBuilder = builder;
        builder = thenBuilder;
        for (var s : statement.getConsequent()) {
            s.acceptVisitor(this);
        }
        if (!statement.getAlternative().isEmpty()) {
            builder = cond.getElseBlock().builder();
            for (var s : statement.getAlternative()) {
                s.acceptVisitor(this);
            }
        }
        builder = oldBuilder;
        --blockLevel;
    }

    @Override
    public void visit(SwitchStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        int min = statement.getClauses().stream()
                .flatMapToInt(clause -> Arrays.stream(clause.getConditions()))
                .min().orElse(0);
        int max = statement.getClauses().stream()
                .flatMapToInt(clause -> Arrays.stream(clause.getConditions()))
                .max().orElse(0);

        ++blockLevel;
        var outermostBlock = new WasmBlockInstruction(false);
        breakTargets.put(statement, outermostBlock.getBody());
        var oldBreakTarget = currentBreakTarget;
        currentBreakTarget = statement;

        var innermostBlock = new WasmBlockInstruction(false);
        var innermostBuilder = innermostBlock.getBody().builder();

        var builderBackup = builder;
        builder = innermostBuilder;
        var currentBlock = innermostBlock;
        accept(statement.getValue());

        var clauses = statement.getClauses();
        var clauseLists = new WasmInstructionList[clauses.size()];

        for (int i = 0; i < clauses.size(); i++) {
            clauseLists[i] = currentBlock.getBody();
            var nextBlock = new WasmBlockInstruction(false);
            builder = nextBlock.getBody().builder();
            builder.add(currentBlock);
            currentBlock = nextBlock;

            for (var part : clauses.get(i).getBody()) {
                accept(part);
            }
            builder.breakTo(outermostBlock.getBody());
        }

        var defaultTarget = currentBlock;
        outermostBlock.getBody().add(currentBlock);
        builder = outermostBlock.getBody().builder();
        for (var part : statement.getDefaultClause()) {
            accept(part);
        }

        // Generate the condition dispatcher
        builder = innermostBuilder;
        if ((long) max - min >= SWITCH_TABLE_THRESHOLD) {
            translateSwitchToBinarySearch(statement, defaultTarget.getBody(), clauseLists);
        } else {
            translateSwitchToWasmSwitch(statement, defaultTarget.getBody(), clauseLists, min, max);
        }

        builder = builderBackup;
        builder.add(outermostBlock);
        breakTargets.remove(statement);
        currentBreakTarget = oldBreakTarget;
        --blockLevel;
    }

    private void translateSwitchToBinarySearch(SwitchStatement statement, WasmInstructionList defaultList,
            WasmInstructionList[] clauseLists) {
        var entries = new ArrayList<TableEntry>();
        for (int i = 0; i < statement.getClauses().size(); i++) {
            var clause = statement.getClauses().get(i);
            for (int label : clause.getConditions()) {
                entries.add(new TableEntry(label, clauseLists[i]));
            }
        }
        entries.sort(Comparator.comparingInt(e -> e.label));
        var testValue = valueCache.create(WasmType.INT32, builder);
        generateBinarySearch(entries, 0, entries.size() - 1, builder, defaultList, testValue);
        testValue.release();
        builder.breakTo(defaultList);
    }

    private void generateBinarySearch(List<TableEntry> entries, int lower, int upper, WasmInstructionBuilder builder,
            WasmInstructionList defaultTarget, WasmFragmentBuilder testVar) {
        if (upper - lower == 0) {
            int label = entries.get(lower).label;
            builder
                    .append(testVar)
                    .i32Const(label)
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ)
                    .branch(entries.get(lower).target)
                    .breakTo(defaultTarget);
        } else if (upper - lower <= 0) {
            builder.breakTo(defaultTarget);
        } else {
            int mid = (upper + lower) / 2;
            int label = entries.get(mid).label;
            builder
                    .append(testVar)
                    .i32Const(label)
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED);
            var cond = builder.conditional();
            generateBinarySearch(entries, mid + 1, upper, cond.getThenBlock().builder(), defaultTarget, testVar);
            generateBinarySearch(entries, lower, mid, cond.getElseBlock().builder(), defaultTarget, testVar);
        }
    }

    private void translateSwitchToWasmSwitch(SwitchStatement statement,
            WasmInstructionList defaultTarget, WasmInstructionList[] clauseTargets, int min, int max) {
        if (min != 0) {
            builder.i32Const(min);
            builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB);
        }

        var wasmSwitch = builder.switch_(defaultTarget);

        var expandedTargets = new WasmInstructionList[max - min + 1];
        for (int i = 0; i < statement.getClauses().size(); i++) {
            var clause = statement.getClauses().get(i);
            for (int label : clause.getConditions()) {
                expandedTargets[label - min] = clauseTargets[i];
            }
        }

        for (var target : expandedTargets) {
            wasmSwitch.getTargets().add(target != null ? target : defaultTarget);
        }
    }

    static class TableEntry {
        final int label;
        final WasmInstructionList target;

        TableEntry(int label, WasmInstructionList target) {
            this.label = label;
            this.target = target;
        }
    }

    @Override
    public void visit(WhileStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        ++blockLevel;
        var oldBreakTarget = currentBreakTarget;
        var oldContinueTarget = currentContinueTarget;
        currentBreakTarget = statement;
        currentContinueTarget = statement;

        var outerBlockBuilder = builder.block();
        var loopBuilder = outerBlockBuilder.loop();
        breakTargets.put(statement, outerBlockBuilder.list);
        continueTargets.put(statement, loopBuilder.list);

        if (statement.getCondition() != null) {
            accept(statement.getCondition(), loopBuilder);
            loopBuilder
                    .negate()
                    .branch(outerBlockBuilder);
        }

        var oldBuilder = builder;
        builder = loopBuilder;
        for (var s : statement.getBody()) {
            s.acceptVisitor(this);
        }
        builder.breakTo(loopBuilder.list);
        builder = oldBuilder;

        breakTargets.remove(statement);
        continueTargets.remove(statement);
        currentBreakTarget = oldBreakTarget;
        currentContinueTarget = oldContinueTarget;
        --blockLevel;
    }

    @Override
    public void visit(BlockStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        ++blockLevel;
        var blockBuilder = builder.block();
        if (statement.getId() != null) {
            breakTargets.put(statement, blockBuilder.list);
        }

        var oldBuilder = builder;
        builder = blockBuilder;
        for (var s : statement.getBody()) {
            s.acceptVisitor(this);
        }
        builder = oldBuilder;

        if (statement.getId() != null) {
            breakTargets.remove(statement);
        }
        --blockLevel;
    }

    @Override
    public void visit(BreakStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        var target = statement.getTarget();
        if (target == null) {
            target = currentBreakTarget;
        }
        var wasmTarget = breakTargets.get(target);
        if (wasmTarget != null) {
            builder.breakTo(wasmTarget);
        } else {
            builder.unreachable();
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        var target = statement.getTarget();
        if (target == null) {
            target = currentContinueTarget;
        }
        var wasmTarget = continueTargets.get(target);
        if (wasmTarget != null) {
            builder.breakTo(wasmTarget);
        } else {
            builder.unreachable();
        }
    }

    @Override
    public void visit(ReturnStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        if (statement.getResult() != null) {
            var returnType = context.typeMapper().mapType(currentMethod.getReturnType());
            accept(statement.getResult(), builder, returnType);
        }

        if (blockLevel == 0 || returnBlock == null) {
            builder.return_();
        } else {
            builder.breakTo(returnBlock.getBody());
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        accept(statement.getException());
        builder.throw_(context.getExceptionTag());
    }

    @Override
    public void visit(InitClassStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        if (needsClassInitializer(statement.getClassName())) {
            var pointer = context.classInfoProvider().getClassInfo(statement.getClassName()).getInitializerPointer();
            builder.getGlobal(pointer);
            builder.callReference(context.functionTypes().of(null));
        }
    }

    private boolean needsClassInitializer(String className) {
        if (className.equals(StringInternPool.class.getName())) {
            return false;
        }
        return context.classInfoProvider().getClassInfo(className).getInitializerPointer() != null;
    }

    @Override
    public void visit(TryCatchStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
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
        Collections.reverse(tryCatchStatements);

        ++blockLevel;

        var oldBuilder = builder;

        var tryInsn = new WasmTryInstruction();
        var buildersToClose = new ArrayList<WasmInstructionBuilder>();
        Consumer<WasmType> typeConsumer = tryInsn::setType;
        builder = tryInsn.getBody().builder();
        for (var part : statement.getProtectedBody()) {
            part.acceptVisitor(this);
        }
        var catchClause = new WasmCatchClause(context.getExceptionTag());
        tryInsn.getCatches().add(catchClause);
        buildersToClose.add(builder);

        var throwableType = (WasmType.Reference) context.typeMapper().mapType(ValueType.object("java.lang.Throwable"));
        var catchClauseBuilder = catchClause.builder();
        catchClauseBuilder.typeInference.typeStack.add(throwableType);

        WasmInstructionList innerInsnList = tryInsn.getBody();
        for (var tryCatchStmt : tryCatchStatements) {
            var exType = tryCatchStmt.getExceptionType();
            if (exType == null) {
                exType = "java.lang.Throwable";
            }
            var exceptionType = (WasmType.Reference) context.classInfoProvider().getClassInfo(exType).getType();
            typeConsumer.accept(exceptionType);
            var catchBlock = new WasmBlockInstruction(false);
            catchBlock.getBody().add(innerInsnList.getBreakTarget());

            catchClauseBuilder.castBranch(WasmCastCondition.SUCCESS, throwableType, exceptionType,
                    catchBlock.getBody());

            builder = catchBlock.getBody().builder();
            builder.typeInference.typeStack.add(exceptionType);
            if (tryCatchStmt.getExceptionVariable() != null) {
                builder.setLocal(function.getLocalVariables()
                        .get(tryCatchStmt.getExceptionVariable() - firstVariable));
            } else {
                builder.drop();
            }
            for (var part : tryCatchStmt.getHandler()) {
                part.acceptVisitor(this);
            }
            buildersToClose.add(builder);
            innerInsnList = catchBlock.getBody();
            typeConsumer = t -> catchBlock.setType(t != null ? t.asBlock() : null);
        }
        catchClauseBuilder.throw_(context.getExceptionTag());

        for (var i = 0; i < buildersToClose.size(); i++) {
            buildersToClose.get(i).breakTo(innerInsnList);
        }

        builder = oldBuilder;
        builder.add(innerInsnList.getBreakTarget());

        --blockLevel;
    }

    @Override
    public void visit(GotoPartStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        accept(statement.getObjectRef());
        builder.call(context.functions().forStaticMethod(async ? MONITOR_ENTER : MONITOR_ENTER_SYNC));
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        if (builder.isTerminating()) {
            return;
        }
        accept(statement.getObjectRef());
        builder.call(context.functions().forStaticMethod(async ? MONITOR_EXIT : MONITOR_EXIT_SYNC));
    }

    private void generateInvocation(InvocationExpr expr) {
        var intrinsic = context.intrinsics().get(expr.getMethod());
        if (intrinsic != null) {
            var intrinsicContext = compatIntrinsicVisitor.intrinsicContext;
            var resultExpr = intrinsic.apply(expr, intrinsicContext);
            resultExpr.setLocation(expr.getLocation());
            var tmpList = new WasmInstructionList();
            new WasmExpressionToInstructionConverter(tmpList).convert(resultExpr);
            builder.transferFrom(tmpList.builder());
            return;
        }
        if (expr.getType() == InvocationType.STATIC || expr.getType() == InvocationType.SPECIAL) {
            var method = context.classes().resolve(expr.getMethod());
            var reference = method != null ? method.getReference() : expr.getMethod();
            var function = expr.getType() == InvocationType.STATIC
                    ? context.functions().forStaticMethod(reference)
                    : context.functions().forInstanceMethod(reference);

            var arguments = expr.getArguments();
            for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
                var argument = arguments.get(i);
                var type = expr.getType() == InvocationType.STATIC
                        ? reference.parameterType(i)
                        : i == 0 ? ValueType.object(reference.getClassName()) : reference.parameterType(i - 1);
                var wasmType = context.typeMapper().mapType(type);
                accept(argument, builder, wasmType);
                if (expr.getType() == InvocationType.SPECIAL && i == 0) {
                    forceType(wasmType);
                }
            }

            builder.call(function, isAsyncSplit(expr.getMethod()));
        } else if (expr.getType() == InvocationType.CONSTRUCTOR) {
            var resultType = context.typeMapper().mapType(ValueType.object(expr.getMethod().getClassName()));

            var tmp = tempVars.acquire(resultType);
            allocateObject(expr.getMethod().getClassName(), tmp);
            builder.getLocal(tmp);
            for (var arg : expr.getArguments()) {
                accept(arg);
            }
            builder.call(context.functions().forInstanceMethod(expr.getMethod()), isAsyncSplit(expr.getMethod()));
            builder.getLocal(tmp);
            tempVars.release(tmp);
        } else {
            virtualCall(expr);
        }
    }

    private void generateThrowNpe(WasmInstructionBuilder target) {
        target.call(context.npeMethod());
        target.throw_(context.getExceptionTag());
    }

    private void generateThrowAioobe(WasmInstructionBuilder target) {
        target.call(context.aaiobeMethod());
        target.throw_(context.getExceptionTag());
    }

    private void generateThrowCce(WasmInstructionBuilder target) {
        target.call(context.cceMethod());
        target.throw_(context.getExceptionTag());
    }

    private void forceType(WasmType expectedType) {
        if (builder.isTerminating()) {
            return;
        }
        var types = builder.typeInference.typeStack;
        var top = types.get(types.size() - 1);
        if (top == expectedType || !(top instanceof WasmType.CompositeReference)
                || !(expectedType instanceof WasmType.CompositeReference)) {
            return;
        }
        var actualComposite = ((WasmType.CompositeReference) top).composite;
        var expectedComposite = ((WasmType.CompositeReference) expectedType).composite;
        if (!(actualComposite instanceof WasmStructure) || !(expectedComposite instanceof WasmStructure)) {
            return;
        }

        var actualStruct = (WasmStructure) actualComposite;
        var expectedStruct = (WasmStructure) expectedComposite;
        if (!actualStruct.isSupertypeOf(expectedStruct)) {
            return;
        }

        builder.cast(expectedComposite.getReference());
    }

    private void allocateObject(String className, WasmLocal local) {
        var classInfo = context.classInfoProvider().getClassInfo(className);

        builder
                .structNewDefault(classInfo.getStructure())
                .teeLocal(local)
                .getGlobal(classInfo.getVirtualTablePointer())
                .structSet(classInfo.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET);
    }

    private void virtualCall(InvocationExpr expr) {
        var instanceType = ValueType.object(expr.getMethod().getClassName());
        var instanceWasmType = context.typeMapper().mapType(instanceType);
        accept(expr.getArguments().get(0), builder, instanceWasmType);

        var vtable = context.virtualTables().lookup(expr.getMethod().getClassName());
        if (vtable == null) {
            builder.unreachable();
            return;
        }

        var entry = vtable.entry(expr.getMethod().getDescriptor());
        var nonInterfaceAncestor = vtable.closestNonInterfaceAncestor();
        if (entry == null || nonInterfaceAncestor == null) {
            builder.unreachable();
            return;
        }

        var objectClass = context.classInfoProvider().getClassInfo("java.lang.Object");
        var index = WasmGCClassInfoProvider.VIRTUAL_METHOD_OFFSET + entry.getIndex();
        var instanceClassInfo = context.classInfoProvider().getClassInfo(vtable.getClassName());
        var vtableStruct = instanceClassInfo.getVirtualTableStructure();
        var functionTypeRef = (WasmType.CompositeReference) vtableStruct.getFields().get(index).getUnpackedType();

        if (instanceWasmType instanceof WasmType.CompositeReference) {
            var actualStruct = (WasmStructure) ((WasmType.CompositeReference) instanceWasmType).composite;
            var nonInterfaceClassInfo = context.classInfoProvider().getClassInfo(nonInterfaceAncestor.getClassName());
            var expectedStruct = nonInterfaceClassInfo.getStructure();
            if (actualStruct != expectedStruct && actualStruct.isSupertypeOf(expectedStruct)) {
                builder.cast(expectedStruct);
                instanceWasmType = expectedStruct.getReference();
            }
        }

        forceType(instanceWasmType);
        var instanceValue = valueCache.create(instanceWasmType, builder);

        for (int i = 1; i < expr.getArguments().size(); ++i) {
            var paramType = context.typeMapper().mapType(expr.getMethod().parameterType(i - 1));
            accept(expr.getArguments().get(i), builder, paramType);
        }

        builder
                .append(instanceValue)
                .structGet(objectClass.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET)
                .cast(vtableStruct.getNonNullReference())
                .structGet(vtableStruct, index)
                .callReference((WasmFunctionType) functionTypeRef.composite, isAsyncSplit(expr.getMethod()));

        instanceValue.release();
    }


    private boolean isAsyncSplit(MethodReference method) {
        return asyncSplitMethods.contains(method);
    }

    private boolean isExtern(WasmType.Reference type) {
        if (!(type instanceof WasmType.SpecialReference)) {
            return false;
        }
        return ((WasmType.SpecialReference) type).kind == WasmType.SpecialReferenceKind.EXTERN;
    }
}
