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
package org.teavm.backend.wasm.intrinsics.gc;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayCopy;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSequence;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmThrow;
import org.teavm.backend.wasm.runtime.gc.WasmGCSupport;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class SystemArrayCopyIntrinsic implements WasmGCIntrinsic {
    private WasmFunction defaultFunction;
    private WasmFunction argsCheckFunction;

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "arraycopy":
                return generateArrayCopy(invocation, context);
            case "doArrayCopy":
                return generateDoArrayCopy(invocation, context);
            default:
                throw new IllegalArgumentException();
        }
    }

    private WasmExpression generateArrayCopy(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var result = tryGenerateSpecialCase(invocation, context);
        if (result == null) {
            tryGenerateSpecialCase(invocation, context);
            var function = getDefaultFunction(context);
            result = new WasmCall(function, context.generate(invocation.getArguments().get(0)),
                    context.generate(invocation.getArguments().get(1)),
                    context.generate(invocation.getArguments().get(2)),
                    context.generate(invocation.getArguments().get(3)),
                    context.generate(invocation.getArguments().get(4)));
        }
        return result;
    }

    private WasmExpression generateDoArrayCopy(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var objStruct = context.classInfoProvider().getClassInfo(Object.class.getName()).getStructure();
        var arrayClsStruct = context.classInfoProvider().getArrayVirtualTableStructure();
        var block = new WasmSequence();

        var source = context.exprCache().create(context.generate(invocation.getArguments().get(0)),
                objStruct.getReference(), invocation.getLocation(), block.getBody());
        WasmExpression sourceCls = new WasmStructGet(objStruct, source.expr(),
                WasmGCClassInfoProvider.VT_FIELD_OFFSET);
        sourceCls = new WasmCast(sourceCls, arrayClsStruct.getNonNullReference());
        var sourceClsCached = context.exprCache().create(sourceCls, arrayClsStruct.getNonNullReference(),
                invocation.getLocation(), block.getBody());
        var copyFunction = new WasmStructGet(arrayClsStruct, sourceClsCached.expr(),
                context.classInfoProvider().getArrayCopyOffset());
        var functionTypeRef = (WasmType.CompositeReference) arrayClsStruct.getFields().get(
                context.classInfoProvider().getArrayCopyOffset()).getUnpackedType();
        var functionType = (WasmFunctionType) functionTypeRef.composite;
        var call = new WasmCallReference(copyFunction, functionType);
        call.getArguments().add(new WasmStructGet(arrayClsStruct, sourceClsCached.expr(),
                WasmGCClassInfoProvider.CLASS_FIELD_OFFSET));
        call.getArguments().add(source.expr());
        call.getArguments().add(context.generate(invocation.getArguments().get(1)));
        call.getArguments().add(context.generate(invocation.getArguments().get(2)));
        call.getArguments().add(context.generate(invocation.getArguments().get(3)));
        call.getArguments().add(context.generate(invocation.getArguments().get(4)));
        block.getBody().add(call);
        source.release();
        sourceClsCached.release();
        return block;
    }

    private WasmExpression tryGenerateSpecialCase(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var sourceArray = invocation.getArguments().get(0);
        var targetArray = invocation.getArguments().get(2);
        if (sourceArray.getVariableIndex() < 0 || targetArray.getVariableIndex() < 0) {
            return null;
        }

        var sourceType = context.types().typeOf(sourceArray.getVariableIndex());
        if (sourceType == null || !(sourceType.valueType instanceof ValueType.Array)) {
            return null;
        }
        var targetType = context.types().typeOf(targetArray.getVariableIndex());
        if (targetType == null || !(targetType.valueType instanceof ValueType.Array)) {
            return null;
        }

        var sourceItemType = ((ValueType.Array) sourceType.valueType).getItemType();
        var targetItemType = ((ValueType.Array) targetType.valueType).getItemType();
        if (sourceItemType != targetItemType
                || !context.hierarchy().isSuperType(targetItemType, sourceItemType, false)) {
            return null;
        }

        var block = new WasmSequence();

        var wasmTargetArrayType = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.arrayOf(targetItemType));
        var wasmTargetArrayStruct = (WasmStructure) wasmTargetArrayType.composite;
        var wasmTargetArrayWrapper = context.generate(invocation.getArguments().get(2));
        var wasmTargetArrayTypeRef = (WasmType.CompositeReference) wasmTargetArrayStruct.getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        if (context.isAsync()) {
            wasmTargetArrayTypeRef = wasmTargetArrayTypeRef.composite.getReference();
        }
        var wasmTargetArray = context.exprCache().create(new WasmStructGet(wasmTargetArrayStruct,
                wasmTargetArrayWrapper, WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET),
                wasmTargetArrayTypeRef, null, block.getBody());
        var wasmTargetIndex = context.exprCache().create(context.generate(invocation.getArguments().get(3)),
                WasmType.INT32, null, block.getBody());
        var wasmSourceArrayType = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.arrayOf(sourceItemType));
        var wasmSourceArrayStruct = (WasmStructure) wasmSourceArrayType.composite;
        var wasmSourceArrayWrapper = context.generate(invocation.getArguments().get(0));
        var wasmSourceArrayTypeRef = (WasmType.CompositeReference) wasmSourceArrayStruct.getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        if (context.isAsync()) {
            wasmSourceArrayTypeRef = wasmSourceArrayTypeRef.composite.getReference();
        }
        var wasmSourceArray = context.exprCache().create(new WasmStructGet(wasmSourceArrayStruct,
                wasmSourceArrayWrapper, WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET),
                wasmSourceArrayTypeRef, null, block.getBody());
        var wasmSourceIndex = context.exprCache().create(context.generate(invocation.getArguments().get(1)),
                WasmType.INT32, null, block.getBody());
        var wasmSize = context.exprCache().create(context.generate(invocation.getArguments().get(4)),
                WasmType.INT32, null, block.getBody());


        block.getBody().add(new WasmCall(
                getArgsCheckFunction(context),
                wasmTargetArray.expr(), wasmTargetIndex.expr(),
                wasmSourceArray.expr(), wasmSourceIndex.expr(),
                wasmSize.expr()
        ));

        block.getBody().add(new WasmArrayCopy(
                (WasmArray) wasmTargetArrayTypeRef.composite, wasmTargetArray.expr(), wasmTargetIndex.expr(),
                (WasmArray) wasmSourceArrayTypeRef.composite, wasmSourceArray.expr(), wasmSourceIndex.expr(),
                wasmSize.expr()
        ));
        wasmTargetArray.release();
        wasmTargetIndex.release();
        wasmSourceArray.release();
        wasmSourceIndex.release();
        wasmSize.release();
        return block;
    }

    private WasmFunction getArgsCheckFunction(WasmGCIntrinsicContext context) {
        if (argsCheckFunction == null) {
            argsCheckFunction = createArgsCheckFunction(context);
        }
        return argsCheckFunction;
    }

    private WasmFunction createArgsCheckFunction(WasmGCIntrinsicContext context) {
        var function = new WasmFunction(context.functionTypes().of(null,
                WasmType.Reference.ARRAY, WasmType.INT32, WasmType.Reference.ARRAY,
                WasmType.INT32, WasmType.INT32));
        function.setName(context.names().topLevel("teavm@checkArrayCopy"));
        context.module().functions.add(function);

        var targetArrayLocal = new WasmLocal(WasmType.Reference.ARRAY, "targetArray");
        var targetArrayIndexLocal = new WasmLocal(WasmType.INT32, "targetIndex");
        var sourceArrayLocal = new WasmLocal(WasmType.Reference.ARRAY, "sourceArray");
        var sourceArrayIndexLocal = new WasmLocal(WasmType.INT32, "sourceIndex");
        var countLocal = new WasmLocal(WasmType.INT32, "count");
        function.add(targetArrayLocal);
        function.add(targetArrayIndexLocal);
        function.add(sourceArrayLocal);
        function.add(sourceArrayIndexLocal);
        function.add(countLocal);

        var block = new WasmBlock(false);
        var targetIndexLessThanZero = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                new WasmGetLocal(targetArrayIndexLocal), new WasmInt32Constant(0));
        block.getBody().add(new WasmBranch(targetIndexLessThanZero, block));
        var sourceIndexLessThanZero = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                new WasmGetLocal(sourceArrayIndexLocal), new WasmInt32Constant(0));
        block.getBody().add(new WasmBranch(sourceIndexLessThanZero, block));
        var countPositive = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED,
                new WasmGetLocal(countLocal), new WasmInt32Constant(0));
        block.getBody().add(new WasmBranch(countPositive, block));

        var targetSize = new WasmArrayLength(new WasmGetLocal(targetArrayLocal));
        var targetIndexLimit = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB,
                targetSize, new WasmGetLocal(countLocal));
        var targetIndexGreaterThanSize = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED,
                new WasmGetLocal(targetArrayIndexLocal), targetIndexLimit);
        block.getBody().add(new WasmBranch(targetIndexGreaterThanSize, block));

        var sourceSize = new WasmArrayLength(new WasmGetLocal(sourceArrayLocal));
        var sourceIndexLimit = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB,
                sourceSize, new WasmGetLocal(countLocal));
        var sourceIndexGreaterThanSize = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED,
                new WasmGetLocal(sourceArrayIndexLocal), sourceIndexLimit);
        block.getBody().add(new WasmBranch(sourceIndexGreaterThanSize, block));

        block.getBody().add(new WasmReturn());

        function.getBody().add(block);

        var aioobeFunction = context.functions().forStaticMethod(new MethodReference(WasmGCSupport.class, "aiiobe",
                ArrayIndexOutOfBoundsException.class));
        var throwExpr = new WasmThrow(context.exceptionTag());
        throwExpr.getArguments().add(new WasmCall(aioobeFunction));
        function.getBody().add(throwExpr);
        return function;
    }

    private WasmFunction getDefaultFunction(WasmGCIntrinsicContext manager) {
        if (defaultFunction == null) {
            defaultFunction = manager.functions().forStaticMethod(new MethodReference(System.class,
                    "arrayCopyImpl", Object.class, int.class, Object.class, int.class, int.class, void.class));
        }
        return defaultFunction;
    }
}
