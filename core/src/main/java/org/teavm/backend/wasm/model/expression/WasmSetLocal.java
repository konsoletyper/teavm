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
import org.teavm.backend.wasm.model.WasmLocal;

public class WasmSetLocal extends WasmExpression {
    private WasmLocal local;
    private WasmExpression value;

    public WasmSetLocal(WasmLocal local, WasmExpression value) {
        Objects.requireNonNull(local);
        Objects.requireNonNull(value);
        this.local = local;
        this.value = value;
    }

    public WasmLocal getLocal() {
        return local;
    }

    public void setLocal(WasmLocal local) {
        Objects.requireNonNull(local);
        this.local = local;
    }

    public WasmExpression getValue() {
        return value;
    }

    public void setValue(WasmExpression value) {
        Objects.requireNonNull(value);
        this.value = value;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
