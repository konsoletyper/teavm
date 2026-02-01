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
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmStructGet;

public class TypeVariableInfoIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "name": {
                var struct = context.classInfoProvider().reflectionTypes().typeVariableInfo();
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmStructGet(struct.structure(), receiver, struct.nameIndex());
            }
            case "boundCount": {
                var struct = context.classInfoProvider().reflectionTypes().typeVariableInfo();
                var receiver = context.generate(invocation.getArguments().get(0));
                var bounds = new WasmStructGet(struct.structure(), receiver, struct.boundsIndex());
                return WasmGCGenerationUtil.getArrayLengthOfNullable(bounds);
            }
            case "bound": {
                var struct = context.classInfoProvider().reflectionTypes().typeVariableInfo();
                var receiver = context.generate(invocation.getArguments().get(0));
                var index = context.generate(invocation.getArguments().get(1));
                var bounds = new WasmStructGet(struct.structure(), receiver, struct.boundsIndex());
                return new WasmArrayGet(context.classInfoProvider().reflectionTypes().genericTypeArray(),
                        bounds, index);
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }
}
