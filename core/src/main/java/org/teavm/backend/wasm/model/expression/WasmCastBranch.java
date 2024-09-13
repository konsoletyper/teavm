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
package org.teavm.backend.wasm.model.expression;

import java.util.Objects;
import org.teavm.backend.wasm.model.WasmType;

public class WasmCastBranch extends WasmExpression {
    private WasmCastCondition condition;
    private WasmExpression value;
    private WasmType.Reference sourceType;
    private WasmType.Reference type;
    private WasmBlock target;
    private WasmExpression result;

    public WasmCastBranch(WasmCastCondition condition, WasmExpression value, WasmType.Reference sourceType,
            WasmType.Reference type, WasmBlock target) {
        this.condition = Objects.requireNonNull(condition);
        this.value = Objects.requireNonNull(value);
        this.type = Objects.requireNonNull(type);
        this.target = Objects.requireNonNull(target);
    }

    public WasmCastCondition getCondition() {
        return condition;
    }

    public void setCondition(WasmCastCondition condition) {
        this.condition = Objects.requireNonNull(condition);
    }

    public WasmExpression getValue() {
        return value;
    }

    public void setValue(WasmExpression value) {
        this.value = Objects.requireNonNull(value);
    }

    public WasmType.Reference getSourceType() {
        return sourceType;
    }

    public void setSourceType(WasmType.Reference sourceType) {
        this.sourceType = Objects.requireNonNull(sourceType);
    }

    public WasmType.Reference getType() {
        return type;
    }

    public void setType(WasmType.Reference type) {
        this.type = Objects.requireNonNull(type);
    }

    public WasmBlock getTarget() {
        return target;
    }

    public void setTarget(WasmBlock target) {
        this.target = Objects.requireNonNull(target);
    }

    public WasmExpression getResult() {
        return result;
    }

    public void setResult(WasmExpression result) {
        this.result = result;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
