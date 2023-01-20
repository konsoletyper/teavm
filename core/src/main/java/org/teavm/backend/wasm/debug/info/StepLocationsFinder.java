/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.debug.info;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.common.CollectionUtil;

public class StepLocationsFinder {
    public final DebugInfo debugInfo;
    private LineInfoSequence lines;
    private FunctionControlFlow graph;
    private List<Point> points;
    private IntArrayDeque queue = new IntArrayDeque();
    private String currentFileName;
    private int currentLine;
    private boolean enterMethod;
    private IntSet breakpointAddresses = new IntHashSet();
    private IntSet callAddresses = new IntHashSet();
    private IntSet visited = new IntHashSet();

    public StepLocationsFinder(DebugInfo debugInfo) {
        this.debugInfo = debugInfo;
    }

    public boolean step(String fileName, int line, int address, boolean enterMethod) {
        address -= debugInfo.offset();
        updatePoints(address);
        if (points == null) {
            return false;
        }

        var index = CollectionUtil.binarySearch(points, address, p -> p.address);
        if (index < 0) {
            index = -index - 2;
        }
        if (index < 0) {
            return false;
        }

        this.enterMethod = enterMethod;
        currentFileName = fileName;
        currentLine = line;
        queue.addLast(index);
        callAddresses.clear();
        breakpointAddresses.clear();
        while (!queue.isEmpty()) {
            processTask();
        }
        visited.clear();
        currentFileName = null;

        return true;
    }

    public int[] getBreakpointAddresses() {
        return breakpointAddresses.toArray();
    }

    public int[] getCallAddresses() {
        return callAddresses.toArray();
    }

    private void updatePoints(int address) {
        var lines = debugInfo.lines().find(address);
        var graph = debugInfo.controlFlow().find(address);
        if (lines == null || graph == null) {
            this.lines = null;
            this.graph = null;
            points = null;
            return;
        }

        if (lines != this.lines || graph != this.graph) {
            this.lines = lines;
            this.graph = graph;
            points = null;
        }
        if (points == null) {
            points = createPoints();
        }
    }

    private List<Point> createPoints() {
        var list = new ArrayList<Point>();
        var indexInLines = 0;
        var graphIter = graph.iterator(0);
        var commandExecutor = new LineInfoCommandExecutor();

        while (indexInLines < lines.commands().size() && graphIter.hasNext()) {
            boolean nextInGraph;
            if (graphIter == null) {
                nextInGraph = false;
            } else if (indexInLines >= lines.commands().size()) {
                nextInGraph = true;
            } else {
                nextInGraph = lines.commands().get(indexInLines).address() >= graphIter.address();
            }
            if (nextInGraph) {
                var point = new Point(graphIter.address());
                list.add(point);
                point.isCall = graphIter.isCall();
                point.next = graphIter.targets();
                graphIter.next();
            } else {
                var cmd = lines.commands().get(indexInLines++);
                cmd.acceptVisitor(commandExecutor);
                var location = commandExecutor.createLocation();
                if (location != null && location.location() != null) {
                    Point point;
                    if (!list.isEmpty() && list.get(list.size() - 1).address == cmd.address()) {
                        point = list.get(list.size() - 1);
                    } else {
                        point = new Point(cmd.address());
                        list.add(point);
                    }
                    point.location = location.location();
                }
            }
        }

        for (var point : list) {
            var next = point.next;
            if (next == null) {
                continue;
            }
            int j = 0;
            for (int i = 0; i < next.length; ++i) {
                var foundIndex = CollectionUtil.binarySearch(list, next[i], p -> p.address);
                if (foundIndex < 0) {
                    foundIndex = -foundIndex - 1;
                }
                if (foundIndex >= list.size()) {
                    continue;
                }
                next[j++] = foundIndex;
            }
            if (j != next.length) {
                if (j == 0) {
                    point.next = null;
                } else {
                    point.next = Arrays.copyOf(next, j);
                }
            }
        }

        list.trimToSize();
        return list;
    }

    private void processTask() {
        var index = queue.removeFirst();
        if (!visited.add(index)) {
            return;
        }

        var point = points.get(index);
        if (point.location != null && !isCurrent(point.location)) {
            breakpointAddresses.add(point.address + debugInfo.offset());
            return;
        }
        if (enterMethod && point.isCall) {
            breakpointAddresses.add(point.address + debugInfo.offset());
            callAddresses.add(point.address + debugInfo.offset());
        }
        if (point.next != null) {
            for (var nextIndex : point.next) {
                queue.addLast(nextIndex);
            }
        } else if (index < points.size() - 1) {
            queue.addLast(index + 1);
        }
    }

    private boolean isCurrent(Location loc) {
        return loc.line() == currentLine && loc.file().fullName().equals(currentFileName);
    }

    private static class Point {
        int address;
        int[] next;
        boolean isCall;
        Location location;

        Point(int address) {
            this.address = address;
        }
    }
}
