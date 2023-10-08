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
import org.teavm.backend.wasm.model.WasmType;

public class WasmBlock extends WasmExpression {
    private boolean loop;
    private List<WasmExpression> body = new ArrayList<>();
    private WasmType type;

    public WasmBlock(boolean loop) {
        this.loop = loop;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public List<WasmExpression> getBody() {
        return body;
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
    public boolean isTerminating() {
        if (loop) {
            return false;
        }
        return !body.isEmpty() && body.get(body.size() - 1).isTerminating();
    }
}
