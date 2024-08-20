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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class WasmStructure extends WasmCompositeType {
    private List<WasmField> fieldsStorage = new ArrayList<>();
    private WasmStructure supertype;
    private boolean indexesValid = true;

    public WasmStructure(String name) {
        super(name);
    }

    public List<WasmField> getFields() {
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

    void ensureIndexes() {
        if (!indexesValid) {
            indexesValid = true;
            for (var i = 0; i < fieldsStorage.size(); ++i) {
                fieldsStorage.get(i).index = i;
            }
        }
    }

    @Override
    public void acceptVisitor(WasmCompositeTypeVisitor visitor) {
        visitor.visit(this);
    }

    private List<WasmField> fields = new AbstractList<WasmField>() {
        @Override
        public WasmField get(int index) {
            return fieldsStorage.get(index);
        }

        @Override
        public int size() {
            return fieldsStorage.size();
        }

        @Override
        public void add(int index, WasmField element) {
            if (element.structure != null) {
                throw new IllegalArgumentException("This field already belongs to structure");
            }
            element.structure = WasmStructure.this;
            indexesValid = false;
            fieldsStorage.add(index, element);
        }

        @Override
        public WasmField remove(int index) {
            var result = fieldsStorage.remove(index);
            indexesValid = false;
            result.structure = null;
            return result;
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            var sublist = fieldsStorage.subList(fromIndex, toIndex);
            for (var field : sublist) {
                field.structure = null;
            }
            indexesValid = false;
            sublist.clear();
        }

        @Override
        public void clear() {
            for (var field : fieldsStorage) {
                field.structure = null;
            }
            indexesValid = true;
            fieldsStorage.clear();
        }

        @Override
        public WasmField set(int index, WasmField element) {
            if (element.structure != null) {
                throw new IllegalArgumentException("This field already belongs to structure");
            }
            var former = fieldsStorage.set(index, element);
            former.structure = null;
            if (indexesValid) {
                element.index = former.index;
            }
            element.structure = WasmStructure.this;
            return former;
        }
    };
}
