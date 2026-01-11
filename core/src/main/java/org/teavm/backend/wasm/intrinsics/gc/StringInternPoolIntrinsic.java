/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.intrinsics.gc;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.model.ValueType;

class StringInternPoolIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var entryStruct = context.classInfoProvider().getClassInfo(StringInternPool.class.getName() + "$Entry")
                .getStructure();
        switch (invocation.getMethod().getName()) {
            case "getValue": {
                var weakRef = new WasmStructGet(entryStruct, context.generate(invocation.getArguments().get(0)),
                        WasmGCClassInfoProvider.STRING_POOL_ENTRY_OFFSET);
                return new WasmCall(createDerefFunction(context), weakRef);
            }
            case "setValue": {
                var block = new WasmBlock(false);
                var instance = context.exprCache().create(context.generate(invocation.getArguments().get(0)),
                        entryStruct.getReference(), invocation.getLocation(), block.getBody());
                var value = context.generate(invocation.getArguments().get(1));
                var ref = new WasmCall(createRefFunction(context), value, instance.expr());
                block.getBody().add(new WasmStructSet(entryStruct, instance.expr(),
                        WasmGCClassInfoProvider.STRING_POOL_ENTRY_OFFSET, ref));
                instance.release();
                return block;
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    private WasmFunction createRefFunction(WasmGCIntrinsicContext context) {
        var function = new WasmFunction(context.functionTypes().of(
                WasmType.Reference.EXTERN,
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
                WasmType.Reference.EXTERN
        ));
        function.setName(context.names().topLevel("teavm@stringDeref"));
        function.setImportModule("teavm");
        function.setImportName("stringDeref");
        context.module().functions.add(function);
        return function;
    }
}
