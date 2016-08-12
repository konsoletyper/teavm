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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import org.teavm.interop.Address;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTableEntry;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.wasm.WasmRuntime;
import org.teavm.wasm.intrinsics.WasmIntrinsic;
import org.teavm.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.wasm.model.WasmFunction;
import org.teavm.wasm.model.WasmLocal;
import org.teavm.wasm.model.WasmType;
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
import org.teavm.wasm.model.expression.WasmIndirectCall;
import org.teavm.wasm.model.expression.WasmInt32Constant;
import org.teavm.wasm.model.expression.WasmInt32Subtype;
import org.teavm.wasm.model.expression.WasmInt64Constant;
import org.teavm.wasm.model.expression.WasmInt64Subtype;
import org.teavm.wasm.model.expression.WasmIntBinary;
import org.teavm.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.wasm.model.expression.WasmIntType;
import org.teavm.wasm.model.expression.WasmLoadFloat32;
import org.teavm.wasm.model.expression.WasmLoadFloat64;
import org.teavm.wasm.model.expression.WasmLoadInt32;
import org.teavm.wasm.model.expression.WasmLoadInt64;
import org.teavm.wasm.model.expression.WasmReturn;
import org.teavm.wasm.model.expression.WasmSetLocal;
import org.teavm.wasm.model.expression.WasmStoreFloat32;
import org.teavm.wasm.model.expression.WasmStoreFloat64;
import org.teavm.wasm.model.expression.WasmStoreInt32;
import org.teavm.wasm.model.expression.WasmStoreInt64;
import org.teavm.wasm.model.expression.WasmSwitch;

class WasmGenerationVisitor implements StatementVisitor, ExprVisitor {
    private WasmGenerationContext context;
    private WasmClassGenerator classGenerator;
    private WasmFunction function;
    private int firstVariable;
    private IdentifiedStatement currentContinueTarget;
    private IdentifiedStatement currentBreakTarget;
    private Map<IdentifiedStatement, WasmBlock> breakTargets = new HashMap<>();
    private Map<IdentifiedStatement, WasmBlock> continueTargets = new HashMap<>();
    private Set<WasmBlock> usedBlocks = new HashSet<>();
    private int temporaryInt32 = -1;
    WasmExpression result;

