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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class TryCatchProcessor {
    private List<Block> openingBlocks = new ArrayList<>();
    private Block result;
    private Block current;

    Block processTryCatches(Block block) {
        if (block == null) {
            return null;
        }

        current = block.first;
        result = current;
        current = current.next;
        int minSize = result.tryCatches.length;
        while (current != null) {
            var next = current.next;
            var commonSize = Math.min(result.tryCatches.length, current.tryCatches.length);
            while (commonSize > 0 && !result.tryCatches[commonSize - 1].sameAs(current.tryCatches[commonSize - 1])) {
                --commonSize;
            }
            minSize = Math.min(minSize, commonSize);
            popOld(commonSize);
            pushNew();

            result = current;
            current = next;
        }

        popOld(minSize);

        var resultBlock = result;
        cleanup();
        return resultBlock;
    }

    private void popOld(int size) {
        var block = result;
        var initialTryCatches = result.tryCatches;
        while (openingBlocks.size() > size) {
            var from = openingBlocks.size();
            block = openingBlocks.remove(openingBlocks.size() - 1);
            while (openingBlocks.size() > size && openingBlocks.get(openingBlocks.size() - 1) == block) {
                openingBlocks.remove(openingBlocks.size() - 1);
            }

            block.detach();
            clearTryCatches(block);
            for (var i = from - 1; i >= openingBlocks.size(); --i) {
                var tryCatch = initialTryCatches[i];
                var tryBlock = new TryBlock();
                tryBlock.tryBlock = block;
                tryBlock.catchBlock = tryCatch.handler;
                tryBlock.exception = tryCatch.variable;
                tryBlock.exceptionType = tryCatch.exceptionType;
                block = tryBlock;
            }
        }
        if (block != result) {
            var index = openingBlocks.size() - 1;
            while (index >= 0 && openingBlocks.get(index) == result) {
                openingBlocks.set(index, block);
                --index;
            }
            block.tryCatches = Arrays.copyOf(initialTryCatches, size);
            result = block;
        }
    }

    private void pushNew() {
        while (openingBlocks.size() < current.tryCatches.length) {
            openingBlocks.add(result);
        }
    }

    private void clearTryCatches(Block block) {
        while (block != null) {
            block.tryCatches = null;
            block = block.next;
        }
    }

    private void cleanup() {
        openingBlocks.clear();
        current = null;
    }
}
