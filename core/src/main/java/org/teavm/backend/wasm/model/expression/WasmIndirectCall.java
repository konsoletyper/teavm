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
import org.teavm.backend.wasm.model.WasmType;

public class WasmIndirectCall extends WasmExpression {
    private List<WasmType> parameterTypes = new ArrayList<>();
    private WasmType returnType;
    private WasmExpression selector;
    private List<WasmExpression> arguments = new ArrayList<>();

    public WasmIndirectCall(WasmExpression selector) {
        Objects.requireNonNull(selector);
        this.selector = selector;
    }

    public WasmExpression getSelector() {
        return selector;
    }

    public void setSelector(WasmExpression selector) {
        Objects.requireNonNull(selector);
        this.selector = selector;
    }

    public List<WasmType> getParameterTypes() {
        return parameterTypes;
    }

    public WasmType getReturnType() {
        return returnType;
    }

    public void setReturnType(WasmType returnType) {
        this.returnType = returnType;
    }

    public List<WasmExpression> getArguments() {
        return arguments;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
