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
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmSignedType;

public class AnnotationValueArrayIntrinsic implements WasmGCIntrinsic {
    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "size":
                context.generate(builder, invocation.getArguments().get(0));
                builder.arrayLength();
                break;
            case "getBoolean":
            case "getByte":
                get(invocation, context, builder, WasmStorageType.INT8, WasmSignedType.SIGNED);
                break;
            case "getShort":
                get(invocation, context, builder, WasmStorageType.INT16, WasmSignedType.SIGNED);
                break;
            case "getChar":
                get(invocation, context, builder, WasmStorageType.INT16, WasmSignedType.UNSIGNED);
                break;
            case "getInt":
                get(invocation, context, builder, WasmType.INT32.asStorage(), null);
                break;
            case "getLong":
                get(invocation, context, builder, WasmType.INT64.asStorage(), null);
                break;
            case "getFloat":
                get(invocation, context, builder, WasmType.FLOAT32.asStorage(), null);
                break;
            case "getDouble":
                get(invocation, context, builder, WasmType.FLOAT64.asStorage(), null);
                break;
            case "getClass": {
                var type = context.classInfoProvider().reflectionTypes().derivedClassInfo().structure()
                        .getReference();
                get(invocation, context, builder, type.asStorage(), null);
                break;
            }
            case "getEnum":
                get(invocation, context, builder, WasmStorageType.INT16, WasmSignedType.SIGNED);
                break;
            case "getString": {
                var type = context.classInfoProvider().getClassInfo("java.lang.String").getType();
                get(invocation, context, builder, type.asStorage(), null);
                break;
            }
            case "getAnnotation":
                get(invocation, context, builder, WasmType.STRUCT.asStorage(), null);
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private void get(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder, WasmStorageType type, WasmSignedType signedType) {
        WasmArray array = context.classInfoProvider().reflectionTypes().arrayTypeOf(type);
        context.generate(builder, invocation.getArguments().get(0));
        builder.cast(array.getReference());
        context.generate(builder, invocation.getArguments().get(1));
        builder.arrayGet(array, signedType);
    }
}
