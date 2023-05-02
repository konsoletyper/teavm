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

public abstract class Block {
    Block next;
    Block first = this;
    Block previous;
    TryCatchNode[] tryCatches;

    public Block getNext() {
        return next;
    }

    Block append(Block block) {
        var last = this;
        while (block != null) {
            var next = block.next;
            last.appendSingle(block);
            last = block;
            block = next;
        }
        return last;
    }

    private void appendSingle(Block block) {
        block.first = first;
        block.previous = this;
        next = block;
    }

    public abstract void acceptVisitor(BlockVisitor visitor);

    @Override
    public String toString() {
        return new BlockPrinter().print(this);
    }
}
