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
import org.teavm.backend.wasm.model.WasmStructure;

public class WasmStructGet extends WasmExpression {
    private WasmStructure type;
    private WasmExpression instance;
    private int fieldIndex;
    private WasmSignedType signedType;

    public WasmStructGet(WasmStructure type, WasmExpression instance, int fieldIndex) {
        this.type = Objects.requireNonNull(type);
        this.instance = Objects.requireNonNull(instance);
        this.fieldIndex = fieldIndex;
    }

    public WasmStructure getType() {
        return type;
    }

    public void setType(WasmStructure type) {
        this.type = Objects.requireNonNull(type);
    }

    public WasmExpression getInstance() {
        return instance;
    }

    public void setInstance(WasmExpression instance) {
        this.instance = Objects.requireNonNull(instance);
    }

    public int getFieldIndex() {
        return fieldIndex;
    }

    public void setFieldIndex(int fieldIndex) {
        this.fieldIndex = fieldIndex;
    }

    public WasmSignedType getSignedType() {
        return signedType;
    }

    public void setSignedType(WasmSignedType signedType) {
        this.signedType = signedType;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
