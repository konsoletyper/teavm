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
package org.teavm.backend.wasm.model.instruction;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.model.WasmType;

public class WasmTry extends WasmInstruction {
    private WasmType type;
    private WasmInstructionList body = new WasmInstructionList(this);
    private List<WasmCatchClause> catches = new AbstractList<>() {
        private List<WasmCatchClause> inner = new ArrayList<>();

        @Override
        public WasmCatchClause get(int index) {
            return inner.get(index);
        }

        @Override
        public int size() {
            return inner.size();
        }

        @Override
        public void clear() {
            for (var e : inner) {
                e.owner = null;
            }
            inner.clear();
        }

        @Override
        public boolean add(WasmCatchClause wasmCatchClause) {
            bind(wasmCatchClause);
            return inner.add(wasmCatchClause);
        }

        @Override
        public WasmCatchClause set(int index, WasmCatchClause element) {
            if (inner.get(index) == element) {
                return element;
            }
            inner.get(index).owner = null;
            bind(element);
            return inner.set(index, element);
        }

        @Override
        public WasmCatchClause remove(int index) {
            var result = inner.get(index);
            result.owner = null;
            return result;
        }

        private void bind(WasmCatchClause clause) {
            if (clause.owner != null) {
                throw new IllegalArgumentException("This clause already added to try instruction");
            }
            clause.owner = WasmTry.this;
        }
    };

    public WasmInstructionList getBody() {
        return body;
    }

    public WasmType getType() {
        return type;
    }

    public void setType(WasmType type) {
        this.type = type;
    }

    public List<WasmCatchClause> getCatches() {
        return catches;
    }

    @Override
    public void acceptVisitor(WasmInstructionVisitor visitor) {
        visitor.visit(this);
    }
}
