/*
 *  Copyright 2026 Alexey Andreev.
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

public class WasmSequence extends WasmExpression {
    private List<WasmExpression> body = new ArrayList<>();

    public List<WasmExpression> getBody() {
        return body;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected boolean isTerminating(Set<WasmBlock> blocks) {
        if (body.isEmpty()) {
            return false;
        }
        return body.get(body.size() - 1).isTerminating(blocks);
    }
}
