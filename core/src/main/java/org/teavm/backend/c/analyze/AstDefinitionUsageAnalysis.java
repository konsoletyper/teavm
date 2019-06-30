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
package org.teavm.backend.c.analyze;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.IntStack;
import com.carrotsearch.hppc.LongHashSet;
import com.carrotsearch.hppc.LongSet;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.cursors.IntCursor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.WhileStatement;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;

public class AstDefinitionUsageAnalysis {
    private GraphBuilder graphBuilder = new GraphBuilder();
    private GraphBuilder exceptionGraphBuilder = new GraphBuilder();
    private Graph cfg;
    private Graph exceptionGraph;
    private List<Node> nodes = new ArrayList<>();
    private int lastNode = -1;
    private IdentifiedStatement defaultBreakTarget;
    private IdentifiedStatement defaultContinueTarget;
    private ObjectIntMap<IdentifiedStatement> breakTargets = new ObjectIntHashMap<>();
    private ObjectIntMap<IdentifiedStatement> continueTargets = new ObjectIntHashMap<>();
    private List<Definition> definitions = new ArrayList<>();
    private List<? extends Definition> readonlyDefinitions = Collections.unmodifiableList(definitions);
    private ObjectIntMap<Definition> definitionIds = new ObjectIntHashMap<>();
    private List<VariableExpr> usages = new ArrayList<>();
    private IntStack stack = new IntStack();
    private IntStack exceptionHandlerStack = new IntStack();

    public List<? extends Definition> getDefinitions() {
        return readonlyDefinitions;
    }

    public void analyze(Statement statement) {
        prepare(statement);
        propagate();
        cleanup();
    }

    private void prepare(Statement statement) {
        lastNode = createNode();
        statement.acceptVisitor(visitor);
        cfg = graphBuilder.build();
        exceptionGraph = exceptionGraphBuilder.build();
        graphBuilder = null;
        exceptionGraphBuilder = null;
        breakTargets = null;
        continueTargets = null;
        exceptionHandlerStack = null;
    }

    private void connect(int from, int to) {
        if (from >= 0 && to >= 0) {
            graphBuilder.addEdge(from, to);
        }
    }

    private int createNode() {
        int id = nodes.size();
        nodes.add(new Node());
        for (IntCursor cursor : exceptionHandlerStack) {
            exceptionGraphBuilder.addEdge(id, cursor.value);
        }
        return id;
    }

    private void propagate() {
        while (!stack.isEmpty()) {
            int exceptionHandlerId = stack.pop();
            int nodeId = stack.pop();
            int usageId = stack.pop();
            int variableId = usages.get(usageId).getIndex();

            Node node = nodes.get(nodeId);
            if (exceptionHandlerId < 0) {
                if (!node.usages.add(usageId)) {
                    continue;
                }
                int definitionId = node.definitions.getOrDefault(variableId, -1);
                if (definitionId >= 0) {
                    Definition definition = definitions.get(definitionId);
                    definition.usages.add(usages.get(usageId));
                    continue;
                }
                if (variableId == node.exceptionVariable) {
                    continue;
                }
            } else {
                if (!node.handlerUsages.add(pack(usageId, exceptionHandlerId))) {
                    continue;
                }
                IntArrayList definitionIds = node.handlerDefinitions.get(variableId);
                if (definitionIds != null) {
                    TryCatchStatement handlerStatement = nodes.get(exceptionHandlerId).catchStatement;
                    Expr usage = usages.get(usageId);
                    for (IntCursor cursor : definitionIds) {
                        Definition definition = definitions.get(cursor.value);
                        definition.usages.add(usages.get(usageId));
                        Set<Expr> handlerUsages = definition.exceptionHandlingUsages.get(handlerStatement);
                        if (handlerUsages == null) {
                            handlerUsages = new LinkedHashSet<>();
                            definition.exceptionHandlingUsages.put(handlerStatement, handlerUsages);
                        }
                        handlerUsages.add(usage);
                    }
                }
            }

            if (nodeId < cfg.size()) {
                for (int predecessorId : cfg.incomingEdges(nodeId)) {
                    if (!nodes.get(predecessorId).usages.contains(usageId)) {
                        stack.push(usageId);
                        stack.push(predecessorId);
                        stack.push(-1);
                    }
                }
            }

            if (nodeId < exceptionGraph.size()) {
                for (int predecessorId : exceptionGraph.incomingEdges(nodeId)) {
                    if (!nodes.get(predecessorId).handlerUsages.contains(usageId)) {
                        stack.push(usageId);
                        stack.push(predecessorId);
                        stack.push(nodeId);
                    }
                }
            }
        }
    }

