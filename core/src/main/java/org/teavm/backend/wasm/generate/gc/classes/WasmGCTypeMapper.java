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
package org.teavm.backend.wasm.generate.gc.classes;

import org.teavm.backend.wasm.model.WasmPackedType;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.model.ValueType;

public class WasmGCTypeMapper {
    private WasmGCClassInfoProvider classInfoProvider;

    WasmGCTypeMapper(WasmGCClassInfoProvider classInfoProvider) {
        this.classInfoProvider = classInfoProvider;
    }

    public WasmStorageType mapType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BYTE:
                case BOOLEAN:
                    return WasmStorageType.packed(WasmPackedType.INT8);
                case SHORT:
                case CHARACTER:
                    return WasmStorageType.packed(WasmPackedType.INT8);
                case INTEGER:
                    return WasmType.INT32.asStorage();
                case LONG:
                    return WasmType.INT64.asStorage();
                case FLOAT:
                    return WasmType.FLOAT32.asStorage();
                case DOUBLE:
                    return WasmType.FLOAT64.asStorage();
                default:
                    throw new IllegalArgumentException();
            }
        } else {
            return classInfoProvider.getClassInfo(type).getType().asStorage();
        }
    }
}
