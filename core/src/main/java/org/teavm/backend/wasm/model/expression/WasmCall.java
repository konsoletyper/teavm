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

public class WasmCall extends WasmExpression {
    private String functionName;
    private boolean imported;
    private List<WasmExpression> arguments = new ArrayList<>();

    public WasmCall(String functionName, boolean imported) {
        Objects.requireNonNull(functionName);
        this.functionName = functionName;
        this.imported = imported;
    }

    public WasmCall(String functionName) {
        this(functionName, false);
    }

    public WasmCall(String functionName, WasmExpression... arguments) {
        this(functionName);
        getArguments().addAll(Arrays.asList(arguments));
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        Objects.requireNonNull(functionName);
        this.functionName = functionName;
    }

    public List<WasmExpression> getArguments() {
        return arguments;
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }
}
