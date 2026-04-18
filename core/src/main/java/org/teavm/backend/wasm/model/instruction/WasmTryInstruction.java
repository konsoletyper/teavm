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

public class WasmTryInstruction extends WasmInstruction {
    private WasmType type;
    private WasmInstructionList body = new WasmInstructionList(this);
    private List<WasmCatchClause> catchesStorage = new ArrayList<>();

    private List<WasmCatchClause> catches = new AbstractList<>() {
        @Override
        public void add(int index, WasmCatchClause clause) {
            if (clause.breakTarget != null) {
                throw new IllegalArgumentException("Catch clause is already mounted to another try block");
            }
            clause.breakTarget = WasmTryInstruction.this;
            catchesStorage.add(clause);
        }

        @Override
        public WasmCatchClause remove(int index) {
            var result = catchesStorage.remove(index);
            result.breakTarget = null;
            return result;
        }

        @Override
        public int size() {
            return catchesStorage.size();
        }

        @Override
        public WasmCatchClause get(int index) {
            return catchesStorage.get(index);
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
