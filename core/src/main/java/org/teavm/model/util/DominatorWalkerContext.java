/*
 *  Copyright 2019 Alexey Andreev.
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

import org.teavm.common.Graph;

public interface DominatorWalkerContext {
    /**
     * Returns control flow graph, where node {@code 2 * i} is the entry of block {@code i}
     * and node {@code 2 * i + 1} is its body.
     *
     * @see ProgramUtils#buildControlFlowGraph2(org.teavm.model.Program)
     */
    Graph getControlFlowGraph();

    boolean isVisited(int blockIndex);

    boolean isExceptionHandler(int blockIndex);

    /**
     * <p>Returns the only predecessor of the given block, if the block can be entered only by a single normal
     * (non-exceptional) edge. Otherwise, returns {@code -1}.</p>
     *
     * <p>Only such blocks should be refined by facts derived from the predecessor's branch condition.
     * Being dominated by the branching block is not sufficient, since another path can join the target
     * block, bypassing the edge where the condition holds.</p>
     */
    int getSinglePredecessor(int blockIndex);

    default boolean allPredecessorsVisited(int blockIndex) {
        for (int predecessor : getControlFlowGraph().incomingEdges(blockIndex * 2)) {
            if (!isVisited(predecessor / 2)) {
                return false;
            }
        }
        return true;
    }
}
