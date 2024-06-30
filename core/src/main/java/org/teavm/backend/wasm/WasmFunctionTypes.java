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
package org.teavm.backend.wasm;

import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.render.WasmSignature;

public class WasmFunctionTypes {
    private WasmModule module;
    private Map<WasmSignature, WasmFunctionType> types = new HashMap<>();

    public WasmFunctionTypes(WasmModule module) {
        this.module = module;
    }

    public WasmFunctionType get(WasmSignature signature) {
        return types.computeIfAbsent(signature, k -> {
            var type = new WasmFunctionType(null, signature.getReturnType(), signature.getParameterTypes());
            module.types.add(type);
            return type;
        });
    }

    public WasmFunctionType of(WasmType returnType, WasmType... parameterTypes) {
        return get(new WasmSignature(returnType, parameterTypes));
    }
}
