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
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayCopy;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class SystemArrayCopyIntrinsic implements WasmGCIntrinsic {
    private WasmFunction defaultFunction;

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
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

        var wasmTargetArrayType = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.arrayOf(targetItemType));
        var wasmTargetArrayStruct = (WasmStructure) wasmTargetArrayType.composite;
        var wasmTargetArrayWrapper = context.generate(invocation.getArguments().get(2));
        var wasmTargetArray = new WasmStructGet(wasmTargetArrayStruct, wasmTargetArrayWrapper,
                WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
        var wasmTargetIndex = context.generate(invocation.getArguments().get(3));
        var wasmSourceArrayType = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.arrayOf(sourceItemType));
        var wasmSourceArrayStruct = (WasmStructure) wasmSourceArrayType.composite;
        var wasmSourceArrayWrapper = context.generate(invocation.getArguments().get(0));
        var wasmSourceArray = new WasmStructGet(wasmSourceArrayStruct, wasmSourceArrayWrapper,
                WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
        var wasmSourceIndex = context.generate(invocation.getArguments().get(1));
        var wasmSize = context.generate(invocation.getArguments().get(4));

        var wasmTargetArrayTypeRef = (WasmType.CompositeReference) wasmTargetArrayStruct.getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getUnpackedType();
        var wasmSourceArrayTypeRef = (WasmType.CompositeReference) wasmSourceArrayStruct.getFields()
                .get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET).getUnpackedType();

        return new WasmArrayCopy((WasmArray) wasmTargetArrayTypeRef.composite, wasmTargetArray, wasmTargetIndex,
                (WasmArray) wasmSourceArrayTypeRef.composite, wasmSourceArray, wasmSourceIndex, wasmSize);
    }

    private WasmFunction getDefaultFunction(WasmGCIntrinsicContext manager) {
        if (defaultFunction == null) {
            defaultFunction = manager.functions().forStaticMethod(new MethodReference(System.class,
                    "arraycopy", Object.class, int.class, Object.class, int.class, int.class, void.class));
        }
        return defaultFunction;
    }
}
