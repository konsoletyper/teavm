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
package org.teavm.backend.wasm.generators.gc;

import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.model.MethodReference;

public class WasmGCStringPoolGenerator implements WasmGCCustomGenerator {
    @Override
    public void apply(MethodReference method, WasmFunction function, WasmGCCustomGeneratorContext context) {
        var module = context.module();
        var pointer = new WasmGlobal(context.names().topLevel("teavm@stringPoolPointer"), WasmType.INT32,
                new WasmInt32Constant(0));
        module.globals.add(pointer);

        var resultLocal = new WasmLocal(WasmType.INT32);
        function.add(resultLocal);

        function.getBody().add(new WasmSetLocal(resultLocal, new WasmLoadInt32(1, new WasmGetGlobal(pointer),
                WasmInt32Subtype.UINT8)));
        var increment = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                new WasmGetGlobal(pointer), new WasmInt32Constant(1));
        function.getBody().add(new WasmSetGlobal(pointer, increment));
        function.getBody().add(new WasmGetLocal(resultLocal));
    }
}
