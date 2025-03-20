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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.teavm.backend.wasm.model.WasmFunctionType;

public class WasmCallReference extends WasmExpression {
    private WasmFunctionType type;
    private WasmExpression functionReference;
    private List<WasmExpression> arguments = new ArrayList<>();

    public WasmCallReference(WasmExpression functionReference, WasmFunctionType type) {
        this.functionReference = Objects.requireNonNull(functionReference);
        this.type = Objects.requireNonNull(type);
    }

    public WasmCallReference(WasmExpression functionReference, WasmFunctionType type, WasmExpression... arguments) {
        this(functionReference, type);
        getArguments().addAll(List.of(arguments));
    }

    public WasmExpression getFunctionReference() {
        return functionReference;
    }

    public void setFunctionReference(WasmExpression functionReference) {
        this.functionReference = Objects.requireNonNull(functionReference);
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
