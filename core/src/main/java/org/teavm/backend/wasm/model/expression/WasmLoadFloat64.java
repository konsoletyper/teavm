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

public class WasmLoadFloat64 extends WasmExpression implements WasmMemoryAccess {
    private int alignment;
    private WasmExpression index;
    private int offset;

    public WasmLoadFloat64(int alignment, WasmExpression index) {
        this(alignment, index, 0);
    }

    public WasmLoadFloat64(int alignment, WasmExpression index, int offset) {
        Objects.requireNonNull(index);
        this.alignment = alignment;
        this.index = index;
        this.offset = offset;
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

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
