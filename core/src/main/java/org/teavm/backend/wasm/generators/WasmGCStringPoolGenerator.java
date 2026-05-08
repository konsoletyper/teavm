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
package org.teavm.backend.wasm.generators;

import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmInt32Subtype;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.model.MethodReference;

public class WasmGCStringPoolGenerator implements WasmGCCustomGenerator {
    @Override
    public void apply(MethodReference method, WasmFunction function, WasmGCCustomGeneratorContext context) {
        var module = context.module();
        var pointer = new WasmGlobal(context.names().topLevel("teavm@stringPoolPointer"), WasmType.INT32);
        pointer.getInitialValue().add(new WasmInt32Constant(0));
        module.globals.add(pointer);

        var body = function.getBody().builder();

        body
                .getGlobal(pointer)
                .loadI32(1, 0, WasmInt32Subtype.UINT8);
        body
                .getGlobal(pointer)
                .i32Const(1)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD)
                .setGlobal(pointer);
    }
}
