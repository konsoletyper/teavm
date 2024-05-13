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

import java.util.List;
import java.util.function.Supplier;

public class WasmStructure extends WasmCompositeType {
    private List<? extends WasmStorageType> fields;
    private Supplier<List<? extends WasmStorageType>> fieldsSupplier;

    public WasmStructure(List<? extends WasmStorageType> fields) {
        this.fields = List.copyOf(fields);
    }

    public WasmStructure(Supplier<List<? extends WasmStorageType>> fieldsSupplier) {
        this.fieldsSupplier = fieldsSupplier;
    }

    public List<? extends WasmStorageType> getFields() {
        if (fields == null) {
            fields = List.copyOf(fieldsSupplier.get());
            fieldsSupplier = null;
        }
        return fields;
    }

    @Override
    public void acceptVisitor(WasmCompositeTypeVisitor visitor) {
        visitor.visit(this);
    }
}
