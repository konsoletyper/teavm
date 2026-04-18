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

import java.util.Objects;
import org.teavm.backend.wasm.model.WasmTag;

public class WasmThrowInstruction extends WasmInstruction {
    private WasmTag tag;

    public WasmThrowInstruction(WasmTag tag) {
        this.tag = Objects.requireNonNull(tag);
    }

    public WasmTag getTag() {
        return tag;
    }

    public void setTag(WasmTag tag) {
        this.tag = Objects.requireNonNull(tag);
    }

    @Override
    public void acceptVisitor(WasmInstructionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean isTerminating() {
        return true;
    }
}
