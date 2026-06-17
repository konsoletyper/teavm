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
package org.teavm.flow;

import com.carrotsearch.hppc.sorting.IndirectSort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.teavm.common.Graph;
import org.teavm.common.Loop;
import org.teavm.common.LoopGraph;
import org.teavm.model.BasicBlock;
import org.teavm.model.Program;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.TransitionExtractor;

// Assumes that the input program is guaranteed to be valid:
// - does not form irreducible CFG
// - each block ends with either transition or termination instruction.
public class FlowReconstruction {
    private Program program;
    private Graph cfg;
    private LoopGraph loopGraph;
    private int[] nodeOrder;
    private int[] nodeIndexes;
    private int[] loopEnds;
    private int[][] forwardBlocks;
    private TryCatchInfo currentTryCatch;
    private int currentIndex;
    private int lastLoopIndex;
    private int lastBlocksIndex;

    public List<FlowTreeNode> reconstruct(Program program) {
        this.program = program;
        cfg = ProgramUtils.buildControlFlowGraph(program);
        loopGraph = new LoopGraph(cfg);
        nodeOrder = sort();
        nodeIndexes = new int[nodeOrder.length];
        for (int i = 0; i < nodeOrder.length; ++i) {
            nodeIndexes[nodeOrder[i]] = i;
        }
        loopEnds = findLoopEnds();
        forwardBlocks = findRanges();

        currentTryCatch = null;
        var result = new ArrayList<FlowTreeNode>();
        currentIndex = 0;
        lastLoopIndex = -1;
        lastBlocksIndex = -1;
        reconstructRange(result, nodeOrder.length);
        currentTryCatch = null;

        this.program = null;
        cfg = null;
        loopGraph = null;
        nodeOrder = null;
        nodeIndexes = null;
        loopEnds = null;
        forwardBlocks = null;

        return result;
    }

    // Returns try/catch levels, popped from stack, compared to input state
    private void reconstructRange(List<FlowTreeNode> target, int end) {
        var reconstructor = new RangeReconstructor(target, end);
        reconstructor.reconstruct();
    }

    private class RangeReconstructor {
        List<List<FlowTreeNode>> tryCatchLevels = new ArrayList<>();
        TryCatchInfo localTryCatch;
        List<FlowTreeNode> target;
        int end;

        RangeReconstructor(List<FlowTreeNode> target, int end) {
            this.target = target;
            this.end = end;
            localTryCatch = currentTryCatch;
        }

        void reconstruct() {
            while (currentIndex < end) {
                var node = nodeOrder[currentIndex];
                updateTryCatch(program.basicBlockAt(node));
                closeTryCatches(commonTryCatch(localTryCatch, currentTryCatch));
                if (forwardBlocks[currentIndex * 2] != null && currentIndex * 2 > lastBlocksIndex) {
                    lastBlocksIndex = currentIndex * 2;
                    reconstructBlocks(currentIndex * 2);
                } else if (loopEnds[node] != -1 && currentIndex > lastLoopIndex) {
                    lastLoopIndex = currentIndex;
                    reconstructLoop();
                } else if (forwardBlocks[currentIndex * 2 + 1] != null && currentIndex * 2 + 1 > lastBlocksIndex) {
                    lastBlocksIndex = currentIndex * 2 + 1;
                    reconstructBlocks(currentIndex * 2 + 1);
                } else {
                    openTryCatches();
                    var region = getLastRegion();
                    if (region == null) {
                        region = new FlowTreeNode.Region();
                        target.add(region);
                    }
                    region.blocks.add(program.basicBlockAt(node));
                    ++currentIndex;
                }
            }
        }

        private void reconstructLoop() {
            var node = nodeOrder[currentIndex];
            var loop = new FlowTreeNode.Loop(program.basicBlockAt(node));
            reconstructRange(loop.body, nodeIndexes[loopEnds[node]]);
            var commonTryCatch = commonTryCatch(localTryCatch, currentTryCatch);
            closeTryCatches(commonTryCatch);
            target.add(loop);
            currentTryCatch = commonTryCatch;
        }

