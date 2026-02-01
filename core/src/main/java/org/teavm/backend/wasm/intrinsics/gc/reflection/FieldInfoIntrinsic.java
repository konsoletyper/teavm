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
package org.teavm.backend.wasm.intrinsics.gc.reflection;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmStructGet;

public class FieldInfoIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var infoStruct = context.classInfoProvider().reflectionTypes().fieldInfo();
        switch (invocation.getMethod().getName()) {
            case "name": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmStructGet(infoStruct.structure(), receiver, infoStruct.nameIndex());
            }
            case "modifiers": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmStructGet(infoStruct.structure(), receiver, infoStruct.modifiersIndex());
            }
            case "type": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmStructGet(infoStruct.structure(), receiver, infoStruct.typeIndex());
            }
            case "read": {
                var receiver = context.generate(invocation.getArguments().get(0));
                var reader = new WasmStructGet(infoStruct.structure(), receiver, infoStruct.readerIndex());
                var obj = context.generate(invocation.getArguments().get(1));
                return new WasmCallReference(reader, infoStruct.readerType(), obj);
            }
            case "write": {
                var receiver = context.generate(invocation.getArguments().get(0));
                var writer = new WasmStructGet(infoStruct.structure(), receiver, infoStruct.writerIndex());
                var obj = context.generate(invocation.getArguments().get(1));
                var value = context.generate(invocation.getArguments().get(2));
                return new WasmCallReference(writer, infoStruct.writerType(), obj, value);
            }
            case "reflection": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmStructGet(infoStruct.structure(), receiver, infoStruct.reflectionIndex());
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }
}
