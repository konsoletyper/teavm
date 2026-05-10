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
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;

public class AnnotationInfoIntrinsic implements WasmGCInlineIntrinsic {
    private final WasmGCClassInfoProvider classInfoProvider;

    public AnnotationInfoIntrinsic(WasmGCClassInfoProvider classInfoProvider) {
        this.classInfoProvider = classInfoProvider;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var annotInfoStruct = classInfoProvider.reflectionTypes().annotationInfo();
        context.generate(builder, invocation.getArguments().get(0));
        switch (invocation.getMethod().getName()) {
            case "data":
                builder.structGet(annotInfoStruct.structure(), annotInfoStruct.dataIndex());
                break;
            case "constructor":
                builder.structGet(annotInfoStruct.structure(), annotInfoStruct.constructorIndex());
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }
}
