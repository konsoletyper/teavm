/*
 *  Copyright 2016 Alexey Andreev.
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

import org.teavm.ast.ConstantExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.model.ValueType;

public class StructureIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "toAddress":
            case "cast":
                return context.generate(invocation.getArguments().get(0));
            case "sizeOf": {
                var type = (ValueType.Object) ((ConstantExpr) invocation.getArguments().get(0)).getValue();
                return new WasmInt32Constant(context.classInfoProvider().getHeapSize(type.getClassName()));
            }
            case "add": {
                var base = context.generate(invocation.getArguments().get(1));
                var offset = context.generate(invocation.getArguments().get(2));
                var type = ((ConstantExpr) invocation.getArguments().get(0)).getValue();
                var className = ((ValueType.Object) type).getClassName();
                int size = context.classInfoProvider().getHeapSize(className);
                int alignment = context.classInfoProvider().getHeapAlignment(className);
                size = WasmClassGenerator.align(size, alignment);

                offset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.MUL, offset,
                        new WasmInt32Constant(size));
                return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, base, offset);
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
