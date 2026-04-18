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

public class WasmBlockInstruction extends WasmInstruction {
    private boolean loop;
    private WasmBlockType type;
    private WasmInstructionList body;

    public WasmBlockInstruction(boolean loop) {
        this.body = new WasmInstructionList(this);
        this.loop = loop;
    }

    public WasmInstructionList getBody() {
        return body;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public WasmBlockType getType() {
        return type;
    }

    public void setType(WasmBlockType type) {
        this.type = type;
    }

    @Override
    public void acceptVisitor(WasmInstructionVisitor visitor) {
        visitor.visit(this);
    }
}
