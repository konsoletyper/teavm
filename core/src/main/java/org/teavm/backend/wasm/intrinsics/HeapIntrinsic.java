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
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;

public class HeapIntrinsic implements WasmGCInlineIntrinsic {
    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var pagesVar = context.tempVars().acquire(WasmType.INT32);

        context.generate(builder, invocation.getArguments().get(0));
        builder.i32Const(1).intBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB)
                .i32Const(WasmGeneratorUtil.PAGE_SIZE).intBinary(WasmIntType.INT32, WasmIntBinaryOperation.DIV_UNSIGNED)
                .i32Const(1).intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD)
                .setLocal(pagesVar)
                .getLocal(pagesVar).memoryGrow().drop()
                .getLocal(pagesVar).i32Const(WasmGeneratorUtil.PAGE_SIZE)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.MUL);

        context.tempVars().release(pagesVar);
    }
}
