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

import java.util.List;
import java.util.Objects;
import org.teavm.backend.wasm.model.WasmType;

public final class WasmSignature {
    private List<? extends WasmType> returnTypes;
    private List<? extends WasmType> types;

    public WasmSignature(List<? extends WasmType> returnType, List<? extends WasmType> parameterTypes) {
        this.returnTypes = Objects.requireNonNull(returnType);
        this.types = Objects.requireNonNull(parameterTypes);
    }

    public List<? extends WasmType> getReturnTypes() {
        return returnTypes;
    }

    public List<? extends WasmType> getParameterTypes() {
        return types;
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
        return Objects.equals(types, that.types) && Objects.equals(returnTypes, that.returnTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(types, returnTypes);
    }
}
