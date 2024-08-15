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
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmStructGet;

public class ClassIntrinsics implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "getComponentType":
                var cls = context.generate(invocation.getArguments().get(0));
                var clsStruct = context.classInfoProvider().getClassInfo("java.lang.Class").getStructure();
                var result = new WasmStructGet(clsStruct, cls,
                        context.classInfoProvider().getClassArrayItemOffset());
                result.setLocation(invocation.getLocation());
                return result;
            default:
                throw new IllegalArgumentException("Unsupported invocation method: " + invocation.getMethod());
        }
    }
}
