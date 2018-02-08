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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
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
import org.teavm.ast.Mangling;
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
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
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
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTableEntry;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.ShadowStack;

class WasmGenerationVisitor implements StatementVisitor, ExprVisitor {
    private static FieldReference tagField = new FieldReference(RuntimeClass.class.getName(), "tag");
    private static final int SWITCH_TABLE_THRESHOLD = 256;
    private WasmGenerationContext context;
    private WasmClassGenerator classGenerator;
    private WasmTypeInference typeInference;
    private WasmFunction function;
    private int firstVariable;
    private IdentifiedStatement currentContinueTarget;
    private IdentifiedStatement currentBreakTarget;
    private Map<IdentifiedStatement, WasmBlock> breakTargets = new HashMap<>();
    private Map<IdentifiedStatement, WasmBlock> continueTargets = new HashMap<>();
    private Set<WasmBlock> usedBlocks = new HashSet<>();
    private List<Deque<WasmLocal>> temporaryVariablesByType = new ArrayList<>();
    private WasmLocal stackVariable;
    private BinaryWriter binaryWriter;
    WasmExpression result;

    WasmGenerationVisitor(WasmGenerationContext context, WasmClassGenerator classGenerator,
            BinaryWriter binaryWriter, WasmFunction function, int firstVariable) {
        this.context = context;
        this.classGenerator = classGenerator;
        this.binaryWriter = binaryWriter;
        this.function = function;
        this.firstVariable = firstVariable;
        int typeCount = WasmType.values().length;
        for (int i = 0; i < typeCount; ++i) {
            temporaryVariablesByType.add(new ArrayDeque<>());
        }
        typeInference = new WasmTypeInference(context);
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
                        WasmCall call = new WasmCall(Mangling.mangleMethod(method), false);

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
                WasmCall call = new WasmCall(Mangling.mangleMethod(method), false);

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
                accept(expr.getOperand());
                break;
        }
    }

    private WasmExpression generateArrayLength(WasmExpression array) {
        int sizeOffset = classGenerator.getFieldOffset(new FieldReference(RuntimeArray.class.getName(), "size"));

        WasmExpression ptr = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                array, new WasmInt32Constant(sizeOffset));
        ptr.setLocation(array.getLocation());

        WasmExpression length = new WasmLoadInt32(4, ptr, WasmInt32Subtype.INT32);
        length.setLocation(array.getLocation());
        return length;
    }

    @Override
    public void visit(AssignmentStatement statement) {
        Expr left = statement.getLeftValue();
        if (left == null) {
            accept(statement.getRightValue());
            result.acceptVisitor(typeInference);
            if (typeInference.getResult() != null) {
                result = new WasmDrop(result);
                result.setLocation(statement.getLocation());
            }
        } else if (left instanceof VariableExpr) {
            VariableExpr varExpr = (VariableExpr) left;
            WasmLocal local = function.getLocalVariables().get(varExpr.getIndex() - firstVariable);
            accept(statement.getRightValue());
            result = new WasmSetLocal(local, result);
            result.setLocation(statement.getLocation());
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
        ValueType type = context.getFieldType(field);
        accept(value);

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
        result = (WasmExpression) resultExpr;
        result.setLocation(location);
    }

    private void storeArrayItem(SubscriptExpr leftValue, Expr rightValue) {
        WasmExpression ptr = getArrayElementPointer(leftValue);
        accept(rightValue);

        switch (leftValue.getType()) {
            case BYTE:
                result = new WasmStoreInt32(1, ptr, result, WasmInt32Subtype.INT8);
                break;
            case SHORT:
                result = new WasmStoreInt32(2, ptr, result, WasmInt32Subtype.INT16);
                break;
            case CHAR:
                result = new WasmStoreInt32(2, ptr, result, WasmInt32Subtype.UINT16);
                break;
            case INT:
            case OBJECT:
                result = new WasmStoreInt32(4, ptr, result, WasmInt32Subtype.INT32);
                break;
            case LONG:
                result = new WasmStoreInt64(8, ptr, result, WasmInt64Subtype.INT64);
                break;
            case FLOAT:
                result = new WasmStoreFloat32(4, ptr, result);
                break;
            case DOUBLE:
                result = new WasmStoreFloat64(8, ptr, result);
                break;
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
        WasmBlock block = new WasmBlock(false);
        for (Statement part : statement.getSequence()) {
            accept(part);
            if (result != null) {
                block.getBody().add(result);
            }
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
        WasmConditional conditional = new WasmConditional(forCondition(result));

        for (Statement part : statement.getConsequent()) {
            accept(part);
            if (result != null) {
                conditional.getThenBlock().getBody().add(result);
            }
        }
        for (Statement part : statement.getAlternative()) {
            accept(part);
            if (result != null) {
                conditional.getElseBlock().getBody().add(result);
            }
        }
        result = conditional;
    }

    @Override
    public void visit(VariableExpr expr) {
        result = new WasmGetLocal(function.getLocalVariables().get(expr.getIndex() - firstVariable));
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
        accept(expr.getArray());
        WasmExpression array = result;

        accept(expr.getIndex());
        WasmExpression index = result;

        int size = -1;
        switch (expr.getType()) {
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

        int base = classGenerator.getClassSize(RuntimeArray.class.getName());
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

            for (Statement part : clause.getBody()) {
                accept(part);
                if (result != null) {
                    caseBlock.getBody().add(result);
                }
            }
            wrapper = caseBlock;
        }

        defaultBlock.getBody().add(wrapper);
        for (Statement part : statement.getDefaultClause()) {
            accept(part);
            if (result != null) {
                defaultBlock.getBody().add(result);
            }
        }
        WasmBlock defaultTarget = wrapper;
        wrapper = defaultBlock;

        if ((long) max - (long) min >= SWITCH_TABLE_THRESHOLD) {
            translateSwitchToBinarySearch(statement, condition, initialWrapper, defaultTarget, targets);
        } else {
            translateSwitchToWasmSwitch(statement, condition, initialWrapper, defaultTarget, targets, min, max);
        }

        breakTargets.remove(statement);
        currentBreakTarget = oldBreakTarget;

        result = wrapper;
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

        WasmLocal conditionVar = getTemporary(WasmType.INT32);
        initialWrapper.getBody().add(new WasmSetLocal(conditionVar, condition));

        generateBinarySearch(entries, 0, entries.size() - 1, initialWrapper, defaultTarget, conditionVar);
    }

    private void generateBinarySearch(List<TableEntry> entries, int lower, int upper, WasmBlock consumer,
            WasmBlock defaultTarget, WasmLocal conditionVar) {
        if (upper - lower == 0) {
            int label = entries.get(lower).label;
            WasmExpression condition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ,
                    new WasmGetLocal(conditionVar), new WasmInt32Constant(label));
            WasmConditional conditional = new WasmConditional(condition);
            consumer.getBody().add(conditional);

            conditional.getThenBlock().getBody().add(new WasmBreak(entries.get(lower).target));
            conditional.getElseBlock().getBody().add(new WasmBreak(defaultTarget));
        } else if (upper - lower <= 0) {
            consumer.getBody().add(new WasmBreak(defaultTarget));
        } else {
            int mid = (upper + lower) / 2;
            int label = entries.get(mid).label;
            WasmExpression condition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED,
                    new WasmGetLocal(conditionVar), new WasmInt32Constant(label));
            WasmConditional conditional = new WasmConditional(condition);
            consumer.getBody().add(conditional);

            generateBinarySearch(entries, mid + 1, upper, conditional.getThenBlock(), defaultTarget, conditionVar);
            generateBinarySearch(entries, lower, mid, conditional.getElseBlock(), defaultTarget, conditionVar);
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
        if (min > 0) {
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

        for (Statement part : statement.getBody()) {
            accept(part);
            if (result != null) {
                loop.getBody().add(result);
            }
        }
        loop.getBody().add(new WasmBreak(loop));

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
        if (expr.getMethod().getClassName().equals(ShadowStack.class.getName())) {
            switch (expr.getMethod().getName()) {
                case "allocStack":
                    generateAllocStack(expr.getArguments().get(0));
                    return;
                case "releaseStack":
                    generateReleaseStack();
                    return;
                case "registerGCRoot":
                    generateRegisterGcRoot(expr.getArguments().get(0), expr.getArguments().get(1));
                    return;
                case "removeGCRoot":
                    generateRemoveGcRoot(expr.getArguments().get(0));
                    return;
                case "registerCallSite":
                    generateRegisterCallSite(expr.getArguments().get(0));
                    return;
                case "getExceptionHandlerId":
                    generateGetHandlerId();
                    return;
            }
        }

        WasmIntrinsic intrinsic = context.getIntrinsic(expr.getMethod());
        if (intrinsic != null) {
            result = intrinsic.apply(expr, intrinsicManager);
            return;
        }

        if (expr.getType() == InvocationType.STATIC || expr.getType() == InvocationType.SPECIAL) {
            String methodName = Mangling.mangleMethod(expr.getMethod());

            WasmCall call = new WasmCall(methodName);
            if (context.getImportedMethod(expr.getMethod()) != null) {
                call.setImported(true);
            }
            for (Expr argument : expr.getArguments()) {
                accept(argument);
                call.getArguments().add(result);
            }
            result = call;
        } else if (expr.getType() == InvocationType.CONSTRUCTOR) {
            WasmBlock block = new WasmBlock(false);
            block.setType(WasmType.INT32);

            WasmLocal tmp = getTemporary(WasmType.INT32);
            block.getBody().add(new WasmSetLocal(tmp, allocateObject(expr.getMethod().getClassName(),
                    expr.getLocation())));

            String methodName = Mangling.mangleMethod(expr.getMethod());
            WasmCall call = new WasmCall(methodName);
            call.getArguments().add(new WasmGetLocal(tmp));
            for (Expr argument : expr.getArguments()) {
                accept(argument);
                call.getArguments().add(result);
            }
            block.getBody().add(call);

            block.getBody().add(new WasmGetLocal(tmp));
            releaseTemporary(tmp);

            result = block;
        } else {
            accept(expr.getArguments().get(0));
            WasmExpression instance = result;
            WasmBlock block = new WasmBlock(false);
            block.setType(WasmGeneratorUtil.mapType(expr.getMethod().getReturnType()));

            WasmLocal instanceVar = getTemporary(WasmType.INT32);
            block.getBody().add(new WasmSetLocal(instanceVar, instance));
            instance = new WasmGetLocal(instanceVar);

            int vtableOffset = classGenerator.getClassSize(RuntimeClass.class.getName());
            VirtualTableEntry vtableEntry = context.getVirtualTableProvider().lookup(expr.getMethod());
            if (vtableEntry == null) {
                result = new WasmUnreachable();
                return;
            }
            WasmExpression methodIndex = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                    getReferenceToClass(instance), new WasmInt32Constant(vtableEntry.getIndex() * 4 + vtableOffset));
            methodIndex = new WasmLoadInt32(4, methodIndex, WasmInt32Subtype.INT32);

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

            block.getBody().add(call);
            releaseTemporary(instanceVar);
            result = block;
        }
    }

    private void generateAllocStack(Expr sizeExpr) {
        if (stackVariable != null) {
            throw new IllegalStateException("Call to ShadowStack.allocStack must be done only once");
        }
        stackVariable = getTemporary(WasmType.INT32);
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

        int offset = classGenerator.getFieldOffset(new FieldReference(WasmRuntime.class.getName(), "stack"));
        WasmExpression oldValue = new WasmGetLocal(stackVariable);
        oldValue = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB, oldValue,
                new WasmInt32Constant(4));
        result = new WasmStoreInt32(4, new WasmInt32Constant(offset), oldValue, WasmInt32Subtype.INT32);
    }

    private void generateRegisterCallSite(Expr callSiteExpr) {
        if (stackVariable == null) {
            throw new IllegalStateException("Call to ShadowStack.registerCallSite must be dominated by "
                    + "Mutator.allocStack");
        }

        callSiteExpr.acceptVisitor(this);
        WasmExpression callSite = result;

        result = new WasmStoreInt32(4, new WasmGetLocal(stackVariable), callSite, WasmInt32Subtype.INT32);
    }

    private void generateGetHandlerId() {
        if (stackVariable == null) {
            throw new IllegalStateException("Call to ShadowStack.getHandlerId must be dominated by "
                    + "Mutator.allocStack");
        }

        result = new WasmLoadInt32(4, new WasmGetLocal(stackVariable), WasmInt32Subtype.INT32);
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

        for (Statement part : statement.getBody()) {
            accept(part);
            if (result != null) {
                block.getBody().add(result);
            }
        }

        if (statement.getId() != null) {
            breakTargets.remove(statement);
        }

        result = block;
    }

    @Override
    public void visit(QualificationExpr expr) {
        WasmExpression address = getAddress(expr.getQualified(), expr.getField(), expr.getLocation());

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
        result = new WasmBreak(wasmTarget);
        result.setLocation(statement.getLocation());
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
        result.setLocation(statement.getLocation());
    }

    @Override
    public void visit(NewExpr expr) {
        result = allocateObject(expr.getConstructedClass(), expr.getLocation());
    }

    private WasmExpression allocateObject(String className, TextLocation location) {
        int tag = classGenerator.getClassPointer(ValueType.object(className));
        String allocName = Mangling.mangleMethod(new MethodReference(Allocator.class, "allocate",
                RuntimeClass.class, Address.class));
        WasmCall call = new WasmCall(allocName);
        call.getArguments().add(new WasmInt32Constant(tag));
        call.setLocation(location);
        return call;
    }

    @Override
    public void visit(NewArrayExpr expr) {
        ValueType type = expr.getType();

        int classPointer = classGenerator.getClassPointer(ValueType.arrayOf(type));
        String allocName = Mangling.mangleMethod(new MethodReference(Allocator.class, "allocateArray",
                RuntimeClass.class, int.class, Address.class));
        WasmCall call = new WasmCall(allocName);
        call.getArguments().add(new WasmInt32Constant(classPointer));
        accept(expr.getLength());
        call.getArguments().add(result);
        call.setLocation(expr.getLocation());

        result = call;
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

        int classPointer = classGenerator.getClassPointer(ValueType.arrayOf(type));
        String allocName = Mangling.mangleMethod(new MethodReference(Allocator.class, "allocateMultiArray",
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
        result = new WasmReturn(result);
        result.setLocation(statement.getLocation());
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        accept(expr.getExpr());

        if (expr.getType() instanceof ValueType.Object) {
            ValueType.Object cls = (ValueType.Object) expr.getType();
            List<TagRegistry.Range> ranges = context.getTagRegistry().getRanges(cls.getClassName());
            ranges.sort(Comparator.comparingInt(range -> range.lower));

            WasmBlock block = new WasmBlock(false);
            block.setType(WasmType.INT32);
            block.setLocation(expr.getLocation());

            WasmLocal tagVar = getTemporary(WasmType.INT32);
            int tagOffset = classGenerator.getFieldOffset(tagField);
            WasmExpression tagPtr = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                    getReferenceToClass(result), new WasmInt32Constant(tagOffset));
            block.getBody().add(new WasmSetLocal(tagVar, new WasmLoadInt32(4, tagPtr, WasmInt32Subtype.INT32)));

            WasmExpression lowerThanMinCond = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                    new WasmGetLocal(tagVar), new WasmInt32Constant(ranges.get(0).lower));
            WasmBranch lowerThanMin = new WasmBranch(lowerThanMinCond, block);
            lowerThanMin.setResult(new WasmInt32Constant(0));
            block.getBody().add(new WasmDrop(lowerThanMin));

            WasmExpression upperThanMaxCond = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED,
                    new WasmGetLocal(tagVar), new WasmInt32Constant(ranges.get(ranges.size() - 1).upper));
            WasmBranch upperThanMax = new WasmBranch(upperThanMaxCond, block);
            upperThanMax.setResult(new WasmInt32Constant(0));
            block.getBody().add(new WasmDrop(upperThanMax));

            for (int i = 1; i < ranges.size(); ++i) {
                WasmExpression upperThanExcluded = new WasmIntBinary(WasmIntType.INT32,
                        WasmIntBinaryOperation.GT_SIGNED, new WasmGetLocal(tagVar),
                        new WasmInt32Constant(ranges.get(i - 1).upper));
                WasmConditional conditional = new WasmConditional(upperThanExcluded);
                WasmExpression lowerThanExcluded = new WasmIntBinary(WasmIntType.INT32,
                        WasmIntBinaryOperation.LT_SIGNED, new WasmGetLocal(tagVar),
                        new WasmInt32Constant(ranges.get(i).lower));

                WasmBranch branch = new WasmBranch(lowerThanExcluded, block);
                branch.setResult(new WasmInt32Constant(0));
                conditional.getThenBlock().getBody().add(new WasmDrop(branch));

                block.getBody().add(conditional);
            }

            block.getBody().add(new WasmInt32Constant(1));
            releaseTemporary(tagVar);

            result = block;
        } else if (expr.getType() instanceof ValueType.Array) {
            throw new UnsupportedOperationException();
        } else {
            throw new AssertionError();
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        WasmBlock block = new WasmBlock(false);
        block.setLocation(statement.getLocation());
        accept(statement.getException());
        block.getBody().add(result);

        block.getBody().add(new WasmUnreachable());

        result = block;
    }

    @Override
    public void visit(CastExpr expr) {
        accept(expr.getValue());
    }

    @Override
    public void visit(InitClassStatement statement) {
        if (classGenerator.hasClinit(statement.getClassName())) {
            result = new WasmCall(Mangling.mangleInitializer(statement.getClassName()));
            result.setLocation(statement.getLocation());
        } else {
            result = null;
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
        WasmBlock block = new WasmBlock(false);
        for (Statement bodyPart : statement.getProtectedBody()) {
            accept(bodyPart);
            if (result != null) {
                block.getBody().add(result);
            }
        }
        result = block;
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

            WasmIntBinaryOperation negatedOp = negate(binary.getOperation());
            if (negatedOp != null) {
                return new WasmIntBinary(binary.getType(), negatedOp, binary.getFirst(), binary.getSecond());
            }
        } else if (expr instanceof WasmFloatBinary) {
            WasmFloatBinary binary = (WasmFloatBinary) expr;
            WasmFloatBinaryOperation negatedOp = negate(binary.getOperation());
            if (negatedOp != null) {
                return new WasmFloatBinary(binary.getType(), negatedOp, binary.getFirst(), binary.getSecond());
            }
        }

        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.XOR, expr, new WasmInt32Constant(1));
    }

    private boolean isOne(WasmExpression expression) {
        return expression instanceof WasmInt32Constant && ((WasmInt32Constant) expression).getValue() == 1;
    }

    private boolean isZero(WasmExpression expression) {
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

    private WasmIntBinaryOperation negate(WasmIntBinaryOperation op) {
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

    private WasmFloatBinaryOperation negate(WasmFloatBinaryOperation op) {
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
    };

    private WasmLocal getTemporary(WasmType type) {
        Deque<WasmLocal> stack = temporaryVariablesByType.get(type.ordinal());
        WasmLocal variable = stack.pollFirst();
        if (variable == null) {
            variable = new WasmLocal(type);
            function.add(variable);
        }
        return variable;
    }

    private void releaseTemporary(WasmLocal variable) {
        Deque<WasmLocal> stack = temporaryVariablesByType.get(variable.getType().ordinal());
        stack.push(variable);
    }

    private WasmExpression getReferenceToClass(WasmExpression instance) {
        WasmExpression classIndex = new WasmLoadInt32(4, instance, WasmInt32Subtype.INT32);
        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, classIndex,
                new WasmInt32Constant(3));
    }
}
