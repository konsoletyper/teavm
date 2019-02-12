/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.ast.analysis;

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntDeque;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.WhileStatement;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.TextLocation;

public final class LocationGraphBuilder {
    private LocationGraphBuilder() {
    }

    public static Map<TextLocation, TextLocation[]> build(Statement node) {
        Visitor visitor = new Visitor();
        node.acceptVisitor(visitor);
        Graph graph = visitor.builder.build();
        TextLocation[][] locations = propagate(visitor.locations.toArray(new TextLocation[0]), graph);

        Map<TextLocation, Set<TextLocation>> builder = new LinkedHashMap<>();
        for (int i = 0; i < graph.size(); ++i) {
            for (int j : graph.outgoingEdges(i)) {
                for (TextLocation from : locations[i]) {
                    for (TextLocation to : locations[j]) {
                        builder.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
                    }
                }
            }
        }

        Map<TextLocation, TextLocation[]> result = new LinkedHashMap<>();
        for (Map.Entry<TextLocation, Set<TextLocation>> entry : builder.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toArray(new TextLocation[0]));
        }
        return result;
    }

    private static TextLocation[][] propagate(TextLocation[] locations, Graph graph) {
        List<Set<TextLocation>> result = new ArrayList<>();
        boolean[] stop = new boolean[graph.size()];
        IntDeque queue = new IntArrayDeque();
        for (int i = 0; i < stop.length; ++i) {
            Set<TextLocation> set = new LinkedHashSet<>();
            result.add(set);
            if (locations[i] != null) {
                stop[i] = true;
                queue.addLast(i);
                set.add(locations[i]);
            }
        }

        while (!queue.isEmpty()) {
            int node = queue.removeFirst();
            for (int successor : graph.outgoingEdges(node)) {
                if (stop[successor]) {
                    continue;
                }
                if (result.get(successor).addAll(result.get(node))) {
                    queue.addLast(successor);
                }
            }
        }

        return result.stream().map(s -> s.toArray(new TextLocation[0])).toArray(TextLocation[][]::new);
    }

    static class Visitor extends RecursiveVisitor {
        static final int[] EMPTY = new int[0];
        int[] nodes = EMPTY;
        ObjectIntMap<IdentifiedStatement> breakNodes = new ObjectIntHashMap<>();
        ObjectIntMap<IdentifiedStatement> continueNodes = new ObjectIntHashMap<>();
        IdentifiedStatement defaultBreakTarget;
        IdentifiedStatement defaultContinueTarget;
        GraphBuilder builder = new GraphBuilder();
        List<TextLocation> locations = new ArrayList<>();

        @Override
        protected void afterVisit(Expr expr) {
            setLocation(expr.getLocation());
        }

        @Override
        public void visit(BlockStatement statement) {
            int exit = createNode(null);
            breakNodes.put(statement, exit);
            super.visit(statement);
            breakNodes.remove(statement);

            setNode(exit);
        }

        @Override
        public void visit(WhileStatement statement) {
            IdentifiedStatement oldDefaultBreakTarget = defaultBreakTarget;
            IdentifiedStatement oldDefaultContinueTarget = defaultContinueTarget;

            int head = createNode(null);
            int exit = createNode(null);

            setNode(head);

            breakNodes.put(statement, exit);
            continueNodes.put(statement, head);
            defaultBreakTarget = statement;
            defaultContinueTarget = statement;

            if (statement.getCondition() != null) {
                statement.getCondition().acceptVisitor(this);
            }
            for (int node : nodes) {
                builder.addEdge(node, exit);
            }
            visit(statement.getBody());
            for (int node : nodes) {
                builder.addEdge(node, head);
            }
            nodes = new int[] { exit };

            defaultBreakTarget = oldDefaultBreakTarget;
            defaultContinueTarget = oldDefaultContinueTarget;
            breakNodes.remove(statement);
            continueNodes.remove(statement);
        }

        @Override
        public void visit(SwitchStatement statement) {
            IdentifiedStatement oldDefaultBreakTarget = defaultBreakTarget;

            int exit = createNode(null);

            breakNodes.put(statement, exit);
            defaultBreakTarget = statement;

            statement.getValue().acceptVisitor(this);
            int[] headNodes = nodes;
            for (SwitchClause clause : statement.getClauses()) {
                nodes = headNodes;
                visit(clause.getBody());
                for (int node : nodes) {
                    builder.addEdge(node, exit);
                }
            }
            nodes = headNodes;
            visit(statement.getDefaultClause());
            for (int node : nodes) {
                builder.addEdge(node, exit);
            }

            nodes = new int[] { exit };

            defaultBreakTarget = oldDefaultBreakTarget;
            breakNodes.remove(statement);
        }

        @Override
        public void visit(ConditionalStatement statement) {
            statement.getCondition().acceptVisitor(this);
            IntArrayList exit = new IntArrayList();

            int[] head = nodes;
            visit(statement.getConsequent());
            exit.add(nodes);

            nodes = head;
            visit(statement.getAlternative());
            exit.add(nodes);

            nodes = distinct(exit);
        }

        private int[] distinct(IntArrayList list) {
            IntHashSet set = new IntHashSet();
            int j = 0;
            int[] result = new int[list.size()];
            for (int i = 0; i < list.size(); ++i) {
                int e = list.get(i);
                if (set.add(e)) {
                    result[j++] = e;
                }
            }
            if (j < result.length) {
                result = Arrays.copyOf(result, j);
            }
            return result;
        }

        @Override
        public void visit(BreakStatement statement) {
            IdentifiedStatement target = statement.getTarget();
            if (target == null) {
                target = defaultBreakTarget;
            }
            int targetNode = breakNodes.get(target);
            for (int node : nodes) {
                builder.addEdge(node, targetNode);
            }
            nodes = EMPTY;
        }

        @Override
        public void visit(ContinueStatement statement) {
            IdentifiedStatement target = statement.getTarget();
            if (target == null) {
                target = defaultContinueTarget;
            }
            int targetNode = continueNodes.get(target);
            for (int node : nodes) {
                builder.addEdge(node, targetNode);
            }
            nodes = EMPTY;
        }

        @Override
        public void visit(ThrowStatement statement) {
            super.visit(statement);
            setLocation(statement.getLocation());
            nodes = EMPTY;
        }

        @Override
        public void visit(ReturnStatement statement) {
            super.visit(statement);
            setLocation(statement.getLocation());
            nodes = EMPTY;
        }

        @Override
        public void visit(TryCatchStatement statement) {
            int catchNode = createNode(null);
            for (Statement s : statement.getProtectedBody()) {
                s.acceptVisitor(this);
                for (int node : nodes) {
                    builder.addEdge(node, catchNode);
                }
            }

            nodes = new int[] { catchNode };
            visit(statement.getHandler());
        }

        @Override
        public void visit(AssignmentStatement statement) {
            super.visit(statement);
            setLocation(statement.getLocation());
        }

        @Override
        public void visit(InitClassStatement statement) {
            super.visit(statement);
            setLocation(statement.getLocation());
        }

        @Override
        public void visit(MonitorEnterStatement statement) {
            super.visit(statement);
            setLocation(statement.getLocation());
        }

        @Override
        public void visit(MonitorExitStatement statement) {
            super.visit(statement);
            setLocation(statement.getLocation());
        }

        @Override
        public void visit(ConditionalExpr expr) {
            expr.getCondition().acceptVisitor(this);
            IntArrayList exit = new IntArrayList();

            int[] head = nodes;
            expr.getConsequent().acceptVisitor(this);
            exit.add(nodes);

            nodes = head;
            expr.getAlternative().acceptVisitor(this);
            exit.add(nodes);

            nodes = distinct(exit);
        }

        private void setNode(int node) {
            for (int prevNode : nodes) {
                builder.addEdge(prevNode, node);
            }
            nodes = new int[] { node };
        }

        private void setLocation(TextLocation location) {
            if (location == null) {
                return;
            }
            int node = createNode(location);
            for (int prevNode : nodes) {
                builder.addEdge(prevNode, node);
            }
            nodes = new int[] { node };
        }

        private int createNode(TextLocation location) {
            int index = locations.size();
            locations.add(location);
            return index;
        }
    }
}