    private void cleanup() {
        cfg = null;
        stack = null;
    }

    private RecursiveVisitor visitor = new RecursiveVisitor() {
        @Override
        public void visit(ConditionalStatement statement) {
            statement.getCondition().acceptVisitor(this);
            int forkNode = createNode();
            int joinNode = createNode();

            connect(lastNode, forkNode);
            lastNode = forkNode;
            visit(statement.getConsequent());
            connect(lastNode, joinNode);

            lastNode = forkNode;
            visit(statement.getAlternative());
            connect(lastNode, joinNode);

            lastNode = joinNode;
        }

        @Override
        public void visit(SwitchStatement statement) {
            IdentifiedStatement oldDefaultBreakTarget = defaultBreakTarget;
            defaultBreakTarget = statement;

            statement.getValue().acceptVisitor(this);
            int forkNode = createNode();
            int joinNode = createNode();

            connect(lastNode, forkNode);
            for (SwitchClause clause : statement.getClauses()) {
                lastNode = forkNode;
                visit(clause.getBody());
                connect(lastNode, joinNode);
            }

            lastNode = forkNode;
            visit(statement.getDefaultClause());
            connect(lastNode, joinNode);

            lastNode = joinNode;
            defaultBreakTarget = oldDefaultBreakTarget;
        }

        @Override
        public void visit(WhileStatement statement) {
            IdentifiedStatement oldDefaultBreakTarget = defaultBreakTarget;
            IdentifiedStatement oldDefaultContinueTarget = defaultContinueTarget;
            defaultBreakTarget = statement;
            defaultContinueTarget = statement;

            int continueNode = createNode();
            int forkNode = createNode();
            int breakNode = createNode();
            breakTargets.put(statement, breakNode);
            continueTargets.put(statement, continueNode);

            connect(lastNode, continueNode);
            lastNode = continueNode;
            if (statement.getCondition() != null) {
                statement.getCondition().acceptVisitor(this);
            }

            connect(lastNode, forkNode);
            connect(forkNode, breakNode);
            lastNode = forkNode;
            visit(statement.getBody());
            connect(lastNode, continueNode);
            lastNode = breakNode;

            breakTargets.remove(statement);
            continueTargets.remove(statement);
            defaultBreakTarget = oldDefaultBreakTarget;
            defaultContinueTarget = oldDefaultContinueTarget;
        }

        @Override
        public void visit(BlockStatement statement) {
            int breakNode = createNode();
            breakTargets.put(statement, breakNode);
            visit(statement.getBody());
            connect(lastNode, breakNode);
            lastNode = breakNode;
            breakTargets.remove(statement);
        }

        @Override
        public void visit(TryCatchStatement statement) {
            int handlerNode = createNode();
            Node handlerNodeData = nodes.get(handlerNode);
            handlerNodeData.catchStatement = statement;
            if (statement.getExceptionVariable() != null) {
                handlerNodeData.exceptionVariable = statement.getExceptionVariable();
            }
            exceptionHandlerStack.push(handlerNode);
            visit(statement.getProtectedBody());
            exceptionHandlerStack.pop();

            int node = lastNode;
            lastNode = handlerNode;
            visit(statement.getHandler());
            connect(lastNode, node);

            lastNode = node;
        }

        @Override
        public void visit(ReturnStatement statement) {
            super.visit(statement);
            lastNode = -1;
        }

        @Override
        public void visit(ThrowStatement statement) {
            super.visit(statement);
            lastNode = -1;
        }

        @Override
        public void visit(BreakStatement statement) {
            IdentifiedStatement target = statement.getTarget();
            if (target == null) {
                target = defaultBreakTarget;
            }
            connect(lastNode, breakTargets.getOrDefault(target, -1));
            lastNode = -1;
        }

        @Override
        public void visit(ContinueStatement statement) {
            IdentifiedStatement target = statement.getTarget();
            if (target == null) {
                target = defaultContinueTarget;
            }
            connect(lastNode, continueTargets.getOrDefault(target, -1));
            lastNode = -1;
        }

        @Override
        public void visit(AssignmentStatement statement) {
            if (!processAssignment(statement)) {
                super.visit(statement);
            }
        }

        private boolean processAssignment(AssignmentStatement statement) {
            if (!(statement.getLeftValue() instanceof VariableExpr)) {
                return false;
            }

            int leftIndex = ((VariableExpr) statement.getLeftValue()).getIndex();
            statement.getRightValue().acceptVisitor(this);
            Definition definition = new Definition(statement, definitions.size(), leftIndex);
            definitions.add(definition);
            definitionIds.put(definition, definition.id);
            if (lastNode >= 0) {
                Node node = nodes.get(lastNode);
                node.definitions.put(definition.variableIndex, definition.id);

                IntArrayList handlerDefinitions = node.handlerDefinitions.get(leftIndex);
                if (handlerDefinitions == null) {
                    handlerDefinitions = new IntArrayList();
                    node.handlerDefinitions.put(leftIndex, handlerDefinitions);
                }
                handlerDefinitions.add(definition.id);
            }
            return true;
        }

        @Override
        public void visit(VariableExpr expr) {
            if (lastNode < 0) {
                return;
            }

            Node node = nodes.get(lastNode);
            int definitionId = node.definitions.getOrDefault(expr.getIndex(), -1);
            if (definitionId >= 0) {
                Definition definition = definitions.get(definitionId);
                definition.usages.add(expr);
            } else if (node.exceptionVariable != expr.getIndex()) {
                int id = usages.size();
                usages.add(expr);
                node.usages.add(id);
                stack.push(id);
                stack.push(lastNode);
                stack.push(0);
            }
        }
    };

