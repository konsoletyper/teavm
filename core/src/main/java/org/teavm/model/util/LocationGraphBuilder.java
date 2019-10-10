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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.teavm.ast.ControlFlowEntry;
import org.teavm.common.Graph;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Program;
import org.teavm.model.TextLocation;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.JumpInstruction;

class LocationGraphBuilder {
    private Map<TextLocation, Set<TextLocation>> graphBuilder;
    private List<Set<TextLocation>> startLocations;
    private List<AdditionalConnection> additionalConnections;

    public ControlFlowEntry[] build(Program program) {
        graphBuilder = new HashMap<>();
        Graph graph = ProgramUtils.buildControlFlowGraph(program);
        dfs(graph, program);
        return assemble();
    }

    private void dfs(Graph graph, Program program) {
        startLocations = new ArrayList<>(Collections.nCopies(graph.size(), null));
        additionalConnections = new ArrayList<>();
        Deque<Step> stack = new ArrayDeque<>();
        for (int i = 0; i < graph.size(); ++i) {
            if (graph.incomingEdgesCount(i) == 0) {
                stack.push(new Step(null, new HashSet<>(), i));
            }
        }
        boolean[] visited = new boolean[graph.size()];
        TextLocation[] blockLocations = new TextLocation[graph.size()];

        while (!stack.isEmpty()) {
            Step step = stack.pop();
            if (visited[step.block]) {
                if (step.location != null && !step.location.isEmpty()) {
                    additionalConnections.add(new AdditionalConnection(step.location, startLocations.get(step.block)));
                }
                continue;
            }
            visited[step.block] = true;
            startLocations.set(step.block, step.startLocations);
            BasicBlock block = program.basicBlockAt(step.block);
            TextLocation location = step.location;
            boolean started = false;
            for (Instruction insn : block) {
                if (insn instanceof JumpInstruction || insn instanceof EmptyInstruction) {
                    continue;
                }
                if (insn.getLocation() != null) {
                    if (!started) {
                        step.startLocations.add(insn.getLocation());
                    }
                    started = true;
                    if (blockLocations[step.block] == null) {
                        blockLocations[step.block] = insn.getLocation();
                    }
                    if (location != null && !location.isEmpty() && !Objects.equals(location, insn.getLocation())) {
                        addEdge(location, insn.getLocation());
                    }
                    location = insn.getLocation();
                }
            }
            if (graph.outgoingEdgesCount(step.block) == 0) {
                if (location != null && !location.isEmpty()) {
                    addEdge(location, new TextLocation(null, -1));
                }
            } else {
                for (int next : graph.outgoingEdges(step.block)) {
                    stack.push(new Step(location, started ? new HashSet<>() : step.startLocations, next));
                }
            }
        }
    }

    private ControlFlowEntry[] assemble() {
        for (AdditionalConnection additionalConn : additionalConnections) {
            for (TextLocation succ : additionalConn.successors) {
                addEdge(additionalConn.location, succ);
            }
        }
        ControlFlowEntry[] locationGraph = new ControlFlowEntry[graphBuilder.size()];
        int index = 0;
        for (Map.Entry<TextLocation, Set<TextLocation>> entry : graphBuilder.entrySet()) {
            TextLocation[] successors = entry.getValue().toArray(new TextLocation[0]);
            for (int i = 0; i < successors.length; ++i) {
                if (successors[i] != null && successors[i].getLine() < 0) {
                    successors[i] = null;
                }
            }
            locationGraph[index++] = new ControlFlowEntry(entry.getKey(), successors);
        }
        return locationGraph;
    }

    private void addEdge(TextLocation source, TextLocation dest) {
        Set<TextLocation> successors = graphBuilder.get(source);
        if (successors == null) {
            successors = new HashSet<>();
            graphBuilder.put(source, successors);
        }
        successors.add(dest);
    }

    static class Step {
        TextLocation location;
        Set<TextLocation> startLocations;
        int block;
        Step(TextLocation location, Set<TextLocation> startLocations, int block) {
            this.location = location;
            this.startLocations = startLocations;
            this.block = block;
        }
    }

    static class AdditionalConnection {
        TextLocation location;
        Set<TextLocation> successors;
        AdditionalConnection(TextLocation location, Set<TextLocation> successors) {
            this.location = location;
            this.successors = successors;
        }
    }
}
