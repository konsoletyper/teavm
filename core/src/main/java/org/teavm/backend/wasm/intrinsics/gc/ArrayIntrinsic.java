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
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmStructGet;

public class ArrayIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "getLength":
                return arrayLength(invocation, context);
            case "getImpl":
                return arrayGet(invocation, context);
            default:
                throw new IllegalArgumentException("Unknown method: " + invocation.getMethod());
        }
    }

    private WasmExpression arrayLength(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        return arrayVirtualCall(invocation, context, context.classInfoProvider().getArrayLengthOffset());
    }

    private WasmExpression arrayGet(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        return arrayVirtualCall(invocation, context, context.classInfoProvider().getArrayGetOffset());
    }

    private WasmExpression arrayVirtualCall(InvocationExpr invocation, WasmGCIntrinsicContext context,
            int offset) {
        var objectStruct = context.classInfoProvider().getClassInfo("java.lang.Object").getStructure();
        var vtStruct = context.classInfoProvider().getArrayVirtualTableStructure();
        var type = (WasmType.CompositeReference) vtStruct.getFields().get(offset).getUnpackedType();
        var functionType = (WasmFunctionType) type.composite;
        var block = new WasmBlock(false);
        block.setType(functionType.getReturnType());

        var originalObject = context.generate(invocation.getArguments().get(0));
        var object = context.exprCache().create(originalObject, objectStruct.getReference(),
                invocation.getLocation(), block.getBody());
        var classRef = new WasmStructGet(objectStruct, object.expr(), WasmGCClassInfoProvider.CLASS_FIELD_OFFSET);
        var vt = new WasmCast(classRef, vtStruct.getNonNullReference());
        var function = new WasmStructGet(vtStruct, vt, offset);
        var call = new WasmCallReference(function, functionType);
        call.getArguments().add(object.expr());
        for (var i = 1; i < invocation.getArguments().size(); ++i) {
            call.getArguments().add(context.generate(invocation.getArguments().get(i)));
        }
        block.getBody().add(call);

        object.release();
        return block;
    }
}
