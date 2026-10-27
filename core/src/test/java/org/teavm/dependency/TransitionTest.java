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
package org.teavm.dependency;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import org.junit.Test;
import org.teavm.common.ServiceRepository;
import org.teavm.diagnostics.AccumulationDiagnostics;
import org.teavm.model.ReferenceCache;
import org.teavm.model.ValueType;

public class TransitionTest {
    private static PreciseDependencyAnalyzer createAnalyzer() {
        return new PreciseDependencyAnalyzer(name -> null, null, TransitionTest.class.getClassLoader(),
                new ServiceRepository() {
                    @Override
                    public <T> T getService(Class<T> type) {
                        return null;
                    }
                }, new AccumulationDiagnostics(), new ReferenceCache(), new String[0], Properties::new);
    }

    @Test
    public void mergeDeliversTypesThatAreStillOnTheirWay() {
        mergeDeliversTypesThatAreStillOnTheirWay(false);
    }

    @Test
    public void batchMergeDeliversTypesThatAreStillOnTheirWay() {
        mergeDeliversTypesThatAreStillOnTheirWay(true);
    }

    private void mergeDeliversTypesThatAreStillOnTheirWay(boolean batch) {
        var analyzer = createAnalyzer();
        var a = analyzer.getType(ValueType.object("A"));
        var b = analyzer.getType(ValueType.object("B"));
        var c = analyzer.getType(ValueType.object("C"));
        var d = analyzer.getType(ValueType.object("D"));

        var source = analyzer.createNode();
        var first = analyzer.createNode();
        var second = analyzer.createNode();
        Set<ValueType> received = new LinkedHashSet<>();
        second.addConsumer(type -> received.add(type.getValueType()));
        second.propagate(c);
        source.propagate(c);

        // A transition with a filter never merges domains, so A reaches the first node by a plain delivery.
        source.connect(first, type -> true);
        source.connect(second);

        // When A reaches the first node, the source gets B (and D in the batch case). B reaches the second node
        // before A does, and that delivery merges the second node into the source's domain, which already holds A.
        first.addConsumer(type -> {
            if (type == a) {
                if (batch) {
                    source.propagate(new DependencyType[] { b, d });
                } else {
                    source.propagate(b);
                }
            }
        });
        source.propagate(a);
        analyzer.processDependencies();

        var expected = new HashSet<>(Set.of(a.getValueType(), b.getValueType(), c.getValueType()));
        if (batch) {
            expected.add(d.getValueType());
        }
        assertEquals(expected, received);
        assertEquals(expected, Set.of(second.getTypes()));
    }

    // Invariant: whatever order the analyzer delivers types in, and however it merges domains, every node's
    // consumers receive exactly the types the node ends up with, and those are the types of the least fixpoint
    // of the graph (initial types, transitions, and consumers that add types to other nodes).
    @Test
    public void consumersReceiveExactlyTheFixpointTypesOnRandomGraphs() {
        for (int seed = 0; seed < 50000; ++seed) {
            checkRandomGraph(seed);
        }
    }

    private void checkRandomGraph(int seed) {
        var random = new Random(seed);
        var analyzer = createAnalyzer();
        int nodeCount = 2 + random.nextInt(5);
        int typeCount = 1 + random.nextInt(6);
        var types = new DependencyType[typeCount];
        for (int i = 0; i < typeCount; ++i) {
            types[i] = analyzer.getType(ValueType.object("T" + i));
        }
        var nodes = new DependencyNode[nodeCount];
        List<Set<ValueType>> received = new ArrayList<>();
        for (int i = 0; i < nodeCount; ++i) {
            nodes[i] = analyzer.createNode();
            var set = new HashSet<ValueType>();
            received.add(set);
            nodes[i].addConsumer(type -> set.add(type.getValueType()));
        }

        // Edges: {from, to, filtered}. A filtered transition passes every type, but never merges domains.
        List<int[]> edges = new ArrayList<>();
        int edgeCount = random.nextInt(nodeCount * 3);
        for (int i = 0; i < edgeCount; ++i) {
            edges.add(new int[] { random.nextInt(nodeCount), random.nextInt(nodeCount),
                    random.nextBoolean() ? 1 : 0 });
        }
        // Triggers: {node, type, targetNode, targetTypes...}. When the node gets the type, the target node gets
        // the target types (as a batch, when there are several).
        List<int[]> triggers = new ArrayList<>();
        int triggerCount = random.nextInt(8);
        for (int i = 0; i < triggerCount; ++i) {
            int[] trigger = new int[3 + 1 + random.nextInt(2)];
            trigger[0] = random.nextInt(nodeCount);
            trigger[1] = random.nextInt(typeCount);
            trigger[2] = random.nextInt(nodeCount);
            for (int j = 3; j < trigger.length; ++j) {
                trigger[j] = random.nextInt(typeCount);
            }
            triggers.add(trigger);
        }
        // Initial types: {node, type}.
        List<int[]> initial = new ArrayList<>();
        int initialCount = 1 + random.nextInt(10);
        for (int i = 0; i < initialCount; ++i) {
            initial.add(new int[] { random.nextInt(nodeCount), random.nextInt(typeCount) });
        }

        for (var trigger : triggers) {
            var node = nodes[trigger[0]];
            var type = types[trigger[1]];
            var target = nodes[trigger[2]];
            var targetTypes = new LinkedHashSet<DependencyType>();
            for (int j = 3; j < trigger.length; ++j) {
                targetTypes.add(types[trigger[j]]);
            }
            node.addConsumer(t -> {
                if (t == type) {
                    if (targetTypes.size() == 1) {
                        target.propagate(targetTypes.iterator().next());
                    } else {
                        target.propagate(targetTypes.toArray(new DependencyType[0]));
                    }
                }
            });
        }
        // Interleave edges and initial types, so that some types are already there when a transition appears.
        int e = 0;
        int p = 0;
        while (e < edges.size() || p < initial.size()) {
            if (p == initial.size() || (e < edges.size() && random.nextBoolean())) {
                var edge = edges.get(e++);
                if (edge[2] != 0) {
                    nodes[edge[0]].connect(nodes[edge[1]], type -> true);
                } else {
                    nodes[edge[0]].connect(nodes[edge[1]]);
                }
            } else {
                var init = initial.get(p++);
                nodes[init[0]].propagate(types[init[1]]);
            }
        }
        analyzer.processDependencies();

        var expected = new ArrayList<Set<ValueType>>();
        for (int i = 0; i < nodeCount; ++i) {
            expected.add(new HashSet<>());
        }
        for (var init : initial) {
            expected.get(init[0]).add(types[init[1]].getValueType());
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (var edge : edges) {
                changed |= expected.get(edge[1]).addAll(expected.get(edge[0]));
            }
            for (var trigger : triggers) {
                if (expected.get(trigger[0]).contains(types[trigger[1]].getValueType())) {
                    for (int j = 3; j < trigger.length; ++j) {
                        changed |= expected.get(trigger[2]).add(types[trigger[j]].getValueType());
                    }
                }
            }
        }

        for (int i = 0; i < nodeCount; ++i) {
            assertEquals("seed " + seed + ", node " + i + " types", expected.get(i), Set.of(nodes[i].getTypes()));
            assertEquals("seed " + seed + ", node " + i + " consumer", expected.get(i), received.get(i));
        }
    }
}
