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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.teavm.backend.wasm.model.WasmFunction;

public class WasmCall extends WasmExpression {
    private WasmFunction function;
    private List<WasmExpression> arguments = new ArrayList<>();
    private boolean suspensionPoint;

    public WasmCall(WasmFunction function) {
        this.function = Objects.requireNonNull(function);
    }

    public WasmCall(WasmFunction functionName, WasmExpression... arguments) {
        this(functionName);
        getArguments().addAll(Arrays.asList(arguments));
    }

    public WasmFunction getFunction() {
        return function;
    }

    public void setFunction(WasmFunction function) {
        this.function = Objects.requireNonNull(function);
    }

    public List<WasmExpression> getArguments() {
        return arguments;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }

    public boolean isSuspensionPoint() {
        return suspensionPoint;
    }

    public void setSuspensionPoint(boolean suspensionPoint) {
        this.suspensionPoint = suspensionPoint;
    }
}
