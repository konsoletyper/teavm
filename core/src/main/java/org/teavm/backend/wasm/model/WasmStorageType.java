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
package org.teavm.backend.wasm.model;

public abstract class WasmStorageType {
    private static final Packed INT16 = new Packed(WasmPackedType.INT16);
    private static final Packed INT8 = new Packed(WasmPackedType.INT8);

    private WasmStorageType() {
    }

    public abstract WasmType asUnpackedType();

    public static WasmStorageType.Packed packed(WasmPackedType type) {
        switch (type) {
            case INT8:
                return INT8;
            case INT16:
                return INT16;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static final class Regular extends WasmStorageType {
        public final WasmType type;

        Regular(WasmType type) {
            this.type = type;
        }

        @Override
        public WasmType asUnpackedType() {
            return type;
        }
    }

    public static final class Packed extends WasmStorageType {
        public final WasmPackedType type;

        private Packed(WasmPackedType type) {
            this.type = type;
        }

        @Override
        public WasmType asUnpackedType() {
            return WasmType.INT32;
        }
    }
}
