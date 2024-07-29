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

import java.util.List;
import org.teavm.ast.ArrayType;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.Expr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.SubscriptExpr;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.backend.wasm.generate.common.methods.BaseWasmGenerationVisitor;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmArraySet;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmReferencesEqual;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;

public class WasmGCGenerationVisitor extends BaseWasmGenerationVisitor {
    private WasmGCGenerationContext context;
    private WasmGCGenerationUtil generationUtil;

    public WasmGCGenerationVisitor(WasmGCGenerationContext context, WasmFunction function,
            int firstVariable, boolean async) {
        super(context, function, firstVariable, async);
        this.context = context;
        generationUtil = new WasmGCGenerationUtil(context.classInfoProvider(), tempVars);
    }

    @Override
    protected boolean isManaged() {
        return false;
    }

    @Override
    protected boolean isManagedCall(MethodReference method) {
        return false;
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
        var setThrowable = new WasmSetGlobal(context.exceptionGlobal(), expression);
        setThrowable.setLocation(location);
        target.add(setThrowable);

        var result = new WasmThrow(context.getExceptionTag());
        result.setLocation(location);
        target.add(result);
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        accept(expr.getArray());
        result = unwrapArray(result);
        result.setLocation(expr.getLocation());
    }

    private WasmExpression unwrapArray(WasmExpression array) {
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
    protected WasmExpression storeArrayItem(WasmExpression array, WasmExpression index, WasmExpression value,
            ArrayType type) {
        array.acceptVisitor(typeInference);
        var arrayRefType = (WasmType.CompositeReference) typeInference.getResult();
        var arrayType = (WasmArray) arrayRefType.composite;
        return new WasmArraySet(arrayType, array, index, value);
    }

    @Override
    protected void storeField(Expr qualified, FieldReference field, Expr value, TextLocation location) {
        if (qualified == null) {
            accept(value);
            var wasmValue = result;
            var global = context.classInfoProvider().getStaticFieldLocation(field);
            var result = new WasmSetGlobal(global, wasmValue);
            result.setLocation(location);
            resultConsumer.add(result);
        } else {
            accept(qualified);
            var target = result;
            accept(value);
            var wasmValue = result;

            target.acceptVisitor(typeInference);
            var type = (WasmType.CompositeReference) typeInference.getResult();
            var struct = (WasmStructure) type.composite;

            var fieldIndex = context.classInfoProvider().getFieldIndex(field);

            var expr = new WasmStructSet(struct, target, fieldIndex, wasmValue);
            expr.setLocation(location);
            resultConsumer.add(expr);
        }
    }

    @Override
    protected WasmExpression stringLiteral(String s) {
        var stringConstant = context.strings().getStringConstant(s);
        return new WasmGetGlobal(stringConstant.global);
    }

    @Override
    protected WasmExpression classLiteral(ValueType type) {
        var classConstant = context.classInfoProvider().getClassInfo(type);
        return new WasmGetGlobal(classConstant.getPointer());
    }

    @Override
    protected WasmExpression nullLiteral() {
        return new WasmNullConstant(WasmType.Reference.STRUCT);
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
                    accept(expr.getFirstOperand());
                    var first = result;
                    accept(expr.getSecondOperand());
                    var second = result;
                    result = new WasmReferencesEqual(first, second);
                    result.setLocation(expr.getLocation());
                    return;
                }
                case NOT_EQUALS:
                    accept(expr.getFirstOperand());
                    var first = result;
                    accept(expr.getSecondOperand());
                    var second = result;
                    result = new WasmReferencesEqual(first, second);
                    result = new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ, result);
                    result.setLocation(expr.getLocation());
                    return;
                default:
                    break;
            }
        }
        super.visit(expr);
    }

    @Override
    protected WasmExpression generateVirtualCall(WasmLocal instance, MethodReference method,
            List<WasmExpression> arguments) {
        var vtable = context.virtualTables().lookup(method.getClassName());
        if (vtable != null) {
            vtable = vtable.findMethodContainer(method.getDescriptor());
        }
        if (vtable == null) {
            return new WasmUnreachable();
        }
        method = new MethodReference(vtable.getClassName(), method.getDescriptor());

        arguments.get(0).acceptVisitor(typeInference);
        var instanceType = (WasmType.CompositeReference) typeInference.getResult();
        var instanceStruct = (WasmStructure) instanceType.composite;

        WasmExpression classRef = new WasmStructGet(instanceStruct, new WasmGetLocal(instance),
                WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);
        var index = context.classInfoProvider().getVirtualMethodIndex(method);
        var vtableType = (WasmType.CompositeReference) instanceStruct.getFields()
                .get(WasmGCClassInfoProvider.CLASS_FIELD_OFFSET).asUnpackedType();
        var vtableStruct = (WasmStructure) vtableType.composite;
        var expectedVtableStruct = context.classInfoProvider().getClassInfo(vtable.getClassName())
                .getVirtualTableStructure();
        if (expectedVtableStruct != vtableStruct) {
            classRef = new WasmCast(classRef, expectedVtableStruct.getReference());
        }

        var functionRef = new WasmStructGet(expectedVtableStruct, classRef, index);
        var functionTypeRef = (WasmType.CompositeReference) expectedVtableStruct.getFields()
                .get(index).asUnpackedType();
        var invoke = new WasmCallReference(functionRef, (WasmFunctionType) functionTypeRef.composite);
        invoke.getArguments().addAll(arguments);
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
                WasmGCClassInfoProvider.CLASS_FIELD_OFFSET, new WasmGetGlobal(classInfo.getPointer()));
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
    protected void allocateArray(ValueType itemType, WasmExpression length, TextLocation location, WasmLocal local,
            List<WasmExpression> target) {
        generationUtil.allocateArray(itemType, length, location, local, target);
    }

    @Override
    protected WasmExpression allocateMultiArray(List<WasmExpression> target, ValueType itemType,
            List<WasmExpression> dimensions, TextLocation location) {
        return null;
    }

    @Override
    protected WasmExpression generateInstanceOf(WasmExpression expression, ValueType type) {
        context.classInfoProvider().getClassInfo(type);
        var supertypeCall = new WasmCall(context.supertypeFunctions().getIsSupertypeFunction(type));
        var classRef = new WasmStructGet(
                context.standardClasses().objectClass().getStructure(),
                expression,
                WasmGCClassInfoProvider.CLASS_FIELD_OFFSET
        );
        supertypeCall.getArguments().add(classRef);
        return supertypeCall;
    }

    @Override
    protected WasmExpression generateCast(WasmExpression value, WasmType targetType) {
        return new WasmCast(value, (WasmType.Reference) targetType);
    }

    @Override
    protected boolean needsClassInitializer(String className) {
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
    protected WasmExpression peekException() {
        return new WasmGetGlobal(context.exceptionGlobal());
    }

    @Override
    protected void catchException(TextLocation location, List<WasmExpression> target, WasmLocal local) {
        var type = context.classInfoProvider().getClassInfo("java.lang.Throwable").getType();
        if (local != null) {
            var save = new WasmSetLocal(local, new WasmGetGlobal(context.exceptionGlobal()));
            save.setLocation(location);
            target.add(save);
        }

        var erase = new WasmSetGlobal(context.exceptionGlobal(), new WasmNullConstant(type));
        erase.setLocation(location);
        target.add(erase);
    }

    @Override
    protected WasmType mapType(ValueType type) {
        return context.typeMapper().mapType(type).asUnpackedType();
    }

    @Override
    public void visit(SubscriptExpr expr) {
        accept(expr.getArray());
        var arrayData = unwrapArray(result);
        arrayData.acceptVisitor(typeInference);
        var arrayTypeRef = (WasmType.CompositeReference) typeInference.getResult();
        var arrayType = (WasmArray) arrayTypeRef.composite;

        accept(expr.getIndex());
        var index = result;

        result = new WasmArrayGet(arrayType, arrayData, index);
        result.setLocation(expr.getLocation());
    }

    @Override
    public void visit(InvocationExpr expr) {
        result = invocation(expr, null, false);
    }

    @Override
    public void visit(QualificationExpr expr) {
        if (expr.getQualified() == null) {
            var global = context.classInfoProvider().getStaticFieldLocation(expr.getField());
            result = new WasmGetGlobal(global);
            result.setLocation(expr.getLocation());
        } else {
            accept(expr.getQualified());
            var target = result;

            target.acceptVisitor(typeInference);
            var type = (WasmType.CompositeReference) typeInference.getResult();
            var struct = (WasmStructure) type.composite;

            var fieldIndex = context.classInfoProvider().getFieldIndex(expr.getField());

            result = new WasmStructGet(struct, target, fieldIndex);
            result.setLocation(expr.getLocation());
        }
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
}
