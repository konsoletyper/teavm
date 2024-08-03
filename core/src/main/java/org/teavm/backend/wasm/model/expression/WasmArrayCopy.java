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

import org.teavm.backend.wasm.model.WasmArray;

public class WasmArrayCopy extends WasmExpression {
    private WasmArray targetArrayType;
    private WasmExpression targetArray;
    private WasmExpression targetIndex;
    private WasmArray sourceArrayType;
    private WasmExpression sourceArray;
    private WasmExpression sourceIndex;
    private WasmExpression size;

    public WasmArrayCopy(
            WasmArray targetArrayType, WasmExpression targetArray, WasmExpression targetIndex,
            WasmArray sourceArrayType, WasmExpression sourceArray, WasmExpression sourceIndex,
            WasmExpression size
    ) {
        this.targetArrayType = targetArrayType;
        this.targetArray = targetArray;
        this.targetIndex = targetIndex;
        this.sourceArrayType = sourceArrayType;
        this.sourceArray = sourceArray;
        this.sourceIndex = sourceIndex;
        this.size = size;
    }

    public WasmArray getTargetArrayType() {
        return targetArrayType;
    }

    public void setTargetArrayType(WasmArray targetArrayType) {
        this.targetArrayType = targetArrayType;
    }

    public WasmExpression getTargetArray() {
        return targetArray;
    }

    public void setTargetArray(WasmExpression targetArray) {
        this.targetArray = targetArray;
    }

    public WasmExpression getTargetIndex() {
        return targetIndex;
    }

    public void setTargetIndex(WasmExpression targetIndex) {
        this.targetIndex = targetIndex;
    }

    public WasmArray getSourceArrayType() {
        return sourceArrayType;
    }

    public void setSourceArrayType(WasmArray sourceArrayType) {
        this.sourceArrayType = sourceArrayType;
    }

    public WasmExpression getSourceArray() {
        return sourceArray;
    }

    public void setSourceArray(WasmExpression sourceArray) {
        this.sourceArray = sourceArray;
    }

    public WasmExpression getSourceIndex() {
        return sourceIndex;
    }

    public void setSourceIndex(WasmExpression sourceIndex) {
        this.sourceIndex = sourceIndex;
    }

    public WasmExpression getSize() {
        return size;
    }

    public void setSize(WasmExpression size) {
        this.size = size;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