        private void reconstructBlocks(int index) {
            FlowTreeNode.Block block = null;
            var initialTryCatch = currentTryCatch;
            for (var endIndex : forwardBlocks[index]) {
                var end = nodeOrder[endIndex];
                var newBlock = new FlowTreeNode.Block(program.basicBlockAt(end));
                if (block != null) {
                    newBlock.body.add(block);
                }
                block = newBlock;
                var rangeReconstructor = new RangeReconstructor(block.body, endIndex);
                rangeReconstructor.localTryCatch = initialTryCatch;
                rangeReconstructor.reconstruct();

                var isClosingExceptionHandler = false;
                while (currentTryCatch != null && currentTryCatch.handler.getIndex() == end) {
                    currentTryCatch = currentTryCatch.next;
                    isClosingExceptionHandler = true;
                }
                if (isClosingExceptionHandler) {
                    rangeReconstructor.closeTryCatches(currentTryCatch);
                    initialTryCatch = currentTryCatch;
                }
                initialTryCatch = commonTryCatch(initialTryCatch, currentTryCatch);
            }
            closeTryCatches(commonTryCatch(localTryCatch, currentTryCatch));
            openTryCatches();
            target.add(block);
        }

        private FlowTreeNode.Region getLastRegion() {
            if (target.isEmpty()) {
                return null;
            }
            var last = target.get(target.size() - 1);
            if (last instanceof FlowTreeNode.Region region) {
                return region;
            }
            return null;
        }

        private void closeTryCatches(TryCatchInfo commonTryCatch) {
            if (target.isEmpty()) {
               localTryCatch = commonTryCatch;
            } else {
                while (localTryCatch != commonTryCatch) {
                    if (tryCatchLevels.isEmpty()) {
                        var containingTryCatchNode = new FlowTreeNode.TryCatch(localTryCatch.handler,
                                localTryCatch.exceptionType);
                        containingTryCatchNode.tryBody.addAll(target);
                        target.clear();
                        target.add(containingTryCatchNode);
                    } else {
                        target = tryCatchLevels.remove(tryCatchLevels.size() - 1);
                    }
                    localTryCatch = localTryCatch.next;
                }
            }
        }

        private void openTryCatches() {
            var tryCatch = currentTryCatch;
            if (tryCatch == localTryCatch) {
                return;
            }
            FlowTreeNode.TryCatch currentTryCatchNode = null;
            var startLevel = tryCatchLevels.size();
            tryCatchLevels.add(target);
            FlowTreeNode.TryCatch firstCreatedTryCatchNode = null;
            while (tryCatch != localTryCatch) {
                var newTryCatchNode = new FlowTreeNode.TryCatch(tryCatch.handler, tryCatch.exceptionType);
                if (currentTryCatchNode != null) {
                    newTryCatchNode.tryBody.add(currentTryCatchNode);
                    tryCatchLevels.add(newTryCatchNode.tryBody);
                } else {
                    firstCreatedTryCatchNode = newTryCatchNode;
                }
                tryCatchLevels.add(newTryCatchNode.tryBody);
                currentTryCatchNode = newTryCatchNode;
                tryCatch = tryCatch.next;
            }
            Collections.reverse(tryCatchLevels.subList(startLevel, tryCatchLevels.size()));
            target.add(currentTryCatchNode);
            target = firstCreatedTryCatchNode.tryBody;
            localTryCatch = currentTryCatch;
        }
    }

    private int[] sort() {
        var stack = new int[program.basicBlockCount() * 2];
        var state = new int[program.basicBlockCount() * 2];
        var stackTop = 0;
        stack[stackTop++] = 0;
        var result = new int[program.basicBlockCount()];
        var resultIndex = result.length - 1;
        while (stackTop > 0) {
            var node = stack[--stackTop];
            switch (state[node]) {
                case 0: {
                    state[node] = 1;
                    stack[stackTop++] = node;
                    var successors = extractOrderedSuccessors(node);
                    for (var successor : successors) {
                        if (state[successor] == 0) {
                            stack[stackTop++] = successor;
                        }
                    }
                    break;
                }
                case 1:
                    result[resultIndex--] = node;
                    state[node] = 2;
                    break;
                case 2:
                    // do nothing
            }
        }
        return result;
    }

    private int[] extractOrderedSuccessors(int node) {
        var successors = cfg.outgoingEdges(node);
        if (successors.length > 4) {
            return IndirectSort.mergesort(successors, (a, b) -> compareSuccessors(node, a, b));
        } else {
            for (var i = 0; i < successors.length; ++i) {
                var e = successors[i];
                var newIndex = i;
                for (var j = i + 1; j < successors.length; ++j) {
                   if (compareSuccessors(node, successors[newIndex], successors[j]) < 0) {
                       newIndex = j;
                   }
                }
                if (newIndex != i) {
                    successors[i] = successors[newIndex];
                    successors[newIndex] = e;
                }
            }
            return successors;
        }
    }

