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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.teavm.backend.wasm.model.WasmFunctionType;

public class WasmIndirectCall extends WasmExpression {
    private WasmFunctionType type;
    private WasmExpression selector;
    private List<WasmExpression> arguments = new ArrayList<>();

    public WasmIndirectCall(WasmExpression selector, WasmFunctionType type) {
        this.selector = Objects.requireNonNull(selector);
        this.type = Objects.requireNonNull(type);
    }

    public WasmExpression getSelector() {
        return selector;
    }

    public void setSelector(WasmExpression selector) {
        this.selector = Objects.requireNonNull(selector);
    }

    public List<WasmExpression> getArguments() {
        return arguments;
    }

    public WasmFunctionType getType() {
        return type;
    }

    public void setType(WasmFunctionType type) {
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
