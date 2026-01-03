/*
 *  Copyright 2025 Alexey Andreev.
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
import org.teavm.backend.wasm.WasmHeap;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmMemoryGrow;
import org.teavm.backend.wasm.model.expression.WasmSequence;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;

public class HeapIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var pagesVar = context.tempVars().acquire(WasmType.INT32);
        var block = new WasmSequence();
        var bytes = context.generate(invocation.getArguments().get(0));
        WasmExpression pages = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SUB,
                bytes, new WasmInt32Constant(1));
        pages = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.DIV_UNSIGNED,
                pages, new WasmInt32Constant(WasmHeap.PAGE_SIZE));
        pages = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                pages, new WasmInt32Constant(1));
        block.getBody().add(new WasmSetLocal(pagesVar, pages));
        var grow = new WasmMemoryGrow(new WasmGetLocal(pagesVar));
        block.getBody().add(new WasmDrop(grow));
        block.getBody().add(new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.MUL,
                new WasmGetLocal(pagesVar), new WasmInt32Constant(WasmHeap.PAGE_SIZE)));
        context.tempVars().release(pagesVar);
        return block;
    }
}