    private int compareSuccessors(int node, int a, int b) {
        var r = commonTryCatchSize(node, a) - commonTryCatchSize(node, b);
        if (r == 0) {
            var loopDepth = loopDepth(node);
            if (loopDepth > 0) {
                var aBreaks = commonLoopDepth(node, a) < loopDepth;
                var bBreaks = commonLoopDepth(node, b) < loopDepth;
                if (aBreaks != bBreaks) {
                    r = aBreaks ? -1 : 1;
                }
            }
        }
        return r;
    }

    private int commonTryCatchSize(int a, int b) {
        var block1 = program.basicBlockAt(a);
        var block2 = program.basicBlockAt(b);
        var min = Math.min(block1.getTryCatchBlocks().size(), block2.getTryCatchBlocks().size());
        for (var i = 0; i < min; ++i) {
            var tryCatch1 = block1.getTryCatchBlocks().get(i);
            var tryCatch2 = block2.getTryCatchBlocks().get(i);
            if (tryCatch1.getHandler() != tryCatch2.getHandler()
                    || !Objects.equals(tryCatch1.getExceptionType(), tryCatch2.getExceptionType())) {
                return i;
            }
        }
        return min;
    }

    private int loopDepth(int node) {
        var loop = loopGraph.loopAt(node);
        var depth = 0;
        while (loop != null) {
            loop = loop.getParent();
            ++depth;
        }
        return depth;
    }

    private int commonLoopDepth(int a, int b) {
        var loop1 = loopGraph.loopAt(a);
        var loop2 = loopGraph.loopAt(b);
        if (loop1 == null || loop2 == null) {
            return 0;
        }
        var depth1 = depth(loop1);
        var depth2 = depth(loop2);
        var common = Math.min(depth1, depth2);
        while (depth1 > common) {
            loop1 = loop1.getParent();
            --depth1;
        }
        while (depth2 > common) {
            loop2 = loop2.getParent();
            --depth2;
        }
        while (loop1 != loop2) {
            loop1 = loop1.getParent();
            loop2 = loop2.getParent();
            --common;
        }

        return common;
    }

    private int depth(Loop loop) {
        var result = 0;
        while (loop != null) {
            result++;
            loop = loop.getParent();
        }
        return result;
    }

    private int[] findLoopEnds() {
        var result = new int[nodeOrder.length];
        Arrays.fill(result, -1);
        Loop previousLoop = null;
        for (var i = 0; i < nodeOrder.length; ++i) {
            var node = nodeOrder[i];
            var loop = loopGraph.loopAt(node);
            if (loop != previousLoop) {
                if (previousLoop != null && (loop == null || !loop.isChildOf(previousLoop))) {
                    result[previousLoop.getHead()] = node;
                }
                previousLoop = loop;
            } else if (hasSuccessor(cfg.outgoingEdges(node), node)) {
                result[node] = i + 1 < nodeOrder.length ? nodeOrder[i + 1] : nodeOrder.length;
            }
        }
        if (previousLoop != null) {
            result[previousLoop.getHead()] = nodeOrder.length;
        }
        return result;
    }

    private boolean hasSuccessor(int[] successors, int successor) {
        for (var candidate : successors) {
            if (candidate == successor) {
                return true;
            }
        }
        return false;
    }

