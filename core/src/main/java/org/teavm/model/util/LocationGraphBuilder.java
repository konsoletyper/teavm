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

import java.util.*;
import org.teavm.common.Graph;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.InstructionLocation;
import org.teavm.model.Program;

/**
 *
 * @author Alexey Andreev
 */
class LocationGraphBuilder {
    private Map<InstructionLocation, Set<InstructionLocation>> graphBuilder;
    private List<Set<InstructionLocation>> startLocations;
    private List<AdditionalConnection> additionalConnections;

    public Map<InstructionLocation, InstructionLocation[]> build(Program program) {
        graphBuilder = new HashMap<>();
        Graph graph = ProgramUtils.buildControlFlowGraph(program);
        dfs(graph, program);
        return assemble();
    }

    private void dfs(Graph graph, Program program) {
        startLocations = new ArrayList<>(Collections.<Set<InstructionLocation>>nCopies(graph.size(), null));
        additionalConnections = new ArrayList<>();
        Deque<Step> stack = new ArrayDeque<>();
        for (int i = 0; i < graph.size(); ++i) {
            if (graph.incomingEdgesCount(i) == 0) {
                stack.push(new Step(null, new HashSet<>(), i));
            }
        }
        boolean[] visited = new boolean[graph.size()];
        InstructionLocation[] blockLocations = new InstructionLocation[graph.size()];

        while (!stack.isEmpty()) {
            Step step = stack.pop();
            if (visited[step.block]) {
                if (step.location != null) {
                    additionalConnections.add(new AdditionalConnection(step.location, startLocations.get(step.block)));
                }
                continue;
            }
            visited[step.block] = true;
            startLocations.set(step.block, step.startLocations);
            BasicBlock block = program.basicBlockAt(step.block);
            InstructionLocation location = step.location;
            boolean started = false;
            for (Instruction insn : block.getInstructions()) {
                if (insn.getLocation() != null) {
                    if (!started) {
                        step.startLocations.add(insn.getLocation());
                    }
                    started = true;
                    if (blockLocations[step.block] == null) {
                        blockLocations[step.block] = insn.getLocation();
                    }
                    if (location != null && !Objects.equals(location, insn.getLocation())) {
                        addEdge(location, insn.getLocation());
                    }
                    location = insn.getLocation();
                }
            }
            if (graph.outgoingEdgesCount(step.block) == 0) {
                if (location != null) {
                    addEdge(location, new InstructionLocation(null, -1));
                }
            } else {
                for (int next : graph.outgoingEdges(step.block)) {
                    stack.push(new Step(location, started ? new HashSet<>() : step.startLocations, next));
                }
            }
        }
    }

    private Map<InstructionLocation, InstructionLocation[]> assemble() {
        for (AdditionalConnection additionalConn : additionalConnections) {
            for (InstructionLocation succ : additionalConn.successors) {
                addEdge(additionalConn.location, succ);
            }
        }
        Map<InstructionLocation, InstructionLocation[]> locationGraph = new HashMap<>();
        for (Map.Entry<InstructionLocation, Set<InstructionLocation>> entry : graphBuilder.entrySet()) {
            InstructionLocation[] successors = entry.getValue().toArray(new InstructionLocation[0]);
            for (int i = 0; i < successors.length; ++i) {
                if (successors[i] != null && successors[i].getLine() < 0) {
                    successors[i] = null;
                }
            }
            locationGraph.put(entry.getKey(), successors);
        }
        return locationGraph;
    }

    private void addEdge(InstructionLocation source, InstructionLocation dest) {
        Set<InstructionLocation> successors = graphBuilder.get(source);
        if (successors == null) {
            successors = new HashSet<>();
            graphBuilder.put(source, successors);
        }
        successors.add(dest);
    }

    static class Step {
        InstructionLocation location;
        Set<InstructionLocation> startLocations;
        int block;
        public Step(InstructionLocation location, Set<InstructionLocation> startLocations, int block) {
            this.location = location;
            this.startLocations = startLocations;
            this.block = block;
        }
    }

    static class AdditionalConnection {
        InstructionLocation location;
        Set<InstructionLocation> successors;
        public AdditionalConnection(InstructionLocation location, Set<InstructionLocation> successors) {
            this.location = location;
            this.successors = successors;
        }
    }
}
