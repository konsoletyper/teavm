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
package org.teavm.backend.wasm.model;

import java.util.Objects;
import org.teavm.backend.wasm.model.expression.WasmExpression;

public class WasmGlobal extends WasmEntity {
    private String name;
    private WasmType type;
    private WasmExpression initialValue;

    public WasmGlobal(String name, WasmType type, WasmExpression initialValue) {
        this.name = name;
        this.type = Objects.requireNonNull(type);
        this.initialValue = Objects.requireNonNull(initialValue);
    }

    public String getName() {
        return name;
    }

    public WasmType getType() {
        return type;
    }

    public void setType(WasmType type) {
        this.type = Objects.requireNonNull(type);
    }

    public WasmExpression getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(WasmExpression initialValue) {
        this.initialValue = Objects.requireNonNull(initialValue);
    }
}
