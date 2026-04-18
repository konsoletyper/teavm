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

import org.teavm.backend.wasm.model.WasmBlockType;

public class WasmConditionalInstruction extends WasmInstruction {
    private WasmBlockType type;
    private final WasmInstructionList thenBlock = new WasmInstructionList(this);
    private final WasmInstructionList elseBlock = new WasmInstructionList(this);

    public WasmBlockType getType() {
        return type;
    }

    public void setType(WasmBlockType type) {
        this.type = type;
    }

    public WasmInstructionList getThenBlock() {
        return thenBlock;
    }

    public WasmInstructionList getElseBlock() {
        return elseBlock;
    }

    @Override
    public void acceptVisitor(WasmInstructionVisitor visitor) {
        visitor.visit(this);
    }
}
