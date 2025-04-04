/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.generate.gc.methods;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.teavm.ast.ArrayFromDataExpr;
import org.teavm.ast.ArrayType;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.CastExpr;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.Expr;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.InvocationType;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.SubscriptExpr;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.VariableExpr;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.gc.PreciseTypeInference;
import org.teavm.backend.wasm.generate.ExpressionCache;
import org.teavm.backend.wasm.generate.TemporaryVariablePool;
import org.teavm.backend.wasm.generate.common.methods.BaseWasmGenerationVisitor;
import org.teavm.backend.wasm.generate.gc.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.generate.gc.strings.WasmGCStringProvider;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmArraySet;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmCastBranch;
import org.teavm.backend.wasm.model.expression.WasmCastCondition;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmInt64Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIsNull;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat32;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat64;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmLoadInt64;
import org.teavm.backend.wasm.model.expression.WasmNullBranch;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmReferencesEqual;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmSignedType;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat32;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat64;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.model.expression.WasmStoreInt64;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.model.expression.WasmTest;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;

public class WasmGCGenerationVisitor extends BaseWasmGenerationVisitor {
    private WasmGCGenerationContext context;
    private WasmGCGenerationUtil generationUtil;
    private WasmType expectedType;
    private PreciseTypeInference types;
    private boolean compactMode;

    public WasmGCGenerationVisitor(WasmGCGenerationContext context, MethodReference currentMethod,
            WasmFunction function, int firstVariable, boolean async, PreciseTypeInference types) {
        super(context, currentMethod, function, firstVariable, async);
        this.context = context;
        generationUtil = new WasmGCGenerationUtil(context.classInfoProvider());
        this.types = types;
    }

    public void setCompactMode(boolean compactMode) {
        this.compactMode = compactMode;
    }

    @Override
    protected void accept(Expr expr) {
        accept(expr, null);
    }

    @Override
    protected void accept(Expr expr, WasmType type) {
        var previousExpectedType = expectedType;
        expectedType = type;
        super.accept(expr);
        expectedType = previousExpectedType;
    }

    @Override
    protected void acceptWithType(Expr expr, ValueType type) {
        accept(expr, mapType(type));
    }

    @Override
    protected boolean isManaged() {
        return true;
    }

    @Override
    protected boolean needsCallSiteId() {
        return false;
    }

    @Override
    protected boolean isManagedCall(MethodReference method) {
        return false;
    }

    @Override
    public void visit(VariableExpr expr) {
        super.visit(expr);
        if (compactMode && expr.getVariableIndex() == 0) {
            var receiverType = context.typeMapper().mapType(ValueType.object(currentMethod.getClassName()));
            var cast = new WasmCast(result, (WasmType.Reference) receiverType);
            cast.setLocation(expr.getLocation());
            result = cast;
        }
    }

    @Override
    protected void generateThrowNPE(TextLocation location, List<WasmExpression> target) {
        generateThrow(new WasmCall(context.npeMethod()), location, target);
    }

    @Override
    protected void generateThrowAIOOBE(TextLocation location, List<WasmExpression> target) {
        generateThrow(new WasmCall(context.aaiobeMethod()), location, target);
    }

    @Override
    protected void generateThrowCCE(TextLocation location, List<WasmExpression> target) {
        generateThrow(new WasmCall(context.cceMethod()), location, target);
    }

    @Override
    protected void generateThrow(WasmExpression expression, TextLocation location, List<WasmExpression> target) {
        var result = new WasmThrow(context.getExceptionTag());
        result.getArguments().add(expression);
        result.setLocation(location);
        target.add(result);
    }

