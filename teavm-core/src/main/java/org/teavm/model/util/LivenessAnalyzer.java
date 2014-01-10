/*
 *  Copyright 2014 Alexey Andreev.
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

import java.util.ArrayList;
import java.util.List;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.Program;

/**
 *
 * @author Alexey Andreev
 */
public class LivenessAnalyzer {
    private BackNode[] backNodeGraph;
    private int[] domLeft;
    private int[] domRight;

    public void analyze(Program program) {
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        DominatorTree domTree = GraphUtils.buildDominatorTree(cfg);
        Graph domGraph = GraphUtils.buildDominatorGraph(domTree, cfg.size());
        computeDomLeftRight(domGraph);
        prepareBackGraph(domTree, cfg);
    }

    private void computeDomLeftRight(Graph domGraph) {
        domLeft = new int[domGraph.size()];
        domRight = new int[domGraph.size()];
        int index = 1;
        int[] stack = new int[domGraph.size() * 2];
        int top = 0;
        for (int i = domGraph.size(); i >= 0; --i) {
            if (domGraph.incomingEdgesCount(i) == 0) {
                stack[top++] = i;
            }
        }
        while (top > 0) {
            int v = stack[top--];
            if (domLeft[v] == 0) {
                domLeft[v] = index++;
                for (int succ : domGraph.outgoingEdges(v)) {
                    stack[top++] = succ;
                }
            } else if (domRight[v] == 0) {
                domRight[v] = index++;
                if (domGraph.incomingEdgesCount(0) > 0) {
                    stack[top++] = domGraph.incomingEdges(v)[0];
                }
            }
        }
    }

    private void prepareBackGraph(DominatorTree domTree, Graph cfg) {
        backNodeGraph = new BackNode[cfg.size() * 2];
        int[] stack = new int[cfg.size() * 2];
        int top = 0;
        for (int i = 0; i < cfg.size(); ++i) {
            if (cfg.outgoingEdgesCount(i) == 0) {
                stack[top++] = i;
            }
        }
        while (top > 0) {

        }
    }

    private boolean dominates(int a, int b) {
        return domLeft[a] <= domLeft[b] && domRight[a] >= domRight[b];
    }

    private static class BackNode {
        final List<BackNode> successors = new ArrayList<>();
        int blockIndex;
    }
}
