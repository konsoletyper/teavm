/*
 *  Copyright 2024 konsoletyper.
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

import java.util.Objects;

public class WasmField {
    WasmStructure structure;
    int index;
    private String name;
    private WasmStorageType type;
    private boolean immutable;

    public WasmField(WasmStorageType type, String name) {
        this(type);
        this.name = name;
    }

    public WasmField(WasmStorageType type) {
        this.type = Objects.requireNonNull(type);
    }

    public WasmStructure getStructure() {
        return structure;
    }

    public WasmStorageType getType() {
        return type;
    }

    public WasmType getUnpackedType() {
        return type.asUnpackedType();
    }

    public void setType(WasmStorageType type) {
        this.type = Objects.requireNonNull(type);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }

    public int getIndex() {
        if (structure == null) {
            return -1;
        }
        structure.ensureIndexes();
        return index;
    }
}