    static class Node {
        IntIntMap definitions = new IntIntHashMap();
        IntObjectMap<IntArrayList> handlerDefinitions = new IntObjectHashMap<>();
        IntSet usages = new IntHashSet();
        LongSet handlerUsages = new LongHashSet();
        TryCatchStatement catchStatement;
        int exceptionVariable = -1;
    }

    private static long pack(int a, int b) {
        return ((long) a << 32) | b;
    }

    public static class Definition {
        private AssignmentStatement statement;
        private int id;
        private int variableIndex;
        final Set<Expr> usages = new LinkedHashSet<>();
        private Set<? extends Expr> readonlyUsages = Collections.unmodifiableSet(usages);
        final Map<TryCatchStatement, Set<Expr>> exceptionHandlingUsages = new LinkedHashMap<>();
        private Map<? extends TryCatchStatement, Set<? extends Expr>> readonlyExceptionHandlingUsages =
                Collections.unmodifiableMap(exceptionHandlingUsages);

        Definition(AssignmentStatement statement, int id, int variableIndex) {
            this.statement = statement;
            this.id = id;
            this.variableIndex = variableIndex;
        }

        public AssignmentStatement getStatement() {
            return statement;
        }

        public int getId() {
            return id;
        }

        public int getVariableIndex() {
            return variableIndex;
        }

        public Set<? extends Expr> getUsages() {
            return readonlyUsages;
        }

        public Map<? extends TryCatchStatement, Set<? extends Expr>> getExceptionHandlingUsages() {
            return readonlyExceptionHandlingUsages;
        }
    }
}
