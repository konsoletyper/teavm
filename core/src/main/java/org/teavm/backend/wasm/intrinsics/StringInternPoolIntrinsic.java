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
package org.teavm.backend.wasm.intrinsics;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.model.ValueType;

class StringInternPoolIntrinsic implements WasmGCIntrinsic {
    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        var entryStruct = context.classInfoProvider().getClassInfo(StringInternPool.class.getName() + "$Entry")
                .getStructure();
        switch (invocation.getMethod().getName()) {
            case "getValue":
                context.generate(builder, invocation.getArguments().get(0));
                builder.structGet(entryStruct, WasmGCClassInfoProvider.STRING_POOL_ENTRY_OFFSET);
                builder.call(createDerefFunction(context));
                break;
            case "setValue": {
                context.generate(builder, invocation.getArguments().get(0));
                var instance = context.valueCache().create(entryStruct.getReference(), builder);
                builder.drop();

                builder.append(instance);
                context.generate(builder, invocation.getArguments().get(1));
                builder.append(instance);
                builder.call(createRefFunction(context));
                builder.structSet(entryStruct, WasmGCClassInfoProvider.STRING_POOL_ENTRY_OFFSET);

                instance.release();
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    private WasmFunction createRefFunction(WasmGCIntrinsicContext context) {
        var function = new WasmFunction(context.functionTypes().of(
                WasmType.EXTERN,
                context.typeMapper().mapType(ValueType.parse(String.class)),
                context.typeMapper().mapType(ValueType.object(StringInternPool.class.getName() + "$Entry"))
        ));
        function.setName(context.names().topLevel("teavm@stringRef"));
        function.setImportModule("teavm");
        function.setImportName("createStringWeakRef");
        context.module().functions.add(function);
        return function;
    }

    private WasmFunction createDerefFunction(WasmGCIntrinsicContext context) {
        var function = new WasmFunction(context.functionTypes().of(
                context.typeMapper().mapType(ValueType.parse(String.class)),
                WasmType.EXTERN
        ));
        function.setName(context.names().topLevel("teavm@stringDeref"));
        function.setImportModule("teavm");
        function.setImportName("stringDeref");
        context.module().functions.add(function);
        return function;
    }
}
