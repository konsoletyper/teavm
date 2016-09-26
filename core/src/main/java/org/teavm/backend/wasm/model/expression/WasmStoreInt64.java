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

public class WasmStoreInt64 extends WasmExpression implements WasmMemoryAccess {
    private int alignment;
    private WasmExpression index;
    private WasmExpression value;
    private WasmInt64Subtype convertTo;
    private int offset;

    public WasmStoreInt64(int alignment, WasmExpression index, WasmExpression value,
            WasmInt64Subtype convertTo) {
        Objects.requireNonNull(index);
        Objects.requireNonNull(convertTo);
        Objects.requireNonNull(value);
        this.alignment = alignment;
        this.index = index;
        this.value = value;
        this.convertTo = convertTo;
    }

    public int getAlignment() {
        return alignment;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public WasmExpression getIndex() {
        return index;
    }

    @Override
    public void setIndex(WasmExpression index) {
        Objects.requireNonNull(index);
        this.index = index;
    }

    public WasmExpression getValue() {
        return value;
    }

    public void setValue(WasmExpression value) {
        Objects.requireNonNull(value);
        this.value = value;
    }

    public WasmInt64Subtype getConvertTo() {
        return convertTo;
    }

    public void setConvertTo(WasmInt64Subtype convertTo) {
        Objects.requireNonNull(convertTo);
        this.convertTo = convertTo;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
