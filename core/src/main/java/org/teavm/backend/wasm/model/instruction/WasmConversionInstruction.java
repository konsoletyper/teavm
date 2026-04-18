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
import org.teavm.backend.wasm.model.WasmNumType;

public class WasmConversionInstruction extends WasmInstruction {
    private WasmNumType sourceType;
    private WasmNumType targetType;
    private boolean signed;
    private boolean reinterpret;
    private boolean nonTrapping;

    public WasmConversionInstruction(WasmNumType sourceType, WasmNumType targetType, boolean signed) {
        this.sourceType = Objects.requireNonNull(sourceType);
        this.targetType = Objects.requireNonNull(targetType);
        this.signed = signed;
    }

    public WasmNumType getSourceType() {
        return sourceType;
    }

    public void setSourceType(WasmNumType sourceType) {
        this.sourceType = Objects.requireNonNull(sourceType);
    }

    public WasmNumType getTargetType() {
        return targetType;
    }

    public void setTargetType(WasmNumType targetType) {
        this.targetType = Objects.requireNonNull(targetType);
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public boolean isReinterpret() {
        return reinterpret;
    }

    public void setReinterpret(boolean reinterpret) {
        this.reinterpret = reinterpret;
    }

    public boolean isNonTrapping() {
        return nonTrapping;
    }

    public void setNonTrapping(boolean nonTrapping) {
        this.nonTrapping = nonTrapping;
    }

    @Override
    public void acceptVisitor(WasmInstructionVisitor visitor) {
        visitor.visit(this);
    }
}
