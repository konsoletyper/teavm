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
import java.util.Set;
import org.teavm.backend.wasm.model.WasmType;

public class WasmTry extends WasmExpression {
    private List<WasmExpression> body = new ArrayList<>();
    private List<WasmCatch> catches = new ArrayList<>();
    private WasmType type;

    public List<WasmExpression> getBody() {
        return body;
    }

    public List<WasmCatch> getCatches() {
        return catches;
    }

    public WasmType getType() {
        return type;
    }

    public void setType(WasmType type) {
        this.type = type;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected boolean isTerminating(Set<WasmBlock> blocks) {
        return !body.isEmpty() && body.get(body.size() - 1).isTerminating(blocks);
    }
}
