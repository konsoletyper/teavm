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

import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.instruction.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmFloatType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.model.instructions.InvokeInstruction;

public class DoubleIntrinsic implements WasmGCInlineIntrinsic {
    private static final long EXPONENT_BITS = 0x7FF0000000000000L;

    @Override
    public void apply(InvokeInstruction invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "getNaN":
                builder.f64Const(Double.NaN);
                break;
            case "isNaN": {
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.floatBinary(WasmFloatType.FLOAT64, WasmFloatBinaryOperation.NE);
                break;
            }
            case "isFinite":
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.reinterpret(WasmNumType.FLOAT64, WasmNumType.INT64);
                builder.i64Const(EXPONENT_BITS).intBinary(WasmIntType.INT64, WasmIntBinaryOperation.AND)
                        .i64Const(EXPONENT_BITS).intBinary(WasmIntType.INT64, WasmIntBinaryOperation.NE);
                break;
            case "doubleToRawLongBits":
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.reinterpret(WasmNumType.FLOAT64, WasmNumType.INT64);
                break;
            case "longBitsToDouble":
                builder.getLocal(context.mapToLocal(invocation.getArguments().get(0)));
                builder.reinterpret(WasmNumType.INT64, WasmNumType.FLOAT64);
                break;
            default:
                throw new AssertionError();
        }
    }

}
