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
package org.teavm.model.util;

import org.teavm.common.IntegerArray;
import org.teavm.common.Loop;
import org.teavm.common.LoopGraph;

public final class ControlFlowUtils {
    private ControlFlowUtils() {
    }

    public static int[][] findLoopExits(LoopGraph cfg) {
        IntegerArray[] loops = new IntegerArray[cfg.size()];
        for (Loop loop : cfg.knownLoops()) {
            loops[loop.getHead()] = new IntegerArray(4);
        }

        for (int node = 0; node < cfg.size(); ++node) {
            Loop loop = cfg.loopAt(node);
            while (loop != null) {
                for (int successor : cfg.outgoingEdges(node)) {
                    Loop successorLoop = cfg.loopAt(successor);
                    if (successorLoop == null || !successorLoop.isChildOf(loop)) {
                        loops[loop.getHead()].add(node);
                        break;
                    }
                }
                loop = loop.getParent();
            }
        }

        int[][] result = new int[cfg.size()][];
        for (int i = 0; i < loops.length; ++i) {
            IntegerArray builder = loops[i];
            if (builder != null) {
                result[i] = builder.getAll();
            }
        }

        return result;
    }
}
