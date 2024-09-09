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
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.model.ValueType;

public class ClassSupportIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var enumArrayStruct = context.classInfoProvider().getClassInfo(ValueType.arrayOf(
                ValueType.object("java.lang.Enum"))).getStructure();
        var clsStruct = context.classInfoProvider().getClassInfo("java.lang.Class").getStructure();
        var cls = context.generate(invocation.getArguments().get(0));
        var fieldIndex = context.classInfoProvider().getEnumConstantsFunctionOffset();
        var functionRef = new WasmStructGet(clsStruct, cls, fieldIndex);
        return new WasmCallReference(functionRef, context.functionTypes().of(enumArrayStruct.getReference()));
    }
}
