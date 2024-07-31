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

import java.util.ArrayList;
import java.util.List;

public class WasmStructure extends WasmCompositeType {
    private List<WasmStorageType> fields = new ArrayList<>();
    private WasmStructure supertype;

    public WasmStructure(String name) {
        super(name);
    }

    public List<WasmStorageType> getFields() {
        return fields;
    }

    public WasmStructure getSupertype() {
        return supertype;
    }

    public void setSupertype(WasmStructure supertype) {
        this.supertype = supertype;
    }

    public boolean isSupertypeOf(WasmStructure subtype) {
        while (subtype != null) {
            if (subtype == this) {
                return true;
            }
            subtype = subtype.getSupertype();
        }
        return false;
    }

    @Override
    public void acceptVisitor(WasmCompositeTypeVisitor visitor) {
        visitor.visit(this);
    }
}
