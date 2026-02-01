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
import org.teavm.backend.wasm.generate.gc.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmStructGet;

public class MethodInfoIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var infoStruct = context.classInfoProvider().reflectionTypes().methodInfo();
        switch (invocation.getMethod().getName()) {
            case "name": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmStructGet(infoStruct.structure(), receiver, infoStruct.nameIndex());
            }
            case "modifiers": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmStructGet(infoStruct.structure(), receiver, infoStruct.modifiersIndex());
            }
            case "returnType": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmStructGet(infoStruct.structure(), receiver, infoStruct.returnTypeIndex());
            }
            case "parameterCount": {
                var receiver = context.generate(invocation.getArguments().get(0));
                var params = new WasmStructGet(infoStruct.structure(), receiver, infoStruct.parameterTypesIndex());
                return WasmGCGenerationUtil.getArrayLengthOfNullable(params);
            }
            case "parameterType": {
                var receiver = context.generate(invocation.getArguments().get(0));
                var index = context.generate(invocation.getArguments().get(1));
                var params = new WasmStructGet(infoStruct.structure(), receiver, infoStruct.parameterTypesIndex());
                var paramsType = context.classInfoProvider().reflectionTypes().derivedClassInfo().array();
                return new WasmArrayGet(paramsType, params, index);
            }
            case "call": {
                var receiver = context.generate(invocation.getArguments().get(0));
                var reader = new WasmStructGet(infoStruct.structure(), receiver, infoStruct.callerIndex());
                var callReceiver = context.generate(invocation.getArguments().get(1));
                var args = context.generate(invocation.getArguments().get(2));
                return new WasmCallReference(reader, infoStruct.callerType(), callReceiver, args);
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
