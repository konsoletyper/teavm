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
import org.teavm.backend.wasm.model.WasmArray;

public class WasmArrayCopyInstruction extends WasmInstruction {
    private WasmArray targetArrayType;
    private WasmArray sourceArrayType;

    public WasmArrayCopyInstruction(WasmArray targetArrayType, WasmArray sourceArrayType) {
        this.targetArrayType = Objects.requireNonNull(targetArrayType);
        this.sourceArrayType = Objects.requireNonNull(sourceArrayType);
    }

    public WasmArray getTargetArrayType() {
        return targetArrayType;
    }

    public void setTargetArrayType(WasmArray targetArrayType) {
        this.targetArrayType = Objects.requireNonNull(targetArrayType);
    }

    public WasmArray getSourceArrayType() {
        return sourceArrayType;
    }

    public void setSourceArrayType(WasmArray sourceArrayType) {
        this.sourceArrayType = Objects.requireNonNull(sourceArrayType);
    }

    @Override
    public void acceptVisitor(WasmInstructionVisitor visitor) {
        visitor.visit(this);
    }
}
