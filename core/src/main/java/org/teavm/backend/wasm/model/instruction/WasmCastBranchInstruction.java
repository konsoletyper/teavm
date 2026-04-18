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
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCastCondition;

public class WasmCastBranchInstruction extends WasmInstruction {
    private WasmCastCondition condition;
    private WasmType.Reference sourceType;
    private WasmType.Reference targetType;
    private WasmInstructionList target;

    public WasmCastBranchInstruction(WasmCastCondition condition, WasmType.Reference sourceType,
            WasmType.Reference targetType, WasmInstructionList target) {
        this.condition = Objects.requireNonNull(condition);
        this.sourceType = Objects.requireNonNull(sourceType);
        this.targetType = Objects.requireNonNull(targetType);
        this.target = Objects.requireNonNull(target);
    }

    public WasmCastCondition getCondition() {
        return condition;
    }

    public void setCondition(WasmCastCondition condition) {
        this.condition = Objects.requireNonNull(condition);
    }

    public WasmType.Reference getSourceType() {
        return sourceType;
    }

    public void setSourceType(WasmType.Reference sourceType) {
        this.sourceType = Objects.requireNonNull(sourceType);
    }

    public WasmType.Reference getTargetType() {
        return targetType;
    }

    public void setTargetType(WasmType.Reference targetType) {
        this.targetType = Objects.requireNonNull(targetType);
    }

    public WasmInstructionList getTarget() {
        return target;
    }

    public void setTarget(WasmInstructionList target) {
        this.target = Objects.requireNonNull(target);
    }

    @Override
    public void acceptVisitor(WasmInstructionVisitor visitor) {
        visitor.visit(this);
    }
}
