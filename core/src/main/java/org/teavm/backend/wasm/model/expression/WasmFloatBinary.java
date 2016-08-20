/*
 *  Copyright 2016 Alexey Andreev.
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

public class WasmFloatBinary extends WasmExpression {
    private WasmFloatType type;
    private WasmFloatBinaryOperation operation;
    private WasmExpression first;
    private WasmExpression second;

    public WasmFloatBinary(WasmFloatType type, WasmFloatBinaryOperation operation, WasmExpression first,
            WasmExpression second) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        this.type = type;
        this.operation = operation;
        this.first = first;
        this.second = second;
    }

    public WasmFloatType getType() {
        return type;
    }

    public void setType(WasmFloatType type) {
        Objects.requireNonNull(type);
        this.type = type;
    }

    public WasmFloatBinaryOperation getOperation() {
        return operation;
    }

    public void setOperation(WasmFloatBinaryOperation operation) {
        Objects.requireNonNull(operation);
        this.operation = operation;
    }

    public WasmExpression getFirst() {
        return first;
    }

    public void setFirst(WasmExpression first) {
        Objects.requireNonNull(first);
        this.first = first;
    }

    public WasmExpression getSecond() {
        return second;
    }

    public void setSecond(WasmExpression second) {
        Objects.requireNonNull(second);
        this.second = second;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
