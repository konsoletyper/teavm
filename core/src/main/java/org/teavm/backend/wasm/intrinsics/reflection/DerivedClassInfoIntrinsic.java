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
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmSignedType;

public class DerivedClassInfoIntrinsic implements WasmGCIntrinsic {
    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        var infoStruct = context.classInfoProvider().reflectionTypes().derivedClassInfo();
        context.generate(builder, invocation.getArguments().get(0));
        switch (invocation.getMethod().getName()) {
            case "classInfo":
                builder.structGet(infoStruct.structure(), infoStruct.classInfoIndex());
                break;
            case "arrayDegree":
                builder.structGet(infoStruct.structure(), infoStruct.arrayDegreeIndex(), WasmSignedType.SIGNED);
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }
}
