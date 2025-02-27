/*
 *  Copyright 2024 Alexey Andreev.
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
import java.util.HashMap;
import java.util.List;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;

final class WasmTypeGraphBuilder {
    private WasmTypeGraphBuilder() {
    }

    static Graph buildTypeGraph(WasmModule module, Iterable<WasmCompositeType> types, int size) {
        var graphBuilder = new GraphBuilder(size);
        var visitor = new GraphBuilderVisitor(module, graphBuilder);
        for (var type : types) {
            visitor.currentIndex = module.types.indexInCollection(type);
            type.acceptVisitor(visitor);
        }

        addNominalStructures(module, types, graphBuilder);

        return graphBuilder.build();
    }

    private static void addNominalStructures(WasmModule module, Iterable<WasmCompositeType> types,
            GraphBuilder graphBuilder) {
        var subStructures = new HashMap<WasmStructure, List<WasmStructure>>();
        var topLevelStructures = new ArrayList<WasmStructure>();
        for (var type : types) {
            if (type instanceof WasmStructure) {
                var structure = (WasmStructure) type;
                if (structure.isNominal()) {
                    if (structure.getSupertype() != null) {
                        subStructures.computeIfAbsent(structure.getSupertype(), k -> new ArrayList<>()).add(structure);
                    } else {
                        topLevelStructures.add(structure);
                    }
                }
            }
        }
        mergeNominalStructures(module, topLevelStructures, 0, graphBuilder);
        for (var entry : subStructures.entrySet()) {
            mergeNominalStructures(module, entry.getValue(), entry.getKey().getFields().size(), graphBuilder);
        }
    }

    private static void mergeNominalStructures(WasmModule module, List<WasmStructure> structures, int parentFieldCount,
            GraphBuilder graphBuilder) {
        outer: for (var i = 0; i < structures.size(); i++) {
            for (var j = i + 1; j < structures.size(); j++) {
                var a = structures.get(i);
                var b = structures.get(j);
                if (areSameStructures(parentFieldCount, a, b)) {
                    var p = module.types.indexInCollection(a);
                    var q = module.types.indexInCollection(b);
                    graphBuilder.addEdge(p, q);
                    graphBuilder.addEdge(q, p);
                    continue outer;
                }
            }
        }
    }

    private static boolean areSameStructures(int start, WasmStructure a, WasmStructure b) {
        if (a.getFields().size() != b.getFields().size()) {
            return false;
        }
        for (var i = start; i < a.getFields().size(); i++) {
            if (a.getFields().get(i).getType() != b.getFields().get(i).getType()) {
                return false;
            }
        }
        return true;
    }

    private static class GraphBuilderVisitor implements WasmCompositeTypeVisitor {
        final WasmModule module;
        final GraphBuilder graphBuilder;
        int currentIndex;

        GraphBuilderVisitor(WasmModule module, GraphBuilder graphBuilder) {
            this.module = module;
            this.graphBuilder = graphBuilder;
        }

        @Override
        public void visit(WasmStructure type) {
            if (type.getSupertype() != null) {
                addEdge(type.getSupertype().getReference());
            }
            for (var field : type.getFields()) {
                addEdge(field.getUnpackedType());
            }
        }

        @Override
        public void visit(WasmArray type) {
            addEdge(type.getElementType().asUnpackedType());
        }

        @Override
        public void visit(WasmFunctionType type) {
            for (var supertype : type.getSupertypes()) {
                addEdge(supertype.getReference());
            }
            for (var parameter : type.getParameterTypes()) {
                addEdge(parameter);
            }
            if (type.getReturnType() != null) {
                addEdge(type.getReturnType());
            }
        }

        private void addEdge(WasmType type) {
            if (type instanceof WasmType.CompositeReference) {
                var composite = ((WasmType.CompositeReference) type).composite;
                graphBuilder.addEdge(currentIndex, module.types.indexInCollection(composite));
            }
        }
    }
}
