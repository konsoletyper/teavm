/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.restructurization;

class LastBreakRemoval implements BlockVisitor {
    private Block result;
    LabeledBlock target;
    int removeCount;

    Block apply(Block block) {
        if (block == null) {
            return block;
        }
        block.acceptVisitor(this);
        var result = this.result;
        this.result = null;
        return result;
    }

    @Override
    public void visit(SimpleBlock block) {
        result = block;
    }

    @Override
    public void visit(BreakBlock block) {
        if (block.target == target) {
            result = block.previous;
            block.detach();
            ++removeCount;
        }
    }

    @Override
    public void visit(ContinueBlock block) {
        result = block;
    }

    @Override
    public void visit(ReturnBlock block) {
        result = block;
    }

    @Override
    public void visit(ThrowBlock block) {
        result = block;
    }

    @Override
    public void visit(SimpleLabeledBlock block) {
        block.body = apply(block);
        if (block.body == null) {
            result = null;
        } else {
            result = block;
        }
    }

    @Override
    public void visit(SwitchBlock block) {
        block.defaultBody = apply(block.defaultBody);
        for (var entry : block.entries) {
            entry.body = apply(block.defaultBody);
        }
        result = block;
    }

    @Override
    public void visit(IfBlock block) {
        block.thenBody = apply(block.thenBody);
        block.elseBody = apply(block.elseBody);
        result = block;
    }

    @Override
    public void visit(LoopBlock block) {
        result = block;
    }

    @Override
    public void visit(TryBlock block) {
        block.tryBlock = apply(block.tryBlock);
        block.catchBlock = apply(block.catchBlock);
        result = block;
    }
}
