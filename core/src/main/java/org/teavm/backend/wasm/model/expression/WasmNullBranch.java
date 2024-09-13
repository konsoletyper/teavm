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

public class WasmNullBranch extends WasmExpression {
    private WasmNullCondition condition;
    private WasmExpression value;
    private WasmBlock target;
    private WasmExpression result;

    public WasmNullBranch(WasmNullCondition condition, WasmExpression value, WasmBlock target) {
        this.condition = Objects.requireNonNull(condition);
        this.value = Objects.requireNonNull(value);
        this.target = Objects.requireNonNull(target);
    }

    public WasmNullCondition getCondition() {
        return condition;
    }

    public void setCondition(WasmNullCondition condition) {
        this.condition = Objects.requireNonNull(condition);
    }

    public WasmExpression getValue() {
        return value;
    }

    public void setValue(WasmExpression value) {
        this.value = Objects.requireNonNull(value);
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