    @Override
    protected WasmExpression unwrapArray(WasmExpression array) {
        array.acceptVisitor(typeInference);
        var arrayType = (WasmType.CompositeReference) typeInference.getResult();
        var arrayStruct = (WasmStructure) arrayType.composite;
        return new WasmStructGet(arrayStruct, array, WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
    }

    @Override
    protected WasmExpression generateArrayLength(WasmExpression array) {
        return new WasmArrayLength(array);
    }

    @Override
    protected WasmExpression storeArrayItem(WasmExpression array, WasmExpression index, Expr value,
            ArrayType type) {
        array.acceptVisitor(typeInference);
        var arrayRefType = (WasmType.CompositeReference) typeInference.getResult();
        var arrayType = (WasmArray) arrayRefType.composite;
        accept(value, arrayType.getElementType().asUnpackedType());
        var wasmValue = result;
        return new WasmArraySet(arrayType, array, index, wasmValue);
    }

    @Override
    protected void storeField(Expr qualified, FieldReference field, Expr value, TextLocation location) {
        WasmExpression expr;
        if (qualified == null) {
            var global = context.classInfoProvider().getStaticFieldLocation(field);
            accept(value, global.getType());
            var wasmValue = result;
            expr = new WasmSetGlobal(global, wasmValue);
        } else {
            acceptWithType(qualified, ValueType.object(field.getClassName()));
            var target = result;
            if (context.classInfoProvider().getClassInfo(field.getClassName()).isHeapStructure()) {
                expr = storeHeapField(target, field, value);
            } else {
                expr = storeNormalField(target, field, value);
            }
        }
        expr.setLocation(location);
        resultConsumer.add(expr);
    }

    private WasmExpression storeNormalField(WasmExpression target, FieldReference field, Expr value) {
        target.acceptVisitor(typeInference);
        var type = (WasmType.CompositeReference) typeInference.getResult();
        var struct = (WasmStructure) type.composite;
        var fieldIndex = context.classInfoProvider().getFieldIndex(field);
        if (fieldIndex >= 0) {
            accept(value, struct.getFields().get(fieldIndex).getUnpackedType());
            return new WasmStructSet(struct, target, fieldIndex, result);
        } else {
            accept(value);
            return result;
        }
    }

    private WasmExpression storeHeapField(WasmExpression target, FieldReference field, Expr value) {
        var cls = context.classes().get(field.getClassName());
        var type = cls.getField(field.getFieldName()).getType();
        var offset = context.classInfoProvider().getHeapFieldOffset(field);
        accept(value, context.typeMapper().mapType(type));
        var wasmValue = result;
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    return new WasmStoreInt32(1, target, wasmValue, WasmInt32Subtype.INT8, offset);
                case CHARACTER:
                    return new WasmStoreInt32(2, target, wasmValue, WasmInt32Subtype.UINT16, offset);
                case SHORT:
                    return new WasmStoreInt32(2, target, wasmValue, WasmInt32Subtype.INT16, offset);
                case INTEGER:
                    return new WasmStoreInt32(4, target, wasmValue, WasmInt32Subtype.INT32, offset);
                case LONG:
                    return new WasmStoreInt64(8, target, wasmValue, WasmInt64Subtype.INT32, offset);
                case FLOAT:
                    return new WasmStoreFloat32(4, target, wasmValue, offset);
                case DOUBLE:
                    return new WasmStoreFloat64(8, target, wasmValue, offset);
            }
        }
        return new WasmStoreInt32(4, target, wasmValue, WasmInt32Subtype.INT32, offset);
    }

    @Override
    protected WasmExpression stringLiteral(String s) {
        var stringConstant = context.strings().getStringConstant(s);
        return new WasmGetGlobal(stringConstant.global);
    }

    @Override
    protected WasmExpression classLiteral(ValueType type) {
        if (type instanceof ValueType.Array) {
            var itemType = ((ValueType.Array) type).getItemType();
            if (!(itemType instanceof ValueType.Primitive)) {
                var degree = 0;
                while (type instanceof ValueType.Array) {
                    type = ((ValueType.Array) type).getItemType();
                    ++degree;
                }
                WasmExpression result = new WasmGetGlobal(context.classInfoProvider().getClassInfo(type).getPointer());
                while (degree-- > 0) {
                    result = new WasmCall(context.classInfoProvider().getGetArrayClassFunction(), result);
                }
                return result;
            }
        }
        var classConstant = context.classInfoProvider().getClassInfo(type);
        return new WasmGetGlobal(classConstant.getPointer());
    }

    @Override
    protected WasmExpression nullLiteral(Expr expr) {
        var type = expectedType;
        if (type == WasmType.INT32) {
            return new WasmInt32Constant(0);
        }
        if (expr.getVariableIndex() >= 0) {
            var javaType = types.typeOf(expr.getVariableIndex());
            if (javaType != null) {
                type = mapType(javaType.valueType);
            }
        }
        return new WasmNullConstant(type instanceof WasmType.Reference
                ? (WasmType.Reference) type
                : context.classInfoProvider().getClassInfo("java.lang.Object").getType());
    }

    @Override
    protected WasmExpression nullLiteral(WasmType type) {
        return new WasmNullConstant((WasmType.Reference) type);
    }

    @Override
    protected WasmExpression genIsNull(WasmExpression value) {
        return new WasmIsNull(value);
    }

    @Override
    protected WasmExpression nullCheck(Expr value, TextLocation location) {
        var block = new WasmBlock(false);
        block.setLocation(location);

        accept(value);
        if (result instanceof WasmUnreachable) {
            return result;
        }
        result.acceptVisitor(typeInference);
        block.setType(typeInference.getResult());
        var check = new WasmNullBranch(WasmNullCondition.NOT_NULL, result, block);
        block.getBody().add(check);

        var callSiteId = generateCallSiteId(location);
        callSiteId.generateRegister(block.getBody(), location);
        generateThrowNPE(location, block.getBody());
        callSiteId.generateThrow(block.getBody(), location);

        return block;
    }

    @Override
    protected CallSiteIdentifier generateCallSiteId(TextLocation location) {
        return new SimpleCallSite();
    }

    @Override
    public void visit(BinaryExpr expr) {
        if (expr.getType() == null) {
            switch (expr.getOperation()) {
                case EQUALS: {
                    isReferenceEq(expr);
                    result.setLocation(expr.getLocation());
                    return;
                }
                case NOT_EQUALS:
                    isReferenceEq(expr);
                    result = new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ, result);
                    result.setLocation(expr.getLocation());
                    return;
                default:
                    break;
            }
        }
        super.visit(expr);
    }

    private void isReferenceEq(BinaryExpr expr) {
        if (isNull(expr.getFirstOperand())) {
            accept(expr.getSecondOperand());
            result.acceptVisitor(typeInference);
            if (typeInference.getResult() == WasmType.INT32) {
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ, result,
                        new WasmInt32Constant(0));
            } else {
                result = new WasmIsNull(result);
            }
        } else if (isNull(expr.getSecondOperand())) {
            accept(expr.getFirstOperand());
            result.acceptVisitor(typeInference);
            if (typeInference.getResult() == WasmType.INT32) {
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ, result,
                        new WasmInt32Constant(0));
            } else {
                result = new WasmIsNull(result);
            }
        } else {
            accept(expr.getFirstOperand());
            var first = result;
            accept(expr.getSecondOperand());
            var second = result;
            first.acceptVisitor(typeInference);
            if (typeInference.getResult() == WasmType.INT32) {
                result = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ, first, second);
            } else {
                result = new WasmReferencesEqual(first, second);
            }
        }
    }

    private boolean isNull(Expr expr) {
        if (!(expr instanceof ConstantExpr)) {
            return false;
        }
        return ((ConstantExpr) expr).getValue() == null;
    }

    @Override
    protected WasmExpression generateVirtualCall(WasmLocal instance, MethodReference method,
            List<WasmExpression> arguments) {
        var vtable = context.virtualTables().lookup(method.getClassName());
        if (vtable == null) {
            return new WasmUnreachable();
        }

        var entry = vtable.entry(method.getDescriptor());
        var nonInterfaceAncestor = vtable.closestNonInterfaceAncestor();
        if (entry == null || nonInterfaceAncestor == null) {
            return new WasmUnreachable();
        }

        WasmExpression classRef = new WasmStructGet(context.standardClasses().objectClass().getStructure(),
                new WasmGetLocal(instance), WasmGCClassInfoProvider.VT_FIELD_OFFSET);
        var index = WasmGCClassInfoProvider.VIRTUAL_METHOD_OFFSET + entry.getIndex();
        var expectedInstanceClassInfo = context.classInfoProvider().getClassInfo(vtable.getClassName());
        var expectedInstanceClassStruct = context.classInfoProvider().getClassInfo(
                nonInterfaceAncestor.getClassName()).getStructure();
        var vtableStruct = expectedInstanceClassInfo.getVirtualTableStructure();
        classRef = new WasmCast(classRef, vtableStruct.getNonNullReference());

        var functionRef = new WasmStructGet(vtableStruct, classRef, index);
        var functionTypeRef = (WasmType.CompositeReference) vtableStruct.getFields().get(index).getUnpackedType();
        var invoke = new WasmCallReference(functionRef, (WasmFunctionType) functionTypeRef.composite);
        WasmExpression instanceRef = new WasmGetLocal(instance);
        var instanceType = (WasmType.CompositeReference) instance.getType();
        var instanceStruct = (WasmStructure) instanceType.composite;
        if (!expectedInstanceClassStruct.isSupertypeOf(instanceStruct)) {
            instanceRef = new WasmCast(instanceRef, expectedInstanceClassStruct.getNonNullReference());
        }

        invoke.getArguments().add(instanceRef);
        invoke.getArguments().addAll(arguments.subList(1, arguments.size()));
        return invoke;
    }

    @Override
    protected void allocateObject(String className, TextLocation location, WasmLocal local,
            List<WasmExpression> target) {
        var classInfo = context.classInfoProvider().getClassInfo(className);
        var block = new WasmBlock(false);
        block.setType(classInfo.getType());
        var targetVar = local;
        if (targetVar == null) {
            targetVar = tempVars.acquire(classInfo.getType());
        }

        var structNew = new WasmSetLocal(targetVar, new WasmStructNewDefault(classInfo.getStructure()));
        structNew.setLocation(location);
        target.add(structNew);

        var initClassField = new WasmStructSet(classInfo.getStructure(), new WasmGetLocal(targetVar),
                WasmGCClassInfoProvider.VT_FIELD_OFFSET, new WasmGetGlobal(classInfo.getVirtualTablePointer()));
        initClassField.setLocation(location);
        target.add(initClassField);

        if (local == null) {
            var getLocal = new WasmGetLocal(targetVar);
            getLocal.setLocation(location);
            target.add(getLocal);
            tempVars.release(targetVar);
        }
    }

    @Override
    public void visit(ArrayFromDataExpr expr) {
        var wasmArrayType = (WasmType.CompositeReference) mapType(ValueType.arrayOf(expr.getType()));
        var wasmArrayStruct = (WasmStructure) wasmArrayType.composite;
        var wasmArrayDataType = (WasmType.CompositeReference) wasmArrayStruct.getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        var wasmArray = (WasmArray) wasmArrayDataType.composite;

        var array = generationUtil.allocateArrayWithElements(expr.getType(), () -> {
            var items = new ArrayList<WasmExpression>();
            for (int i = 0; i < expr.getData().size(); ++i) {
                accept(expr.getData().get(i), wasmArray.getElementType().asUnpackedType());
                items.add(result);
            }
            return items;
        });
        array.setLocation(expr.getLocation());

        result = array;
    }

    @Override
    public void visit(NewArrayExpr expr) {
        accept(expr.getLength(), WasmType.INT32);
        var function = context.classInfoProvider().getArrayConstructor(expr.getType());
        var call = new WasmCall(function, classLiteral(expr.getType()), result);
        call.setLocation(expr.getLocation());
        result = call;
    }

    @Override
    protected WasmExpression allocateMultiArray(List<WasmExpression> target, ValueType arrayType,
            Supplier<List<WasmExpression>> dimensions, TextLocation location) {
        var args = new ArrayList<WasmExpression>();
        var dimensionsValue = dimensions.get();
        args.add(classLiteral(((ValueType.Array) arrayType).getItemType()));
        args.addAll(dimensionsValue);
        var function = context.classInfoProvider().getMultiArrayConstructor(dimensionsValue.size());
        var call = new WasmCall(function, args.toArray(new WasmExpression[0]));
        call.setLocation(location);
        return call;
    }

    @Override
    protected WasmExpression generateInstanceOf(WasmExpression expression, ValueType type) {
        context.classInfoProvider().getClassInfo(type);
        var block = new WasmBlock(false);
        block.setType(WasmType.INT32);
        var supertypeCall = new WasmCall(context.supertypeFunctions().getIsSupertypeFunction(type));
        var vtRef = new WasmStructGet(
                context.standardClasses().objectClass().getStructure(),
                expression,
                WasmGCClassInfoProvider.VT_FIELD_OFFSET
        );
        var classRef = new WasmStructGet(
                context.standardClasses().objectClass().getVirtualTableStructure(),
                vtRef,
                WasmGCClassInfoProvider.CLASS_FIELD_OFFSET
        );
        var classClass = context.standardClasses().classClass().getType();
        var classRefCached = exprCache.create(classRef, classClass, expression.getLocation(), block.getBody());
        supertypeCall.getArguments().add(classRefCached.expr());
        supertypeCall.getArguments().add(classRefCached.expr());
        classRefCached.release();
        block.getBody().add(supertypeCall);
        return block;
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        var type = expr.getType();
        if (canCastNatively(type)) {
            var wasmType = context.classInfoProvider().getClassInfo(type).getStructure().getNonNullReference();
            acceptWithType(expr.getExpr(), type);
            var wasmValue = result;
            result.acceptVisitor(typeInference);

            result = new WasmTest(wasmValue, wasmType);
            result.setLocation(expr.getLocation());
        } else {
            super.visit(expr);
        }
    }

    @Override
    public void visit(CastExpr expr) {
        acceptWithType(expr.getValue(), expr.getTarget());
        if (expr.getTarget() instanceof ValueType.Object) {
            var className = ((ValueType.Object) expr.getTarget()).getClassName();
            if (context.classInfoProvider().getClassInfo(className).isHeapStructure()) {
                accept(expr.getValue(), WasmType.INT32);
                return;
            }
        }

        result.acceptVisitor(typeInference);
        var sourceType = (WasmType.Reference) typeInference.getResult();
        if (sourceType == null) {
            return;
        }

        var targetType = (WasmType.Reference) context.typeMapper().mapType(expr.getTarget());
        WasmStructure targetStruct = null;
        if (targetType instanceof WasmType.CompositeReference) {
            var targetComposite = ((WasmType.CompositeReference) targetType).composite;
            if (targetComposite instanceof WasmStructure) {
                targetStruct = (WasmStructure) targetComposite;
            }
        }

        var canInsertCast = true;
        if (targetStruct != null && sourceType instanceof WasmType.CompositeReference) {
            var sourceComposite = (WasmType.CompositeReference) sourceType;
            if (!sourceType.isNullable()) {
                sourceType = sourceComposite.composite.getReference();
            }
            var sourceStruct = (WasmStructure) sourceComposite.composite;
            if (targetStruct.isSupertypeOf(sourceStruct)) {
                canInsertCast = false;
            } else if (!sourceStruct.isSupertypeOf(targetStruct)) {
                var block = new WasmBlock(false);
                block.setType(targetType);
                block.setLocation(expr.getLocation());
                block.getBody().add(new WasmDrop(result));
                block.getBody().add(new WasmNullConstant(targetType));
                result = block;
                return;
            }
        }

        if (!expr.isWeak() && context.isStrict()) {
            result.acceptVisitor(typeInference);

            var block = new WasmBlock(false);
            block.setLocation(expr.getLocation());
            if (canCastNatively(expr.getTarget())) {
                block.setType(targetType);
                if (!canInsertCast) {
                    return;
                }
                block.getBody().add(new WasmCastBranch(WasmCastCondition.SUCCESS, result, sourceType,
                        targetType, block));
                result = block;
            } else {
                block.setType(sourceType);
                var nonNullValue = new WasmNullBranch(WasmNullCondition.NULL, result, block);
                nonNullValue.setResult(new WasmNullConstant(sourceType));
                var valueToCast = exprCache.create(nonNullValue, sourceType, expr.getLocation(), block.getBody());

                var supertypeCall = generateInstanceOf(valueToCast.expr(), expr.getTarget());
                var breakIfPassed = new WasmBranch(supertypeCall, block);
                breakIfPassed.setResult(valueToCast.expr());
                block.getBody().add(new WasmDrop(breakIfPassed));

                result = block;
                if (canInsertCast) {
                    var cast = new WasmCast(result, targetType);
                    cast.setLocation(expr.getLocation());
                    result = cast;
                }
            }
            generateThrowCCE(expr.getLocation(), block.getBody());
        } else if (canInsertCast) {
            result = new WasmCast(result, targetType);
            result.setLocation(expr.getLocation());
        }
    }

    private boolean canCastNatively(ValueType type) {
        if (type instanceof ValueType.Array) {
            return true;
        }
        var className = ((ValueType.Object) type).getClassName();
        var cls = context.classes().get(className);
        if (cls == null) {
            return false;
        }
        return !cls.hasModifier(ElementModifier.INTERFACE);
    }

    @Override
    protected boolean needsClassInitializer(String className) {
        if (className.equals(StringInternPool.class.getName())) {
            return false;
        }
        return context.classInfoProvider().getClassInfo(className).getInitializerPointer() != null;
    }

    @Override
    protected WasmExpression generateClassInitializer(String className, TextLocation location) {
        var pointer = context.classInfoProvider().getClassInfo(className).getInitializerPointer();
        var result = new WasmCallReference(new WasmGetGlobal(pointer),
                context.functionTypes().of(null));
        result.setLocation(location);
        return result;
    }

    @Override
    protected void checkExceptionType(TryCatchStatement tryCatch, WasmLocal exceptionVar, List<WasmExpression> target,
            WasmBlock targetBlock) {
        var wasmType = context.classInfoProvider().getClassInfo(tryCatch.getExceptionType()).getType();
        var wasmSourceType = context.classInfoProvider().getClassInfo("java.lang.Throwable").getType();
        var br = new WasmCastBranch(WasmCastCondition.SUCCESS, new WasmGetLocal(exceptionVar),
                wasmSourceType, wasmType, targetBlock);
        target.add(br);
    }

    @Override
    protected WasmType mapType(ValueType type) {
        return context.typeMapper().mapType(type);
    }

    @Override
    public void visit(SubscriptExpr expr) {
        accept(expr.getArray());
        var arrayData = result;
        arrayData.acceptVisitor(typeInference);
        var arrayTypeRef = (WasmType.CompositeReference) typeInference.getResult();
        var arrayType = (WasmArray) arrayTypeRef.composite;

        accept(expr.getIndex());
        var index = result;

        var arrayGet = new WasmArrayGet(arrayType, arrayData, index);
        switch (expr.getType()) {
            case BYTE:
                arrayGet.setSignedType(WasmSignedType.SIGNED);
                break;
            case SHORT:
                arrayGet.setSignedType(WasmSignedType.SIGNED);
                break;
            case CHAR:
                arrayGet.setSignedType(WasmSignedType.UNSIGNED);
                break;
            default:
                break;
        }
        arrayGet.setLocation(expr.getLocation());
        result = arrayGet;
        if (expr.getType() == ArrayType.OBJECT && expr.getVariableIndex() >= 0) {
            var targetType = types.typeOf(expr.getVariableIndex());
            if (targetType != null) {
                var wasmTargetType = (WasmType.Reference) mapType(targetType.valueType);
                if (!isExtern(wasmTargetType)) {
                    result = new WasmCast(result, (WasmType.Reference) mapType(targetType.valueType));
                }
            }
        }
    }

    private boolean isExtern(WasmType.Reference type) {
        if (!(type instanceof WasmType.SpecialReference)) {
            return false;
        }
        return ((WasmType.SpecialReference) type).kind == WasmType.SpecialReferenceKind.EXTERN;
    }

    @Override
    public void visit(InvocationExpr expr) {
        result = invocation(expr, null, false);
    }

    @Override
    protected WasmExpression invocation(InvocationExpr expr, List<WasmExpression> resultConsumer, boolean willDrop) {
        if (expr.getType() == InvocationType.SPECIAL || expr.getType() == InvocationType.STATIC) {
            var intrinsic = context.intrinsics().get(expr.getMethod());
            if (intrinsic != null) {
                var resultExpr = intrinsic.apply(expr, intrinsicContext);
                resultExpr.setLocation(expr.getLocation());
                if (resultConsumer != null) {
                    if (willDrop) {
                        var drop = new WasmDrop(resultExpr);
                        drop.setLocation(expr.getLocation());
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
        }
        return super.invocation(expr, resultConsumer, willDrop);
    }

    @Override
    protected WasmExpression mapFirstArgumentForCall(WasmExpression argument, WasmFunction function,
            MethodReference method) {
        return forceType(argument, function.getType().getParameterTypes().get(0));
    }

    @Override
    protected WasmExpression forceType(WasmExpression expression, ValueType type) {
        return forceType(expression, mapType(currentMethod.getReturnType()));
    }

    private WasmExpression forceType(WasmExpression expression, WasmType expectedType) {
        expression.acceptVisitor(typeInference);
        var actualType = typeInference.getResult();
        if (actualType == expectedType || !(actualType instanceof WasmType.CompositeReference)
                || !(expectedType instanceof WasmType.CompositeReference)) {
            return expression;
        }
        var actualComposite = ((WasmType.CompositeReference) actualType).composite;
        var expectedComposite = ((WasmType.CompositeReference) expectedType).composite;
        if (!(actualComposite instanceof WasmStructure) || !(expectedComposite instanceof WasmStructure)) {
            return expression;
        }

        var actualStruct = (WasmStructure) actualComposite;
        var expectedStruct = (WasmStructure) expectedComposite;
        if (!actualStruct.isSupertypeOf(expectedStruct)) {
            return expression;
        }

        return new WasmCast(expression, expectedComposite.getReference());
    }

    @Override
    public void visit(QualificationExpr expr) {
        if (expr.getQualified() == null) {
            var global = context.classInfoProvider().getStaticFieldLocation(expr.getField());
            result = new WasmGetGlobal(global);
            result.setLocation(expr.getLocation());
        } else {
            acceptWithType(expr.getQualified(), ValueType.object(expr.getField().getClassName()));
            var target = result;

            var classInfo = context.classInfoProvider().getClassInfo(expr.getField().getClassName());
            if (classInfo.isHeapStructure()) {
                var offset = context.classInfoProvider().getHeapFieldOffset(expr.getField());
                var cls = context.classes().get(expr.getField().getClassName());
                var field = cls.getField(expr.getField().getFieldName());
                result = getHeapField(field.getType(), target, offset);
            } else {
                result = getNormalField(expr, target);
            }
            result.setLocation(expr.getLocation());
        }
    }

    private WasmExpression getNormalField(QualificationExpr expr, WasmExpression target) {
        target.acceptVisitor(typeInference);
        var type = (WasmType.CompositeReference) typeInference.getResult();
        if (type == null) {
            return new WasmUnreachable();
        }
        var struct = (WasmStructure) type.composite;
        var fieldIndex = context.classInfoProvider().getFieldIndex(expr.getField());
        if (fieldIndex < 0) {
            return new WasmUnreachable();
        }
        var structGet = new WasmStructGet(struct, target, fieldIndex);
        var cls = context.classes().get(expr.getField().getClassName());
        if (cls != null) {
            var field = cls.getField(expr.getField().getFieldName());
            if (field != null) {
                var fieldType = field.getType();
                if (fieldType instanceof ValueType.Primitive) {
                    switch (((ValueType.Primitive) fieldType).getKind()) {
                        case BOOLEAN:
                            structGet.setSignedType(WasmSignedType.UNSIGNED);
                            break;
                        case BYTE:
                            structGet.setSignedType(WasmSignedType.SIGNED);
                            break;
                        case SHORT:
                            structGet.setSignedType(WasmSignedType.SIGNED);
                            break;
                        case CHARACTER:
                            structGet.setSignedType(WasmSignedType.UNSIGNED);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return structGet;
    }

    private WasmExpression getHeapField(ValueType type, WasmExpression target, int offset) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    return new WasmLoadInt32(1, target, WasmInt32Subtype.INT8, offset);
                case CHARACTER:
                    return new WasmLoadInt32(2, target, WasmInt32Subtype.UINT16, offset);
                case SHORT:
                    return new WasmLoadInt32(2, target, WasmInt32Subtype.INT16, offset);
                case INTEGER:
                    return new WasmLoadInt32(4, target, WasmInt32Subtype.INT32, offset);
                case LONG:
                    return new WasmLoadInt64(8, target, WasmInt64Subtype.INT64, offset);
                case FLOAT:
                    return new WasmLoadFloat32(4, target, offset);
                case DOUBLE:
                    return new WasmLoadFloat64(8, target, offset);
                default:
                    break;
            }
        }
        return new WasmLoadInt32(4, target, WasmInt32Subtype.INT32, offset);
    }

    @Override
    protected WasmType condBlockType(WasmType thenType, WasmType elseType, ConditionalExpr conditional) {
        if (conditional.getVariableIndex() >= 0) {
            var javaType = types.typeOf(conditional.getVariableIndex());
            if (javaType != null) {
                return mapType(javaType.valueType);
            }
        }
        if (conditional.getConsequent().getVariableIndex() >= 0
                && conditional.getConsequent().getVariableIndex() == conditional.getAlternative().getVariableIndex()) {
            var javaType = types.typeOf(conditional.getConsequent().getVariableIndex());
            if (javaType != null) {
                return mapType(javaType.valueType);
            }
        }
        return super.condBlockType(thenType, elseType, conditional);
    }

    private class SimpleCallSite extends CallSiteIdentifier {
        @Override
        public void generateRegister(List<WasmExpression> consumer, TextLocation location) {
        }

        @Override
        public void checkHandlerId(List<WasmExpression> target, TextLocation location) {
        }

        @Override
        public void generateThrow(List<WasmExpression> target, TextLocation location) {
        }
    }

    private WasmGCIntrinsicContext intrinsicContext = new WasmGCIntrinsicContext() {
        @Override
        public WasmExpression generate(Expr expr) {
            accept(expr);
            return result;
        }

        @Override
        public ClassLoader classLoader() {
            return context.classLoader();
        }

        @Override
        public WasmModule module() {
            return context.module();
        }

        @Override
        public WasmFunctionTypes functionTypes() {
            return context.functionTypes();
        }

        @Override
        public PreciseTypeInference types() {
            return types;
        }

        @Override
        public BaseWasmFunctionRepository functions() {
            return context.functions();
        }

        @Override
        public ClassHierarchy hierarchy() {
            return context.hierarchy();
        }

        @Override
        public WasmGCTypeMapper typeMapper() {
            return context.typeMapper();
        }

        @Override
        public WasmGCClassInfoProvider classInfoProvider() {
            return context.classInfoProvider();
        }

        @Override
        public TemporaryVariablePool tempVars() {
            return tempVars;
        }

        @Override
        public ExpressionCache exprCache() {
            return exprCache;
        }

        @Override
        public WasmGCNameProvider names() {
            return context.names();
        }

        @Override
        public WasmGCStringProvider strings() {
            return context.strings();
        }

        @Override
        public WasmTag exceptionTag() {
            return context.getExceptionTag();
        }

        @Override
        public String entryPoint() {
            return context.entryPoint();
        }

        @Override
        public Diagnostics diagnostics() {
            return context.diagnostics();
        }

        @Override
        public MethodReference currentMethod() {
            return currentMethod;
        }

        @Override
        public void addToInitializer(Consumer<WasmFunction> initializerContributor) {
            context.addToInitializer(initializerContributor);
        }
    };
}
