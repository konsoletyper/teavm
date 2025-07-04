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
import java.util.Set;
import org.teavm.backend.wasm.model.WasmBlockType;

public class WasmConditional extends WasmExpression {
    private WasmExpression condition;
    private WasmBlock thenBlock = new WasmBlock(false);
    private WasmBlock elseBlock = new WasmBlock(false);
    private WasmBlockType type;

    public WasmConditional(WasmExpression condition) {
        Objects.requireNonNull(condition);
        this.condition = condition;
    }

    public WasmExpression getCondition() {
        return condition;
    }

    public void setCondition(WasmExpression condition) {
        Objects.requireNonNull(condition);
        this.condition = condition;
    }

    public WasmBlock getThenBlock() {
        return thenBlock;
    }

    public WasmBlock getElseBlock() {
        return elseBlock;
    }

    public WasmBlockType getType() {
        return type;
    }

    public void setType(WasmBlockType type) {
        this.type = type;
    }

    @Override
    public void acceptVisitor(WasmExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected boolean isTerminating(Set<WasmBlock> blocks) {
        blocks.add(thenBlock);
        blocks.add(elseBlock);
        var result = thenBlock.isTerminating(blocks) && elseBlock.isTerminating(blocks);
        blocks.remove(elseBlock);
        blocks.remove(thenBlock);
        return result;
    }
}