    WasmGenerationVisitor(WasmGenerationContext context, WasmClassGenerator classGenerator,
            WasmFunction function, int firstVariable) {
        this.context = context;
        this.classGenerator = classGenerator;
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
                        Class<?> type = convertType(expr.getType());
                        MethodReference method = new MethodReference(WasmRuntime.class, "remainder", type, type, type);
                        WasmCall call = new WasmCall(WasmMangling.mangleMethod(method), false);
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
                Class<?> type = convertType(expr.getType());
                MethodReference method = new MethodReference(WasmRuntime.class, "compare", type, type, int.class);
                WasmCall call = new WasmCall(WasmMangling.mangleMethod(method), false);
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
                expr.getOperand().acceptVisitor(this);
                result = generateArrayLength(result);
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

    private WasmExpression generateArrayLength(WasmExpression array) {
        int sizeOffset = classGenerator.getFieldOffset(new FieldReference(RuntimeArray.class.getName(), "size"));

        WasmIntBinary ptr = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                array, new WasmInt32Constant(sizeOffset));
        return new WasmLoadInt32(4, ptr, WasmInt32Subtype.INT32);
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
        } else if (left instanceof QualificationExpr) {
            QualificationExpr lhs = (QualificationExpr) left;
            storeField(lhs.getQualified(), lhs.getField(), statement.getRightValue());
        } else if (left instanceof SubscriptExpr) {
            SubscriptExpr lhs = (SubscriptExpr) left;
            storeArrayItem(lhs.getArray(), lhs.getIndex(), statement.getRightValue());
        } else {
            throw new UnsupportedOperationException("This expression is not supported yet");
        }
    }

    private void storeField(Expr qualified, FieldReference field, Expr value) {
        WasmExpression address = getAddress(qualified, field);
        ValueType type = context.getFieldType(field);
        value.acceptVisitor(this);
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    result = new WasmStoreInt32(1, address, result, WasmInt32Subtype.INT8);
                    break;
                case SHORT:
                    result = new WasmStoreInt32(2, address, result, WasmInt32Subtype.INT16);
                    break;
                case CHARACTER:
                    result = new WasmStoreInt32(2, address, result, WasmInt32Subtype.UINT16);
                    break;
                case INTEGER:
                    result = new WasmStoreInt32(4, address, result, WasmInt32Subtype.INT32);
                    break;
                case LONG:
                    result = new WasmStoreInt64(8, address, result, WasmInt64Subtype.INT64);
                    break;
                case FLOAT:
                    result = new WasmStoreFloat32(4, address, result);
                    break;
                case DOUBLE:
                    result = new WasmStoreFloat64(8, address, result);
                    break;
            }
        } else {
            result = new WasmStoreInt32(4, address, result, WasmInt32Subtype.INT32);
        }
    }

    private void storeArrayItem(Expr array, Expr index, Expr rightValue) {
        WasmExpression ptr = getArrayElementPointer(array, index);
        rightValue.acceptVisitor(this);
        result = new WasmStoreInt32(4, ptr, result, WasmInt32Subtype.INT32);
    }

    @Override
    public void visit(ConditionalExpr expr) {
        expr.getCondition().acceptVisitor(this);
        WasmConditional conditional = new WasmConditional(forCondition(result));
        expr.getConsequent().acceptVisitor(this);
        conditional.getThenBlock().getBody().add(result);
        expr.getAlternative().acceptVisitor(this);
        conditional.getElseBlock().getBody().add(result);
        result = conditional;
    }

    @Override
    public void visit(SequentialStatement statement) {
        WasmBlock block = new WasmBlock(false);
        for (Statement part : statement.getSequence()) {
            part.acceptVisitor(this);
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
        } else {
            throw new IllegalArgumentException("Constant unsupported: " + expr.getValue());
        }
    }

    @Override
    public void visit(ConditionalStatement statement) {
        statement.getCondition().acceptVisitor(this);
        WasmConditional conditional = new WasmConditional(forCondition(result));
        for (Statement part : statement.getConsequent()) {
            part.acceptVisitor(this);
            if (result != null) {
                conditional.getThenBlock().getBody().add(result);
            }
        }
        for (Statement part : statement.getAlternative()) {
            part.acceptVisitor(this);
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
        WasmExpression ptr = getArrayElementPointer(expr.getArray(), expr.getIndex());
        result = new WasmLoadInt32(4, ptr, WasmInt32Subtype.INT32);
    }

    private WasmExpression getArrayElementPointer(Expr arrayExpr, Expr indexExpr) {
        arrayExpr.acceptVisitor(this);
        WasmExpression array = result;
        indexExpr.acceptVisitor(this);
        WasmExpression index = result;

        classGenerator.addClass(RuntimeArray.class.getName());
        int base = classGenerator.getClassSize(RuntimeArray.class.getName());
        array = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, array, new WasmInt32Constant(base));
        index = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, index, new WasmInt32Constant(2));

        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, array, index);
    }

    @Override
    public void visit(SwitchStatement statement) {
        WasmBlock defaultBlock = new WasmBlock(false);

        int min = statement.getClauses().stream()
                .flatMapToInt(clause -> Arrays.stream(clause.getConditions()))
                .min().orElse(0);
        int max = statement.getClauses().stream()
                .flatMapToInt(clause -> Arrays.stream(clause.getConditions()))
                .max().orElse(0);

        breakTargets.put(statement, defaultBlock);
        IdentifiedStatement oldBreakTarget = currentBreakTarget;
        currentBreakTarget = statement;

        WasmBlock wrapper = new WasmBlock(false);
        statement.getValue().acceptVisitor(this);
        if (min > 0) {
            result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB, result,
                    new WasmInt32Constant(min));
        }

        WasmSwitch wasmSwitch = new WasmSwitch(result, wrapper);
        wrapper.getBody().add(wasmSwitch);
        WasmBlock[] targets = new WasmBlock[max - min + 1];

        for (SwitchClause clause : statement.getClauses()) {
            WasmBlock caseBlock = new WasmBlock(false);
            caseBlock.getBody().add(wrapper);

            for (int condition : clause.getConditions()) {
                targets[condition - min] = wrapper;
            }

            for (Statement part : clause.getBody()) {
                part.acceptVisitor(this);
                if (result != null) {
                    caseBlock.getBody().add(result);
                }
            }
            wrapper = caseBlock;
        }

        defaultBlock.getBody().add(wrapper);
        for (Statement part : statement.getDefaultClause()) {
            part.acceptVisitor(this);
            if (result != null) {
                defaultBlock.getBody().add(result);
            }
        }
        wasmSwitch.setDefaultTarget(wrapper);
        wrapper = defaultBlock;

        for (WasmBlock target : targets) {
            wasmSwitch.getTargets().add(target != null ? target : wasmSwitch.getDefaultTarget());
        }

        breakTargets.remove(statement);
        currentBreakTarget = oldBreakTarget;

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
            loop.getBody().add(new WasmBranch(negate(result), wrapper));
            usedBlocks.add(wrapper);
        }

        for (Statement part : statement.getBody()) {
            part.acceptVisitor(this);
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
        WasmIntrinsic intrinsic = context.getIntrinsic(expr.getMethod());
        if (intrinsic != null) {
            result = intrinsic.apply(expr, intrinsicManager);
            return;
        }

        if (expr.getType() == InvocationType.STATIC || expr.getType() == InvocationType.SPECIAL) {
            String methodName = WasmMangling.mangleMethod(expr.getMethod());

            WasmCall call = new WasmCall(methodName);
            if (context.getImportedMethod(expr.getMethod()) != null) {
                call.setImported(true);
            }
            for (Expr argument : expr.getArguments()) {
                argument.acceptVisitor(this);
                call.getArguments().add(result);
            }
            result = call;
        } else {
            expr.getArguments().get(0).acceptVisitor(this);
            WasmExpression instance = result;
            WasmBlock block = new WasmBlock(false);
            WasmLocal instanceVar = function.getLocalVariables().get(getTemporaryInt32());
            block.getBody().add(new WasmSetLocal(instanceVar, instance));
            instance = new WasmGetLocal(instanceVar);

            VirtualTableEntry vtableEntry = context.getVirtualTableProvider().lookup(expr.getMethod());
            WasmExpression methodIndex = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                    getReferenceToClass(instance), new WasmInt32Constant(vtableEntry.getIndex() * 4
                    + RuntimeClass.VIRTUAL_TABLE_OFFSET));
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
                expr.getArguments().get(i).acceptVisitor(this);
                call.getArguments().add(result);
            }

            block.getBody().add(call);
            result = block;
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
        WasmExpression address = getAddress(expr.getQualified(), expr.getField());

        ValueType type = context.getFieldType(expr.getField());
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    result = new WasmLoadInt32(1, address, WasmInt32Subtype.INT8);
                    break;
                case SHORT:
                    result = new WasmLoadInt32(2, address, WasmInt32Subtype.INT16);
                    break;
                case CHARACTER:
                    result = new WasmLoadInt32(2, address, WasmInt32Subtype.UINT16);
                    break;
                case INTEGER:
                    result = new WasmLoadInt32(4, address, WasmInt32Subtype.INT32);
                    break;
                case LONG:
                    result = new WasmLoadInt64(8, address, WasmInt64Subtype.INT64);
                    break;
                case FLOAT:
                    result = new WasmLoadFloat32(4, address);
                    break;
                case DOUBLE:
                    result = new WasmLoadFloat64(8, address);
                    break;
            }
        } else {
            result = new WasmLoadInt32(4, address, WasmInt32Subtype.INT32);
        }
    }

    private WasmExpression getAddress(Expr qualified, FieldReference field) {
        int offset = classGenerator.getFieldOffset(field);
        if (qualified == null) {
            return new WasmInt32Constant(offset);
        } else {
            qualified.acceptVisitor(this);
            return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, result,
                    new WasmInt32Constant(offset));
        }
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
    public void visit(NewExpr expr) {
        int tag = classGenerator.getClassPointer(expr.getConstructedClass());
        String allocName = WasmMangling.mangleMethod(new MethodReference(Allocator.class, "allocate",
                RuntimeClass.class, Address.class));
        WasmCall call = new WasmCall(allocName);
        call.getArguments().add(new WasmInt32Constant(tag));
        result = call;
    }

    @Override
    public void visit(NewArrayExpr expr) {
        ValueType type = expr.getType();
        int depth = 0;
        while (type instanceof ValueType.Array) {
            ++depth;
            type = ((ValueType.Array) type).getItemType();
        }

        ValueType.Object cls = (ValueType.Object) type;
        int classPointer = classGenerator.getClassPointer(cls.getClassName());
        String allocName = WasmMangling.mangleMethod(new MethodReference(Allocator.class, "allocateArray",
                RuntimeClass.class, int.class, byte.class, Address.class));
        WasmCall call = new WasmCall(allocName);
        call.getArguments().add(new WasmInt32Constant(classPointer));
        expr.getLength().acceptVisitor(this);
        call.getArguments().add(result);
        call.getArguments().add(new WasmInt32Constant(depth));

        result = call;
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
        expr.getExpr().acceptVisitor(this);

        if (expr.getType() instanceof ValueType.Object) {
            ValueType.Object cls = (ValueType.Object) expr.getType();
            List<TagRegistry.Range> ranges = context.getTagRegistry().getRanges(cls.getClassName());
            Collections.sort(ranges, Comparator.comparingInt(range -> range.lower));

            WasmBlock block = new WasmBlock(false);
            WasmLocal tagVar = function.getLocalVariables().get(getTemporaryInt32());
            WasmExpression tagPtr = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                    getReferenceToClass(result), new WasmInt32Constant(RuntimeClass.LOWER_TAG_OFFSET));
            block.getBody().add(new WasmSetLocal(tagVar, new WasmLoadInt32(4, tagPtr, WasmInt32Subtype.INT32)));

            WasmExpression lowerThanMinCond = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                    new WasmGetLocal(tagVar), new WasmInt32Constant(ranges.get(0).lower));
            WasmBranch lowerThanMin = new WasmBranch(lowerThanMinCond, block);
            lowerThanMin.setResult(new WasmInt32Constant(0));
            block.getBody().add(lowerThanMin);

            WasmExpression upperThanMaxCond = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED,
                    new WasmGetLocal(tagVar), new WasmInt32Constant(ranges.get(ranges.size() - 1).upper));
            WasmBranch upperThanMax = new WasmBranch(upperThanMaxCond, block);
            upperThanMax.setResult(new WasmInt32Constant(0));
            block.getBody().add(upperThanMax);

            for (int i = 1; i < ranges.size(); ++i) {
                WasmExpression upperThanExcluded = new WasmIntBinary(WasmIntType.INT32,
                        WasmIntBinaryOperation.GT_SIGNED, new WasmGetLocal(tagVar),
                        new WasmInt32Constant(ranges.get(i - 1).upper));
                WasmConditional conditional = new WasmConditional(upperThanExcluded);
                WasmExpression lowerThanExluded = new WasmIntBinary(WasmIntType.INT32,
                        WasmIntBinaryOperation.LT_SIGNED, new WasmGetLocal(tagVar),
                        new WasmInt32Constant(ranges.get(i).lower));

                WasmBranch branch = new WasmBranch(lowerThanExluded, block);
                branch.setResult(new WasmInt32Constant(0));
                conditional.getThenBlock().getBody().add(branch);

                block.getBody().add(conditional);
            }

            block.getBody().add(new WasmInt32Constant(1));

            result = block;
        } else if (expr.getType() instanceof ValueType.Array) {

        } else {
            throw new AssertionError();
        }
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
        if (hasClinit(statement.getClassName())) {
            result = new WasmCall(WasmMangling.mangleInitializer(statement.getClassName()));
        } else {
            result = null;
        }
    }

    private boolean hasClinit(String className) {
        if (classGenerator.isStructure(className)) {
            return false;
        }
        ClassReader cls = context.getClassSource().get(className);
        if (cls == null) {
            return false;
        }
        return cls.getMethod(new MethodDescriptor("<clinit>", ValueType.VOID)) != null;
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

    private WasmExpression forCondition(WasmExpression expression) {
        if (expression instanceof WasmIntBinary) {
            WasmIntBinary binary = (WasmIntBinary) expression;
            switch (binary.getOperation()) {
                case EQ:
                    if (isZero(binary.getFirst())) {
                        return negate(binary.getSecond());
                    } else if (isZero(binary.getSecond())) {
                        return negate(binary.getFirst());
                    }
                    break;
                case NE:
                    if (isZero(binary.getFirst())) {
                        return binary.getSecond();
                    } else if (isZero(binary.getSecond())) {
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
            expr.acceptVisitor(WasmGenerationVisitor.this);
            return result;
        }
    };

    private int getTemporaryInt32() {
        if (temporaryInt32 < 0) {
            temporaryInt32 = function.getLocalVariables().size();
            function.add(new WasmLocal(WasmType.INT32));
        }
        return temporaryInt32;
    }

    private WasmExpression getReferenceToClass(WasmExpression instance) {
        WasmExpression classIndex = new WasmLoadInt32(4, instance, WasmInt32Subtype.INT32);
        return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL, classIndex,
                new WasmInt32Constant(3));
    }
}
