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
import org.teavm.backend.wasm.model.WasmGlobal;

public class WasmSetGlobal extends WasmExpression {
    private WasmGlobal global;
    private WasmExpression value;

    public WasmSetGlobal(WasmGlobal global, WasmExpression value) {
        this.global = Objects.requireNonNull(global);
        this.value = Objects.requireNonNull(value);
    }

    public WasmGlobal getGlobal() {
        return global;
    }

    public void setGlobal(WasmGlobal global) {
        this.global = Objects.requireNonNull(global);
    }

    public WasmExpression getValue() {
        return value;
    }

    public void setValue(WasmExpression value) {
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
