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
package org.teavm.platform.plugin.wasmgc;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmArrayLength;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.model.ValueType;
import org.teavm.platform.metadata.ResourceMap;

public class WasmGCResourceMapHelperIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "entryCount":
                return new WasmArrayLength(context.generate(invocation.getArguments().get(0)));
            case "entry":
                return new WasmArrayGet(getArrayType(context),
                        context.generate(invocation.getArguments().get(0)),
                        context.generate(invocation.getArguments().get(1)));
            default:
                throw new IllegalArgumentException();
        }
    }

    private WasmArray getArrayType(WasmGCIntrinsicContext context) {
        var type = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.object(ResourceMap.class.getName()));
        return (WasmArray) type.composite;
    }
}
