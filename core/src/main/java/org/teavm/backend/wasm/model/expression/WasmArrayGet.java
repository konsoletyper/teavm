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
import org.teavm.backend.wasm.model.WasmArray;

public class WasmArrayGet extends WasmExpression {
    private WasmArray type;
    private WasmExpression instance;
    private WasmSignedType signedType;
    private WasmExpression index;

    public WasmArrayGet(WasmArray type, WasmExpression instance, WasmExpression index) {
        this.type = Objects.requireNonNull(type);
        this.instance = Objects.requireNonNull(instance);
        this.index = Objects.requireNonNull(index);
    }

    public WasmArray getType() {
        return type;
    }

    public void setType(WasmArray type) {
        this.type = Objects.requireNonNull(type);
    }

    public WasmExpression getInstance() {
        return instance;
    }

    public void setInstance(WasmExpression instance) {
        this.instance = instance;
    }

    public WasmSignedType getSignedType() {
        return signedType;
    }

    public void setSignedType(WasmSignedType signedType) {
        this.signedType = signedType;
    }

    public WasmExpression getIndex() {
        return index;
    }

    public void setIndex(WasmExpression index) {
        this.index = Objects.requireNonNull(index);
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
