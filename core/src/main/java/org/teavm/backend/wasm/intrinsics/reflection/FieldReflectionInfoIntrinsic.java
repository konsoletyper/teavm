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
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;

public class FieldReflectionInfoIntrinsic implements WasmGCInlineIntrinsic {
    private final WasmGCClassInfoProvider classInfoProvider;

    public FieldReflectionInfoIntrinsic(WasmGCClassInfoProvider classInfoProvider) {
        this.classInfoProvider = classInfoProvider;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var infoStruct = classInfoProvider.reflectionTypes().fieldReflectionInfo();
        switch (invocation.getMethod().getName()) {
            case "annotationCount":
                WasmGCGenerationUtil.getArrayLengthOfNullable(builder, b -> {
                    context.generate(b, invocation.getArguments().get(0));
                    b.structGet(infoStruct.structure(), infoStruct.annotationsIndex());
                });
                break;
            case "annotation": {
                var array = classInfoProvider.reflectionTypes().annotationInfo().array();
                context.generate(builder, invocation.getArguments().get(0));
                builder.structGet(infoStruct.structure(), infoStruct.annotationsIndex());
                context.generate(builder, invocation.getArguments().get(1));
                builder.arrayGet(array);
                break;
            }
            case "genericType":
                context.generate(builder, invocation.getArguments().get(0));
                builder.structGet(infoStruct.structure(), infoStruct.genericTypeIndex());
                break;
            default:
                throw new IllegalStateException(invocation.getMethod().getName());
        }
    }
}
