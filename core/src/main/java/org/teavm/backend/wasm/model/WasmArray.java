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

import java.util.Objects;
import java.util.function.Supplier;

public class WasmArray extends WasmCompositeType {
    private WasmStorageType elementType;
    private Supplier<WasmStorageType> elementTypeSupplier;

    public WasmArray(String name, WasmStorageType elementType) {
        super(name);
        this.elementType = Objects.requireNonNull(elementType);
    }

    public WasmArray(String name, Supplier<WasmStorageType> elementTypeSupplier) {
        super(name);
        this.elementTypeSupplier = elementTypeSupplier;
    }

    public WasmStorageType getElementType() {
        if (elementType == null) {
            elementType = elementTypeSupplier.get();
            elementTypeSupplier = null;
        }
        return elementType;
    }

    @Override
    public void acceptVisitor(WasmCompositeTypeVisitor visitor) {
        visitor.visit(this);
    }
}
