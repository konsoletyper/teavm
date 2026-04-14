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
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmSignedType;
import org.teavm.backend.wasm.model.expression.WasmStructGet;

public class DerivedClassInfoIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "classInfo": {
                var infoStruct = context.classInfoProvider().reflectionTypes().derivedClassInfo();
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmStructGet(infoStruct.structure(), receiver, infoStruct.classInfoIndex());
            }
            case "arrayDegree": {
                var infoStruct = context.classInfoProvider().reflectionTypes().derivedClassInfo();
                var receiver = context.generate(invocation.getArguments().get(0));
                var result = new WasmStructGet(infoStruct.structure(), receiver, infoStruct.arrayDegreeIndex());
                result.setSignedType(WasmSignedType.SIGNED);
                return result;
            }
            default: {
                throw new IllegalArgumentException(invocation.getMethod().getName());
            }
        }
    }
}
