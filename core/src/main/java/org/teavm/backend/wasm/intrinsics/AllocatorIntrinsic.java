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

import java.util.stream.Collectors;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeClass;

public class AllocatorIntrinsic implements WasmIntrinsic {
    private static final FieldReference flagsField = new FieldReference(RuntimeClass.class.getName(), "flags");
    private int flagsFieldOffset;

    public AllocatorIntrinsic(WasmClassGenerator classGenerator) {
        flagsFieldOffset = classGenerator.getFieldOffset(flagsField);
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(Allocator.class.getName())) {
            return false;
        }
        switch (methodReference.getName()) {
            case "fillZero":
            case "moveMemoryBlock":
            case "isInitialized":
                return true;
            default:
                return false;
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "fillZero":
            case "moveMemoryBlock": {
                MethodReference delegateMethod = new MethodReference(WasmRuntime.class.getName(),
                        invocation.getMethod().getDescriptor());
                WasmCall call = new WasmCall(manager.getNames().forMethod(delegateMethod));
                call.getArguments().addAll(invocation.getArguments().stream()
                        .map(manager::generate)
                        .collect(Collectors.toList()));
                return call;
            }
            case "isInitialized": {
                WasmExpression pointer = manager.generate(invocation.getArguments().get(0));
                if (pointer instanceof WasmInt32Constant) {
                    pointer = new WasmInt32Constant(((WasmInt32Constant) pointer).getValue() + flagsFieldOffset);
                } else {
                    pointer = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, pointer,
                            new WasmInt32Constant(flagsFieldOffset));
                }
                WasmExpression flags = new WasmLoadInt32(4, pointer, WasmInt32Subtype.INT32);
                WasmExpression flag = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.AND, flags,
                        new WasmInt32Constant(RuntimeClass.INITIALIZED));
                return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.NE, flag, new WasmInt32Constant(0));
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().toString());
        }
    }
}
