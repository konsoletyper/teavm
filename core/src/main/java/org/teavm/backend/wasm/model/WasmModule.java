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
package org.teavm.backend.wasm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;

public class WasmModule {
    private int minMemorySize;
    private int maxMemorySize;
    private List<WasmMemorySegment> segments = new ArrayList<>();
    private List<WasmFunction> functionTable = new ArrayList<>();
    private WasmFunction startFunction;
    private Map<String, WasmCustomSection> customSections = new LinkedHashMap<>();
    private Map<String, WasmCustomSection> readonlyCustomSections = Collections.unmodifiableMap(customSections);

    public final WasmCollection<WasmFunction> functions = new WasmCollection<>();
    public final WasmCollection<WasmGlobal> globals = new WasmCollection<>();
    public final WasmCollection<WasmCompositeType> types = new WasmCollection<>();
    public final WasmCollection<WasmTag> tags = new WasmCollection<>();

    public void add(WasmCustomSection customSection) {
        if (customSections.containsKey(customSection.getName())) {
            throw new IllegalArgumentException("Custom section " + customSection.getName()
                    + " already defined in this module");
        }
        if (customSection.module != null) {
            throw new IllegalArgumentException("Given custom section is already registered in another module");
        }
        customSections.put(customSection.getName(), customSection);
        customSection.module = this;
    }

    public void remove(WasmCustomSection customSection) {
        if (customSection.module != this) {
            return;
        }
        customSection.module = null;
        customSections.remove(customSection.getName());
    }

    public Map<? extends String, ? extends WasmCustomSection> getCustomSections() {
        return readonlyCustomSections;
    }

    public List<WasmFunction> getFunctionTable() {
        return functionTable;
    }

    public List<WasmMemorySegment> getSegments() {
        return segments;
    }

    public int getMinMemorySize() {
        return minMemorySize;
    }

    public void setMinMemorySize(int minMemorySize) {
        this.minMemorySize = minMemorySize;
    }

    public int getMaxMemorySize() {
        return maxMemorySize;
    }

    public void setMaxMemorySize(int maxMemorySize) {
        this.maxMemorySize = maxMemorySize;
    }

    public WasmFunction getStartFunction() {
        return startFunction;
    }

    public void setStartFunction(WasmFunction startFunction) {
        this.startFunction = startFunction;
    }

    public void prepareForRendering() {
        prepareTypes();
    }

    private void prepareTypes() {
        var typeGraph = WasmTypeGraphBuilder.buildTypeGraph(this, types, types.size());
        var sccs = GraphUtils.findStronglyConnectedComponents(typeGraph);
        var sccStartNode = new int[types.size()];
        for (var i = 0; i < sccStartNode.length; ++i) {
            sccStartNode[i] = i;
        }
        var sccsByIndex = new int[types.size()][];
        for (var scc : sccs) {
            sccsByIndex[scc[0]] = scc;
            var firstType = types.get(scc[0]);
            firstType.recursiveTypeCount = scc.length;
            for (var i = 0; i < scc.length; i++) {
                var index = scc[i];
                var type = types.get(index);
                type.indexInRecursiveType = i;
                sccStartNode[scc[i]] = sccStartNode[scc[0]];
            }
        }

        var sorting = new TypeSorting();
        sorting.original = types;
        sorting.graph = typeGraph;
        sorting.visited = new boolean[types.size()];
        sorting.sccVisited = new boolean[types.size()];
        sorting.sccMap = sccStartNode;
        sorting.sccsByIndex = sccsByIndex;
        for (var i = 0; i < types.size(); ++i) {
            sorting.visit(i);
        }

        types.clear();
        for (var type : sorting.sorted) {
            types.add(type);
        }
    }

    private static class TypeSorting {
        WasmCollection<WasmCompositeType> original;
        Graph graph;
        boolean[] visited;
        boolean[] sccVisited;
        int[] sccMap;
        int[][] sccsByIndex;
        List<WasmCompositeType> sorted = new ArrayList<>();

        void visit(int typeIndex) {
            typeIndex = sccMap[typeIndex];
            if (visited[typeIndex]) {
                return;
            }
            var scc = sccsByIndex[typeIndex];
            if (scc == null) {
                visited[typeIndex] = true;
                for (var outgoing : graph.outgoingEdges(typeIndex)) {
                    visit(outgoing);
                }
                sorted.add(original.get(typeIndex));
            } else {
                visited[typeIndex] = true;
                for (var index : scc) {
                    for (var outgoing : graph.outgoingEdges(index)) {
                        visit(outgoing);
                    }
                }
                for (var index : scc) {
                    visitScc(index, typeIndex);
                }
            }
        }

        void visitScc(int index, int sccBase) {
            if (sccVisited[index]) {
                return;
            }
            sccVisited[index] = true;
            var type = original.get(index);
            if (type instanceof WasmStructure) {
                var supertype = ((WasmStructure) type).getSupertype();
                if (supertype != null && sccMap[supertype.index] == sccBase) {
                    visitScc(supertype.index, sccBase);
                }
            } else if (type instanceof WasmFunctionType) {
                var supertypes = ((WasmFunctionType) type).getSupertypes();
                for (var supertype : supertypes) {
                    if (sccMap[supertype.index] == sccBase) {
                        visitScc(supertype.index, sccBase);
                    }
                }
            }
            sorted.add(type);
        }
    }
}
