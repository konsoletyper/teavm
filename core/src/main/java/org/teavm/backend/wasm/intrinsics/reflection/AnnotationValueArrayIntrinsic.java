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
package org.teavm.backend.wasm.intrinsics.reflection;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmSignedType;

public class AnnotationValueArrayIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "size":
                return new WasmArrayLength(context.generate(invocation.getArguments().get(0)));
            case "getBoolean":
            case "getByte":
                return get(invocation, context, WasmStorageType.INT8, WasmSignedType.SIGNED);
            case "getShort":
                return get(invocation, context, WasmStorageType.INT16, WasmSignedType.SIGNED);
            case "getChar":
                return get(invocation, context, WasmStorageType.INT16, WasmSignedType.UNSIGNED);
            case "getInt":
                return get(invocation, context, WasmType.INT32.asStorage(), null);
            case "getLong":
                return get(invocation, context, WasmType.INT64.asStorage(), null);
            case "getFloat":
                return get(invocation, context, WasmType.FLOAT32.asStorage(), null);
            case "getDouble":
                return get(invocation, context, WasmType.FLOAT64.asStorage(), null);
            case "getClass": {
                var type = context.classInfoProvider().reflectionTypes().derivedClassInfo().structure()
                        .getReference();
                return get(invocation, context, type.asStorage(), null);
            }
            case "getEnum":
                return get(invocation, context, WasmStorageType.INT16, WasmSignedType.SIGNED);
            case "getString": {
                var type = context.classInfoProvider().getClassInfo("java.lang.String").getType();
                return get(invocation, context, type.asStorage(), null);
            }
            case "getAnnotation":
                return get(invocation, context, WasmType.STRUCT.asStorage(), null);
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private WasmExpression get(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmStorageType type, WasmSignedType signedType) {
        var arrayRef = context.generate(invocation.getArguments().get(0));
        var index = context.generate(invocation.getArguments().get(1));
        var array = context.classInfoProvider().reflectionTypes().arrayTypeOf(type);
        var cast = new WasmCast(arrayRef, array.getReference());
        var result = new WasmArrayGet(array, cast, index);
        result.setSignedType(signedType);
        return result;
    }
}
