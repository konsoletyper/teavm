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
package org.teavm.backend.wasm.render;

import java.util.Arrays;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmType;

final class WasmSignature {
    WasmType[] types;

    WasmSignature(WasmType[] types) {
        this.types = types;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WasmSignature that = (WasmSignature) o;
        return Arrays.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(types);
    }

    public static WasmSignature fromFunction(WasmFunction function) {
        WasmType[] types = new WasmType[function.getParameters().size() + 1];
        types[0] = function.getResult();
        for (int i = 0; i < function.getParameters().size(); ++i) {
            types[i + 1] = function.getParameters().get(i);
        }
        return new WasmSignature(types);
    }
}
