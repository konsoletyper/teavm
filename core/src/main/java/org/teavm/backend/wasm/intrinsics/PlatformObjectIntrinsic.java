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
package org.teavm.backend.wasm.intrinsics;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.runtime.RuntimeObject;

public class PlatformObjectIntrinsic implements WasmIntrinsic {
    private static final String PLATFORM_OBJECT = "org.teavm.platform.PlatformObject";
    private static final FieldReference classField = new FieldReference(RuntimeObject.class.getName(),
            "classReference");
    private WasmClassGenerator classGenerator;

    public PlatformObjectIntrinsic(WasmClassGenerator classGenerator) {
        this.classGenerator = classGenerator;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        return methodReference.getClassName().equals(PLATFORM_OBJECT);
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "getPlatformClass": {
                int offset = classGenerator.getFieldOffset(classField);
                WasmExpression object = manager.generate(invocation.getArguments().get(0));
                if (offset > 0) {
                    object = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, object,
                            new WasmInt32Constant(offset));
                }
                WasmExpression classPtr = new WasmLoadInt32(4, object, WasmInt32Subtype.INT32);
                return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                        classPtr, new WasmInt32Constant(3));
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
