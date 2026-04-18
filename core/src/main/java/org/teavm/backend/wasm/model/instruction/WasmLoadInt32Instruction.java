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
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;

public class WasmLoadInt32Instruction extends WasmInstruction {
    private int alignment;
    private int offset;
    private WasmInt32Subtype convertFrom;

    public WasmLoadInt32Instruction(int alignment, WasmInt32Subtype convertFrom) {
        this.alignment = alignment;
        this.convertFrom = Objects.requireNonNull(convertFrom);
    }

    public int getAlignment() {
        return alignment;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public WasmInt32Subtype getConvertFrom() {
        return convertFrom;
    }

    public void setConvertFrom(WasmInt32Subtype convertFrom) {
        this.convertFrom = Objects.requireNonNull(convertFrom);
    }

    @Override
    public void acceptVisitor(WasmInstructionVisitor visitor) {
        visitor.visit(this);
    }
}
