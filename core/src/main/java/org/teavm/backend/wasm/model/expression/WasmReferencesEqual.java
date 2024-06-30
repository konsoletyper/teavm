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

public class WasmReferencesEqual extends WasmExpression {
    private WasmExpression first;
    private WasmExpression second;

    public WasmReferencesEqual(WasmExpression first, WasmExpression second) {
        this.first = Objects.requireNonNull(first);
        this.second = Objects.requireNonNull(second);
    }

    public WasmExpression getFirst() {
        return first;
    }

    public void setFirst(WasmExpression first) {
        this.first = Objects.requireNonNull(first);
    }

    public WasmExpression getSecond() {
        return second;
    }

    public void setSecond(WasmExpression second) {
        this.second = Objects.requireNonNull(second);
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
