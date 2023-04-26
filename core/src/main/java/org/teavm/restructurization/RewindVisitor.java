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

class RewindVisitor implements BlockVisitor {
    Block rewind(Block block) {
        if (block == null) {
            return null;
        }
        block = block.first;
        var result = block;
        while (block != null) {
            block.acceptVisitor(this);
            block = block.next;
        }
        return result;
    }

    @Override
    public void visit(SimpleBlock block) {
    }

    @Override
    public void visit(BreakBlock block) {
    }

    @Override
    public void visit(ContinueBlock block) {
    }

    @Override
    public void visit(ReturnBlock block) {
    }

    @Override
    public void visit(ThrowBlock block) {
    }

    @Override
    public void visit(SimpleLabeledBlock block) {
        block.body = rewind(block.body);
    }

    @Override
    public void visit(SwitchBlock block) {
        for (var entry : block.entries) {
            entry.body = rewind(entry.body);
        }
        block.defaultBody = rewind(block.defaultBody);
    }

    @Override
    public void visit(IfBlock block) {
        block.thenBody = rewind(block.thenBody);
        block.elseBody = rewind(block.elseBody);
    }

    @Override
    public void visit(LoopBlock block) {
        block.body = rewind(block.body);
    }

    @Override
    public void visit(TryBlock block) {
        block.tryBlock = rewind(block.tryBlock);
        block.catchBlock = rewind(block.catchBlock);
    }
}
