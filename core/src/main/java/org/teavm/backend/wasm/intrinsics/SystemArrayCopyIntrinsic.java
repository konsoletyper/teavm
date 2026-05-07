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
package org.teavm.backend.wasm.intrinsics;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.runtime.WasmGCSupport;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class SystemArrayCopyIntrinsic implements WasmGCIntrinsic {
    private WasmFunction defaultFunction;
    private WasmFunction argsCheckFunction;

    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "arraycopy":
                generateArrayCopy(invocation, context, builder);
                break;
            case "doArrayCopy":
                generateDoArrayCopy(invocation, context, builder);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void generateArrayCopy(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder) {
        if (!tryGenerateSpecialCase(invocation, context, builder)) {
            for (int i = 0; i < 5; i++) {
                context.generate(builder, invocation.getArguments().get(i));
            }
            builder.call(getDefaultFunction(context));
        }
    }

    private void generateDoArrayCopy(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var classInfoStruct = context.classInfoProvider().reflectionTypes().classInfo();
        var objInfo = context.classInfoProvider().getClassInfo(Object.class.getName());

        context.generate(builder, invocation.getArguments().get(0));
        var source = context.valueCache().create(objInfo.getType(), builder);

        builder
                .structGet(objInfo.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET)
                .structGet(objInfo.getVirtualTableStructure(), WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);

        var sourceClsCached = context.valueCache().create(classInfoStruct.structure().getReference(), builder);
        builder.append(source);
        context.generate(builder, invocation.getArguments().get(1));
        context.generate(builder, invocation.getArguments().get(2));
        context.generate(builder, invocation.getArguments().get(3));
        context.generate(builder, invocation.getArguments().get(4));
        builder.append(sourceClsCached)
                .structGet(classInfoStruct.structure(), classInfoStruct.copyArrayIndex())
                .callReference(classInfoStruct.copyArrayFunctionType());

        source.release();
        sourceClsCached.release();
    }

    private boolean tryGenerateSpecialCase(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var sourceArray = invocation.getArguments().get(0);
        var targetArray = invocation.getArguments().get(2);
        if (sourceArray.getVariableIndex() < 0 || targetArray.getVariableIndex() < 0) {
            return false;
        }

        var sourceType = context.types().typeOf(sourceArray.getVariableIndex());
        if (sourceType == null || !(sourceType.valueType instanceof ValueType.Array)) {
            return false;
        }
        var targetType = context.types().typeOf(targetArray.getVariableIndex());
        if (targetType == null || !(targetType.valueType instanceof ValueType.Array)) {
            return false;
        }

        var sourceItemType = ((ValueType.Array) sourceType.valueType).getItemType();
        var targetItemType = ((ValueType.Array) targetType.valueType).getItemType();
        if (sourceItemType != targetItemType
                || !context.hierarchy().isSuperType(targetItemType, sourceItemType, false)) {
            return false;
        }

        var wasmTargetArrayType = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.arrayOf(targetItemType));
        var wasmTargetArrayStruct = (WasmStructure) wasmTargetArrayType.composite;
        var wasmTargetArrayTypeRef = (WasmType.CompositeReference) wasmTargetArrayStruct.getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        if (context.isAsync()) {
            wasmTargetArrayTypeRef = wasmTargetArrayTypeRef.composite.getReference();
        }
        context.generate(builder, invocation.getArguments().get(2));
        builder.structGet(wasmTargetArrayStruct, WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
        var wasmTargetArray = context.valueCache().create(wasmTargetArrayTypeRef, builder);
        builder.drop();

        context.generate(builder, invocation.getArguments().get(3));
        var wasmTargetIndex = context.valueCache().create(WasmType.INT32, builder);
        builder.drop();

        var wasmSourceArrayType = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.arrayOf(sourceItemType));
        var wasmSourceArrayStruct = (WasmStructure) wasmSourceArrayType.composite;
        var wasmSourceArrayTypeRef = (WasmType.CompositeReference) wasmSourceArrayStruct.getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        if (context.isAsync()) {
            wasmSourceArrayTypeRef = wasmSourceArrayTypeRef.composite.getReference();
        }
        context.generate(builder, invocation.getArguments().get(0));
        builder.structGet(wasmSourceArrayStruct, WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
        var wasmSourceArray = context.valueCache().create(wasmSourceArrayTypeRef, builder);
        builder.drop();

        context.generate(builder, invocation.getArguments().get(1));
        var wasmSourceIndex = context.valueCache().create(WasmType.INT32, builder);
        builder.drop();

        context.generate(builder, invocation.getArguments().get(4));
        var wasmSize = context.valueCache().create(WasmType.INT32, builder);
        builder.drop();

        builder.append(wasmTargetArray).append(wasmTargetIndex)
                .append(wasmSourceArray).append(wasmSourceIndex).append(wasmSize);
        builder.call(getArgsCheckFunction(context));

        builder.append(wasmTargetArray).append(wasmTargetIndex)
                .append(wasmSourceArray).append(wasmSourceIndex).append(wasmSize);
        builder.arrayCopy((WasmArray) wasmTargetArrayTypeRef.composite, (WasmArray) wasmSourceArrayTypeRef.composite);

        wasmTargetArray.release();
        wasmTargetIndex.release();
        wasmSourceArray.release();
        wasmSourceIndex.release();
        wasmSize.release();
        return true;
    }

    private WasmFunction getArgsCheckFunction(WasmGCIntrinsicContext context) {
        if (argsCheckFunction == null) {
            argsCheckFunction = createArgsCheckFunction(context);
        }
        return argsCheckFunction;
    }

    private WasmFunction createArgsCheckFunction(WasmGCIntrinsicContext context) {
        var function = new WasmFunction(context.functionTypes().of(null,
                WasmType.ARRAY, WasmType.INT32, WasmType.ARRAY,
                WasmType.INT32, WasmType.INT32));
        function.setName(context.names().topLevel("teavm@checkArrayCopy"));
        context.module().functions.add(function);

        var targetArrayLocal = new WasmLocal(WasmType.ARRAY, "targetArray");
        var targetArrayIndexLocal = new WasmLocal(WasmType.INT32, "targetIndex");
        var sourceArrayLocal = new WasmLocal(WasmType.ARRAY, "sourceArray");
        var sourceArrayIndexLocal = new WasmLocal(WasmType.INT32, "sourceIndex");
        var countLocal = new WasmLocal(WasmType.INT32, "count");
        function.add(targetArrayLocal);
        function.add(targetArrayIndexLocal);
        function.add(sourceArrayLocal);
        function.add(sourceArrayIndexLocal);
        function.add(countLocal);

        var body = function.getBody().builder();
        var blockBody = body.block();

        blockBody.getLocal(targetArrayIndexLocal).i32Const(0)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED)
                .branch(blockBody);
        blockBody.getLocal(sourceArrayIndexLocal).i32Const(0)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED)
                .branch(blockBody);
        blockBody.getLocal(countLocal).i32Const(0)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.LT_SIGNED)
                .branch(blockBody);

        blockBody.getLocal(targetArrayIndexLocal)
                .getLocal(targetArrayLocal).arrayLength()
                .getLocal(countLocal).intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED)
                .branch(blockBody);
        blockBody.getLocal(sourceArrayIndexLocal)
                .getLocal(sourceArrayLocal).arrayLength()
                .getLocal(countLocal).intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.GT_SIGNED)
                .branch(blockBody);

        blockBody.return_();

        var aioobeFunction = context.functions().forStaticMethod(new MethodReference(WasmGCSupport.class, "aiiobe",
                ArrayIndexOutOfBoundsException.class));
        body.call(aioobeFunction).throw_(context.exceptionTag());
        return function;
    }

    private WasmFunction getDefaultFunction(WasmGCIntrinsicContext context) {
        if (defaultFunction == null) {
            defaultFunction = context.functions().forStaticMethod(new MethodReference(System.class,
                    "arrayCopyImpl", Object.class, int.class, Object.class, int.class, int.class, void.class));
        }
        return defaultFunction;
    }
}
