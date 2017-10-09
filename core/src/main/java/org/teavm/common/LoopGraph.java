/*
 *  Copyright 2011 Alexey Andreev.
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
package org.teavm.common;

import java.util.*;

public class LoopGraph implements Graph {
    private static class LoopImpl implements Loop {
        public int head;
        public LoopImpl parent;
        public int walkIndex;

        @Override
        public int getHead() {
            return head;
        }

        @Override
        public Loop getParent() {
            return parent;
        }

        @Override
        public boolean isChildOf(Loop other) {
            if (other == null) {
                return true;
            }
            Loop loop = this;
            while (loop != null) {
                if (loop == other) {
                    return true;
                }
                loop = loop.getParent();
            }
            return false;
        }
    }

    private static class LoopFrame {
        public int walkIndex;
        public int index;
        public int sortIndex;
        public LoopImpl loop;
        public boolean done;
    }

    private Graph graph;
    private LoopImpl[] loops;
    private LoopImpl[] loopSet;
    private int[] walkIndexes;

    public LoopGraph(Graph graph) {
        this.graph = graph;
        findLoops();
    }

    private void findLoops() {
        int sz = graph.size();
        loops = new LoopImpl[sz];
        loopSet = new LoopImpl[sz];
        walkIndexes = new int[sz];
        LoopImpl[] createdLoops = new LoopImpl[sz];
        LoopFrame[] frames = new LoopFrame[sz];
        Deque<LoopFrame> stack = new ArrayDeque<>(sz * 4);
        LoopFrame rootFrame = new LoopFrame();
        stack.push(rootFrame);
        int walkIndex = 0;
        int lastSortIndex = sz - 1;
        int loopSetSize = 0;
        while (!stack.isEmpty()) {
            LoopFrame frame = stack.pop();
            if (frames[frame.index] == null) {
                frames[frame.index] = frame;
                frame.walkIndex = walkIndex++;
                stack.push(frame);
                int[] targetEdges = graph.outgoingEdges(frame.index);
                for (int i = 0; i < targetEdges.length; ++i) {
                    int next = targetEdges[i];
                    LoopFrame nextFrame = frames[next];
                    if (nextFrame == null) {
                        nextFrame = new LoopFrame();
                        nextFrame.index = next;
                        stack.push(nextFrame);
                    }
                }
            } else {
                frame.sortIndex = lastSortIndex--;
                frame.done = true;
                LoopImpl bestLoop = null;
                int[] targetEdges = graph.outgoingEdges(frame.index);
                for (int i = 0; i < targetEdges.length; ++i) {
                    int next = targetEdges[i];
                    LoopFrame nextFrame = frames[next];
                    LoopImpl loop = nextFrame.loop;
                    if (!nextFrame.done) {
                        loop = createdLoops[next];
                        if (loop == null) {
                            loop = new LoopImpl();
                            loop.head = next;
                            loop.walkIndex = nextFrame.walkIndex;
                            createdLoops[next] = loop;
                            loopSet[loopSetSize++] = loop;
                        }
                    } else {
                        while (loop != null && loop.head == next) {
                            loop = loop.parent;
                        }
                    }
                    if (loop == null) {
                        continue;
                    }
                    bestLoop = chooseLoop(bestLoop, loop);
                }
                frame.loop = bestLoop;
            }
        }
        loopSet = Arrays.copyOf(loopSet, loopSetSize);
        for (int i = 0; i < frames.length; ++i) {
            loops[i] = frames[i] != null ? frames[i].loop : null;
            walkIndexes[i] = frames[i] != null ? frames[i].sortIndex : -1;
        }
    }

    public static Loop commonSuperloop(Loop first, Loop second) {
        if (first == second) {
            return first;
        }
        List<Loop> firstPath = new ArrayList<>();
        List<Loop> secondPath = new ArrayList<>();
        while (first != null) {
            firstPath.add(first);
            first = first.getParent();
        }
        firstPath.add(null);
        while (second != null) {
            secondPath.add(second);
            second = second.getParent();
        }
        secondPath.add(null);
        Collections.reverse(firstPath);
        Collections.reverse(secondPath);
        int sz = Math.min(firstPath.size(), secondPath.size());
        for (int i = 1; i < sz; ++i) {
            if (firstPath.get(i) != secondPath.get(i)) {
                return firstPath.get(i - 1);
            }
        }
        return firstPath.get(sz - 1);
    }

    private LoopImpl chooseLoop(LoopImpl bestLoop, LoopImpl testLoop) {
        if (bestLoop == null || bestLoop == testLoop) {
            return testLoop;
        }
        if (bestLoop.walkIndex >= testLoop.walkIndex) {
            LoopImpl loop = bestLoop;
            while (loop.getParent() != null) {
                if (loop == testLoop) {
                    return bestLoop;
                }
                if (loop.parent.walkIndex < testLoop.walkIndex) {
                    testLoop.parent = loop.parent;
                    loop.parent = testLoop;
                    break;
                }
                loop = loop.parent;
            }
            if (loop.parent == null && loop != testLoop) {
                loop.parent = testLoop;
            }
            return bestLoop;
        } else {
            testLoop.parent = bestLoop;
            return testLoop;
        }
    }

    public Loop loopAt(int index) {
        return loops[index];
    }

    public Loop[] knownLoops() {
        return Arrays.copyOf(loopSet, loopSet.length);
    }

    @Override
    public int size() {
        return graph.size();
    }

    @Override
    public int[] incomingEdges(int node) {
        return graph.incomingEdges(node);
    }

    @Override
    public int copyIncomingEdges(int node, int[] target) {
        return graph.copyIncomingEdges(node, target);
    }

    @Override
    public int[] outgoingEdges(int node) {
        return graph.outgoingEdges(node);
    }

    @Override
    public int copyOutgoingEdges(int node, int[] target) {
        return graph.copyOutgoingEdges(node, target);
    }

    @Override
    public int incomingEdgesCount(int node) {
        return graph.incomingEdgesCount(node);
    }

    @Override
    public int outgoingEdgesCount(int node) {
        return graph.outgoingEdgesCount(node);
    }

    public int indexAt(int node) {
        return walkIndexes[node];
    }
}
