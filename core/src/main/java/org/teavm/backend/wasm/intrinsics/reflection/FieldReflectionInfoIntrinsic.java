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
import org.teavm.backend.wasm.generate.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmStructGet;

public class FieldReflectionInfoIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "annotationCount": {
                var receiver = context.generate(invocation.getArguments().get(0));
                var infoStruct = context.classInfoProvider().reflectionTypes().fieldReflectionInfo();
                var annotations = new WasmStructGet(infoStruct.structure(), receiver, infoStruct.annotationsIndex());
                return WasmGCGenerationUtil.getArrayLengthOfNullable(annotations);
            }
            case "annotation": {
                var infoStruct = context.classInfoProvider().reflectionTypes().fieldReflectionInfo();
                var array = context.classInfoProvider().reflectionTypes().annotationInfo().array();
                var receiver = context.generate(invocation.getArguments().get(0));
                var index = context.generate(invocation.getArguments().get(1));
                var annotations = new WasmStructGet(infoStruct.structure(), receiver, infoStruct.annotationsIndex());
                return new WasmArrayGet(array, annotations, index);
            }
            case "genericType": {
                var receiver = context.generate(invocation.getArguments().get(0));
                var infoStruct = context.classInfoProvider().reflectionTypes().fieldReflectionInfo();
                return new WasmStructGet(infoStruct.structure(), receiver, infoStruct.genericTypeIndex());
            }
            default:
                throw new IllegalStateException(invocation.getMethod().getName());
        }
    }
}
