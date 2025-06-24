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
package org.teavm.backend.wasm.model;

import java.util.List;

public abstract class WasmBlockType {
    private WasmBlockType() {
    }

    public abstract List<? extends WasmType> getInputTypes();

    public abstract List<? extends WasmType> getOutputTypes();

    public static class Function extends WasmBlockType {
        public final WasmFunctionType ref;

        Function(WasmFunctionType ref) {
            this.ref = ref;
        }

        @Override
        public List<? extends WasmType> getInputTypes() {
            return ref.getParameterTypes();
        }

        @Override
        public List<? extends WasmType> getOutputTypes() {
            return ref.getReturnTypes();
        }
    }

    public static class Value extends WasmBlockType {
        public final WasmType type;

        Value(WasmType type) {
            this.type = type;
        }

        @Override
        public List<? extends WasmType> getInputTypes() {
            return List.of();
        }

        @Override
        public List<? extends WasmType> getOutputTypes() {
            return type != null ? List.of(type) : List.of();
        }
    }
}
