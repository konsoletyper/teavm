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

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.sorting.IndirectSort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
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
    private DominatorTree domTree;
    // For each loop head: node that should be placed right after the loop, -1 if none
    private int[] loopFollows;
    // For each node: whether it leaves some loop and should be placed inside this loop's range,
    // right after the node that jumps to it
    private boolean[] insideExits;
    private boolean[] handlers;
    private int[] nodeOrder;
    private int[] nodeIndexes;
    // For each loop head node: position in nodeOrder right after the end of the loop, -1 if node is not a loop head
    private int[] loopEnds;
    // For each position: position of the head of the innermost loop range containing it, -1 if none
    private int[] innermostLoop;
    // For each position of loop head: position of the head of the enclosing loop range, -1 if none
    private int[] parentLoop;
    private int[][] forwardBlocks;
    private TryCatchInfo currentTryCatch;
    private int currentIndex;
    private int lastLoopIndex;
    private int lastBlocksIndex;

    public List<FlowTreeNode> reconstruct(Program program) {
        this.program = program;
        cfg = ProgramUtils.buildControlFlowGraph(program);
        loopGraph = new LoopGraph(cfg);
        domTree = GraphUtils.buildDominatorTree(cfg);
        findLoopExits();
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
        domTree = null;
        loopFollows = null;
        insideExits = null;
        handlers = null;
        nodeOrder = null;
        nodeIndexes = null;
        loopEnds = null;
        innermostLoop = null;
        parentLoop = null;
        forwardBlocks = null;

        return result;
    }

    private void reconstructRange(List<FlowTreeNode> target, int end) {
        var reconstructor = new RangeReconstructor(target, end);
        reconstructor.reconstruct();
    }

    private class RangeReconstructor {
        // For each try/catch node opened by this reconstructor: target to restore after closing it
        List<List<FlowTreeNode>> tryCatchLevels = new ArrayList<>();
        List<FlowTreeNode.TryCatch> openTryCatchNodes = new ArrayList<>();
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
                closeTryCatches(commonTryCatch(localTryCatch, currentTryCatch), program.basicBlockAt(node));
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

            // Try/catch nodes that remain open are the last ones in range, so they complete normally
            // by falling through to the end of range
            var blockAfter = end < nodeOrder.length ? program.basicBlockAt(nodeOrder[end]) : null;
            for (var tryCatchNode : openTryCatchNodes) {
                tryCatchNode.blockAfter = blockAfter;
            }
        }

        private void reconstructLoop() {
            var node = nodeOrder[currentIndex];
            var loop = new FlowTreeNode.Loop(program.basicBlockAt(node));
            reconstructRange(loop.body, loopEnds[node]);
            var commonTryCatch = commonTryCatch(localTryCatch, currentTryCatch);
            closeTryCatches(commonTryCatch, loop.head);
            target.add(loop);
            currentTryCatch = commonTryCatch;
        }

        private void reconstructBlocks(int index) {
            FlowTreeNode.Block block = null;
            var initialTryCatch = currentTryCatch;
            var firstBlock = program.basicBlockAt(nodeOrder[currentIndex]);
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

                // Close try/catches that don't cover block's target inside the block. Otherwise, breaking
                // out of the block would fall off the end of try body instead of leaving try/catch.
                var tryCatchAfter = commonTryCatch(currentTryCatch, block.jumpTarget);
                if (tryCatchAfter != currentTryCatch) {
                    currentTryCatch = tryCatchAfter;
                    rangeReconstructor.closeTryCatches(currentTryCatch, block.jumpTarget);
                }
                initialTryCatch = commonTryCatch(initialTryCatch, currentTryCatch);
            }
            closeTryCatches(commonTryCatch(localTryCatch, currentTryCatch), firstBlock);
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

        // blockAfter is the block that receives control after closed try/catch nodes
        private void closeTryCatches(TryCatchInfo commonTryCatch, BasicBlock blockAfter) {
            while (localTryCatch != commonTryCatch) {
                if (!tryCatchLevels.isEmpty()) {
                    target = tryCatchLevels.remove(tryCatchLevels.size() - 1);
                    openTryCatchNodes.remove(openTryCatchNodes.size() - 1).blockAfter = blockAfter;
                } else if (!target.isEmpty()) {
                    // Try/catch was assumed by the code that created this reconstructor, but not actually
                    // created. Wrap everything produced so far.
                    var containingTryCatchNode = new FlowTreeNode.TryCatch(blockAfter,
                            localTryCatch.handler, localTryCatch.exceptionType);
                    containingTryCatchNode.tryBody.addAll(target);
                    target.clear();
                    target.add(containingTryCatchNode);
                }
                localTryCatch = localTryCatch.next;
            }
        }

        private void openTryCatches() {
            if (currentTryCatch == localTryCatch) {
                return;
            }
            var tryCatches = new ArrayList<TryCatchInfo>();
            for (var tryCatch = currentTryCatch; tryCatch != localTryCatch; tryCatch = tryCatch.next) {
                tryCatches.add(tryCatch);
            }
            // Open from outermost to innermost
            for (var i = tryCatches.size() - 1; i >= 0; --i) {
                var tryCatch = tryCatches.get(i);
                var tryCatchNode = new FlowTreeNode.TryCatch(null, tryCatch.handler, tryCatch.exceptionType);
                target.add(tryCatchNode);
                tryCatchLevels.add(target);
                openTryCatchNodes.add(tryCatchNode);
                target = tryCatchNode.tryBody;
            }
            localTryCatch = currentTryCatch;
        }
    }

    private int[] sort() {
        var stack = new IntArrayList();
        var state = new int[program.basicBlockCount()];
        stack.add(0);
        var result = new int[program.basicBlockCount()];
        var resultIndex = result.length - 1;
        while (!stack.isEmpty()) {
            var node = stack.removeAt(stack.size() - 1);
            switch (state[node]) {
                case 0: {
                    state[node] = 1;
                    stack.add(node);
                    var successors = extractOrderedSuccessors(node);
                    for (var successor : successors) {
                        if (state[successor] == 0) {
                            stack.add(successor);
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
        // Treat loop follow as a successor of loop head. Being an exit, it's visited before loop nodes,
        // so it's placed after all of them, including exits that we want to keep inside loop's range,
        // even if follow is only reachable through these exits.
        var follow = loopFollows[node];
        if (follow >= 0 && !hasSuccessor(successors, follow)) {
            successors = Arrays.copyOf(successors, successors.length + 1);
            successors[successors.length - 1] = follow;
        }
        if (successors.length > 4) {
            // mergesort sorts in ascending order, while we need successors with highest priority to go first
            return IndirectSort.mergesort(successors, (a, b) -> compareSuccessors(node, b, a));
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
            r = Integer.compare(successorRank(node, a), successorRank(node, b));
        }
        return r;
    }

    // Successors with higher rank are visited later by DFS, so they are placed closer to the node.
    // Exits that should stay inside loop's range get the highest rank, so they are placed right after
    // the jumping node. Other exits get lower rank, so they are placed after the loop. Exception handlers
    // get the lowest rank, so that normal control flow can fall through after the end of try block.
    private int successorRank(int node, int successor) {
        if (handlers[successor]) {
            return -1;
        }
        if (insideExits[successor]) {
            return 2;
        }
        var loopDepth = loopDepth(node);
        return loopDepth > 0 && commonLoopDepth(node, successor) < loopDepth ? 0 : 1;
    }

    // For each loop L consider its candidate exits: nodes X outside of L, such that idom(X) is in L
    // and X is in the parent loop of L. Each such X is a root of region (dominator subtree) which can only
    // be entered from L, i.e. it's the code that could be written inside loop body in the source code.
    // Choose at most one candidate (follow) to be placed after the loop, others are placed inside
    // loop's range:
    //
    // 1. If L jumps to a node in parent loop that is not a candidate (i.e. a node that joins with code
    //    outside of L), there's no follow, since this node will be placed after the loop anyway.
    // 2. If there's single candidate, it's the follow.
    // 3. If there are candidates which join control flow from multiple exits, take the last one.
    // 4. Otherwise, take exit from loop head (`while` condition), then exit from back edge source
    //    (`do .. while` condition). If none found, there's no follow, which means that all exits are
    //    placed inside the loop.
    private void findLoopExits() {
        var count = program.basicBlockCount();
        loopFollows = new int[count];
        Arrays.fill(loopFollows, -1);
        insideExits = new boolean[count];

        // Exception handlers are placed according to try/catch structure, so don't consider them as loop exits
        handlers = new boolean[count];
        for (var node = 0; node < count; ++node) {
            for (var tryCatch : program.basicBlockAt(node).getTryCatchBlocks()) {
                handlers[tryCatch.getHandler().getIndex()] = true;
            }
        }

        var candidateLoops = new Loop[count];
        // For each candidate: its index in the list of candidates of the corresponding loop
        var candidateIndexes = new int[count];
        var candidates = new IntArrayList[count];
        for (var node = 0; node < count; ++node) {
            var idom = domTree.immediateDominatorOf(node);
            if (idom < 0 || handlers[node]) {
                continue;
            }
            Loop candidateLoop = null;
            for (var loop = loopGraph.loopAt(idom); loop != null && !contains(loop, node);
                    loop = loop.getParent()) {
                candidateLoop = loop;
            }
            if (candidateLoop != null) {
                candidateLoops[node] = candidateLoop;
                var loopCandidates = candidates[candidateLoop.getHead()];
                if (loopCandidates == null) {
                    loopCandidates = new IntArrayList();
                    candidates[candidateLoop.getHead()] = loopCandidates;
                }
                candidateIndexes[node] = loopCandidates.size();
                loopCandidates.add(node);
            }
        }

        var hasForeignExits = new boolean[count];
        for (var node = 0; node < count; ++node) {
            for (var successor : cfg.outgoingEdges(node)) {
                if (handlers[successor] || domTree.dominates(successor, node)) {
                    continue;
                }
                for (var loop = loopGraph.loopAt(node); loop != null && !contains(loop, successor);
                        loop = loop.getParent()) {
                    if (contains(loop.getParent(), successor) && candidateLoops[successor] != loop) {
                        hasForeignExits[loop.getHead()] = true;
                    }
                }
            }
        }

        for (var head = 0; head < count; ++head) {
            var loopCandidates = candidates[head];
            if (loopCandidates == null) {
                continue;
            }
            var follow = -1;
            if (!hasForeignExits[head]) {
                follow = loopCandidates.size() == 1
                        ? loopCandidates.get(0)
                        : chooseFollow(loopGraph.loopAt(head), loopCandidates, candidateLoops, candidateIndexes);
            }
            loopFollows[head] = follow;
            for (var i = 0; i < loopCandidates.size(); ++i) {
                var candidate = loopCandidates.get(i);
                if (candidate != follow) {
                    insideExits[candidate] = true;
                }
            }
        }
    }

    private int chooseFollow(Loop loop, IntArrayList candidates, Loop[] candidateLoops, int[] candidateIndexes) {
        // For each candidate count incoming edges, either from loop or from regions of other candidates.
        // Additionally, collect edges between regions of candidates (as pairs of candidate indexes).
        var incomingCount = new int[candidates.size()];
        var regionEdges = new IntArrayList();
        for (var i = 0; i < candidates.size(); ++i) {
            var candidate = candidates.get(i);
            for (var predecessor : cfg.incomingEdges(candidate)) {
                if (domTree.dominates(candidate, predecessor)) {
                    continue;
                }
                incomingCount[i]++;
                if (contains(loop, predecessor)) {
                    continue;
                }
                var regionRoot = predecessor;
                while (true) {
                    var idom = domTree.immediateDominatorOf(regionRoot);
                    if (idom < 0 || contains(loop, idom)) {
                        break;
                    }
                    regionRoot = idom;
                }
                if (candidateLoops[regionRoot] == loop) {
                    regionEdges.add(candidateIndexes[regionRoot], i);
                }
            }
        }

        // Choose join which does not jump to other joins
        var isJoin = new boolean[candidates.size()];
        for (var i = 0; i < candidates.size(); ++i) {
            isJoin[i] = incomingCount[i] > 1;
        }
        var notLastJoin = new boolean[candidates.size()];
        for (var i = 0; i < regionEdges.size(); i += 2) {
            var from = regionEdges.get(i);
            var to = regionEdges.get(i + 1);
            if (isJoin[from] && isJoin[to]) {
                notLastJoin[from] = true;
            }
        }
        var follow = -1;
        var followIncomingCount = 0;
        for (var i = 0; i < candidates.size(); ++i) {
            if (isJoin[i] && !notLastJoin[i] && incomingCount[i] > followIncomingCount) {
                follow = candidates.get(i);
                followIncomingCount = incomingCount[i];
            }
        }
        if (follow >= 0) {
            return follow;
        }

        // No joins, take exit from loop head
        var head = loop.getHead();
        for (var i = 0; i < candidates.size(); ++i) {
            var candidate = candidates.get(i);
            if (hasSuccessor(cfg.incomingEdges(candidate), head)) {
                return candidate;
            }
        }

        // Take exit from the source of back edge
        for (var i = 0; i < candidates.size(); ++i) {
            var candidate = candidates.get(i);
            for (var predecessor : cfg.incomingEdges(candidate)) {
                if (contains(loop, predecessor) && hasSuccessor(cfg.outgoingEdges(predecessor), head)) {
                    return candidate;
                }
            }
        }

        return -1;
    }

    private boolean contains(Loop loop, int node) {
        if (loop == null) {
            return true;
        }
        var nodeLoop = loopGraph.loopAt(node);
        return nodeLoop != null && nodeLoop.isChildOf(loop);
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

    // Loop occupies range of positions from its head to its last node. Note that this range may contain
    // nodes which do not belong to the loop (i.e. those which exit loop and never return to loop head),
    // for example, in `while (true) { A; if (c) { B; break; } C }` node B may be placed between A and C.
    // That's fine, since such nodes never jump back to loop nodes.
    private int[] findLoopEnds() {
        var result = new int[program.basicBlockCount()];
        Arrays.fill(result, -1);
        for (var i = 0; i < nodeOrder.length; ++i) {
            var node = nodeOrder[i];
            for (var loop = loopGraph.loopAt(node); loop != null; loop = loop.getParent()) {
                result[loop.getHead()] = i + 1;
            }
            if (hasSuccessor(cfg.outgoingEdges(node), node)) {
                result[node] = Math.max(result[node], i + 1);
            }
        }

        // Compute nesting of loop ranges. Enclosing range is extended if necessary to cover nested range,
        // so that loop ranges never intersect
        innermostLoop = new int[nodeOrder.length];
        parentLoop = new int[nodeOrder.length];
        Arrays.fill(parentLoop, -1);
        var stack = new IntArrayList();
        for (var i = 0; i < nodeOrder.length; ++i) {
            while (!stack.isEmpty() && result[nodeOrder[stack.get(stack.size() - 1)]] <= i) {
                stack.removeAt(stack.size() - 1);
            }
            var node = nodeOrder[i];
            if (result[node] >= 0) {
                var parent = stack.isEmpty() ? -1 : stack.get(stack.size() - 1);
                parentLoop[i] = parent;
                for (var p = parent; p >= 0 && result[nodeOrder[p]] < result[node]; p = parentLoop[p]) {
                    result[nodeOrder[p]] = result[node];
                }
                stack.add(i);
            }
            innermostLoop[i] = stack.isEmpty() ? -1 : stack.get(stack.size() - 1);
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
        // Additionally, take loops into account. That is, in case there's a jump from *any* node in range of
        // loop L to a node beyond this range, we replace node A with head(L).
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
                for (var target : targets) {
                    var targetIndex = nodeIndexes[target.getIndex()];
                    if (targetIndex > i + 1) {
                        var sourceIndex = i * 2 + 1;
                        for (var loop = innermostLoop[i]; loop >= 0 && targetIndex >= loopEnds[nodeOrder[loop]];
                                loop = parentLoop[loop]) {
                            sourceIndex = loop * 2;
                        }
                        sourceIndex = extendToTryCatchStart(sourceIndex, targetIndex);
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
        for (var i = 0; i < result.length; ++i) {
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

    // If range starts inside try/catch which does not cover the range's target, this try/catch will be split
    // into two nodes: one before the block and one inside the block. To avoid this, move range start
    // to the beginning of such try/catch, but don't leave the innermost loop range containing the start.
    private int extendToTryCatchStart(int position, int targetIndex) {
        var index = position / 2;
        var node = nodeOrder[index];
        var common = outerCommonTryCatchSize(node, nodeOrder[targetIndex]);
        if (program.basicBlockAt(node).getTryCatchBlocks().size() <= common) {
            return position;
        }

        // Even position of loop head is outside of loop, odd position is inside
        var loop = innermostLoop[index];
        if (loop == index && position % 2 == 0) {
            loop = parentLoop[index];
        }
        var start = index;
        var lowerBound = Math.max(loop, 0);
        while (start > lowerBound && outerCommonTryCatchSize(nodeOrder[start - 1], node) > common) {
            --start;
        }
        if (start == index) {
            return position;
        }
        return start == loop ? start * 2 + 1 : start * 2;
    }

    // Number of common try/catch blocks, counting from the outermost
    private int outerCommonTryCatchSize(int a, int b) {
        var tryCatches1 = program.basicBlockAt(a).getTryCatchBlocks();
        var tryCatches2 = program.basicBlockAt(b).getTryCatchBlocks();
        var min = Math.min(tryCatches1.size(), tryCatches2.size());
        for (var i = 0; i < min; ++i) {
            var tryCatch1 = tryCatches1.get(tryCatches1.size() - i - 1);
            var tryCatch2 = tryCatches2.get(tryCatches2.size() - i - 1);
            if (tryCatch1.getHandler() != tryCatch2.getHandler()
                    || !Objects.equals(tryCatch1.getExceptionType(), tryCatch2.getExceptionType())) {
                return i;
            }
        }
        return min;
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
            if (Objects.equals(tryCatchBlock.getExceptionType(), currentTryCatch.exceptionType)
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

    // Longest suffix of try/catch chain which is also an outer part of the block's try/catches
    private static TryCatchInfo commonTryCatch(TryCatchInfo tryCatch, BasicBlock block) {
        while (tryCatch != null && !coversBlock(tryCatch, block)) {
            tryCatch = tryCatch.next;
        }
        return tryCatch;
    }

    private static boolean coversBlock(TryCatchInfo tryCatch, BasicBlock block) {
        var tryCatchBlocks = block.getTryCatchBlocks();
        for (; tryCatch != null; tryCatch = tryCatch.next) {
            if (tryCatch.depth > tryCatchBlocks.size()) {
                return false;
            }
            var tryCatchBlock = tryCatchBlocks.get(tryCatchBlocks.size() - tryCatch.depth);
            if (tryCatchBlock.getHandler() != tryCatch.handler
                    || !Objects.equals(tryCatchBlock.getExceptionType(), tryCatch.exceptionType)) {
                return false;
            }
        }
        return true;
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