    private int[][] findRanges() {
        // Step 1. For each node B calculate the most early node A (in ordering) so that A jumps to B
        // Additionally, take loops into account. That is, in case there's a jump from *any* node in loop L,
        // we replace node A with head(L).
        var startPositions = new int[nodeOrder.length];
        for (var i = 0; i < nodeOrder.length; ++i) {
            startPositions[i] = i * 2 + 1;
        }
        var transitionExtractor = new TransitionExtractor();
        var targets = new ArrayList<BasicBlock>();
        for (var i = 0; i < nodeOrder.length; ++i) {
            var node = nodeOrder[i];
            var block = program.basicBlockAt(node);
            block.getLastInstruction().acceptVisitor(transitionExtractor);
            var jumpTargets = transitionExtractor.getTargets();
            if (jumpTargets != null) {
                targets.addAll(Arrays.asList(jumpTargets));
            }
            for (var tryCatch : block.getTryCatchBlocks()) {
                targets.add(tryCatch.getHandler());
            }
            if (!targets.isEmpty()) {
                var nodeLoop = loopGraph.loopAt(node);
                for (var target : targets) {
                    var targetIndex = nodeIndexes[target.getIndex()];
                    if (targetIndex > i + 1) {
                        var sourceIndex = i * 2 + 1;
                        var targetLoop = loopGraph.loopAt(target.getIndex());
                        var loop = nodeLoop;
                        while (loop != targetLoop) {
                            sourceIndex = nodeIndexes[loop.getHead()] * 2;
                            loop = loop.getParent();
                        }
                        if (sourceIndex < startPositions[targetIndex]) {
                            startPositions[targetIndex] = sourceIndex;
                        }
                    }
                }
                targets.clear();
            }
        }

        // Step 2. Update startPositions so that there are no intersecting ranges,
        // Move from the last to the first, maintain stack of ranges: push to the stack
        // when we find range end, pop when we reach range start or beyond it.
        var stack = new int[nodeOrder.length];
        var stackTop = 0;
        for (var i = nodeOrder.length - 1; i >= 0; --i) {
            var pos = i * 2 + 1;
            while (stackTop > 0 && pos <= startPositions[stack[stackTop - 1]]) {
                startPositions[stack[--stackTop]] = pos;
            }
            --pos;
            while (stackTop > 0 && pos <= startPositions[stack[stackTop - 1]]) {
                startPositions[stack[--stackTop]] = pos;
            }
            var source = startPositions[i];
            if (source / 2 != i) {
                stack[stackTop++] = i;
            }
        }

        // Step 3. Calculate numbers of starting ranges for each node
        var rangeCount = new int[nodeOrder.length * 2];
        for (var i = 0; i < nodeOrder.length; ++i) {
            var start = startPositions[i];
            if (start / 2 != i) {
                rangeCount[start]++;
            }
        }

        // Step 4. Initialize range end nodes for each starting node
        var result = new int[nodeOrder.length * 2][];
        for (var i = 0; i < nodeOrder.length; ++i) {
            var count = rangeCount[i];
            if (count > 0) {
                result[i] = new int[count];
                rangeCount[i] = 0;
            }
        }

        // Step 5. Fill range start arrays
        for (var i = 0; i < nodeOrder.length; ++i) {
            var start = startPositions[i];
            if (start / 2 != i) {
                result[start][rangeCount[start]++] = i;
            }
        }

        return result;
    }

    private void updateTryCatch(BasicBlock block) {
        var index = block.getTryCatchBlocks().size();
        if (currentTryCatch == null) {
            index = 0;
        } else if (currentTryCatch.depth > index) {
            while (currentTryCatch != null && currentTryCatch.depth > index) {
                currentTryCatch = currentTryCatch.next;
            }
        } else {
            index = currentTryCatch.depth;
        }
        while (index > 0) {
            var tryCatchBlock = block.getTryCatchBlocks().get(block.getTryCatchBlocks().size() - index);
            if (tryCatchBlock.getExceptionType().equals(currentTryCatch.exceptionType)
                    && tryCatchBlock.getHandler() == currentTryCatch.handler) {
                break;
            }
            currentTryCatch = currentTryCatch.next;
            --index;
        }
        for (var i = index; i < block.getTryCatchBlocks().size(); ++i) {
            var tryCatchBlock = block.getTryCatchBlocks().get(block.getTryCatchBlocks().size() - i - 1);
            currentTryCatch = new TryCatchInfo(tryCatchBlock.getExceptionType(), tryCatchBlock.getHandler(),
                    currentTryCatch, i + 1);
        }
    }

    private static TryCatchInfo commonTryCatch(TryCatchInfo a, TryCatchInfo b) {
        if (a == b) {
            return a;
        }
        if (a == null || b == null) {
            return null;
        }
        while (a.depth > b.depth) {
            a = a.next;
        }
        while (b.depth > a.depth) {
            b = b.next;
        }
        while (a != b) {
            a = a.next;
            b = b.next;
        }
        return a;
    }

    private static class TryCatchInfo {
        String exceptionType;
        BasicBlock handler;
        TryCatchInfo next;
        int depth;

        TryCatchInfo(String exceptionType, BasicBlock handler, TryCatchInfo next, int depth) {
            this.exceptionType = exceptionType;
            this.handler = handler;
            this.next = next;
            this.depth = depth;
        }
    }
}
