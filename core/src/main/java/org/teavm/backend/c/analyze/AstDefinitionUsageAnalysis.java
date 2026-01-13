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

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.IntStack;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ContinueStatement;
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
    private Graph cfg;
    private List<Node> nodes = new ArrayList<>();
    private int lastNode = -1;
    private IdentifiedStatement defaultBreakTarget;
    private IdentifiedStatement defaultContinueTarget;
    private ObjectIntMap<IdentifiedStatement> breakTargets = new ObjectIntHashMap<>();
    private ObjectIntMap<IdentifiedStatement> continueTargets = new ObjectIntHashMap<>();
    private List<Definition> definitions = new ArrayList<>();
    private List<? extends Definition> readonlyDefinitions = Collections.unmodifiableList(definitions);
    private ObjectIntMap<Definition> definitionIds = new ObjectIntHashMap<>();
    private Map<AssignmentStatement, Definition> definitionsByStatements = new HashMap<>();
    private List<Usage> usages = new ArrayList<>();
    private IntStack stack = new IntStack();
    private IntStack exceptionHandlerStack = new IntStack();

    public List<? extends Definition> getDefinitions() {
        return readonlyDefinitions;
    }

    public Definition getDefinition(AssignmentStatement statement) {
        return definitionsByStatements.get(statement);
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
        graphBuilder = null;
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
        return id;
    }

    private void propagate() {
        while (!stack.isEmpty()) {
            int nodeId = stack.pop();
            int usageId = stack.pop();
            var usage = usages.get(usageId);
            var node = nodes.get(nodeId);
            if (!node.usages.add(usageId)) {
                continue;
            }

            int variableId = usage.getExpr().getIndex();
            if (node.catchStatement != null) {
                if (node.catchStatement.getExceptionVariable() != null
                        && node.catchStatement.getExceptionVariable() == variableId) {
                    continue;
                }
                usage.liveInCatches.add(node.catchStatement);
            }

            int definitionId = node.definitions.getOrDefault(variableId, -1);
            if (definitionId >= 0) {
                var definition = definitions.get(definitionId);
                definition.usages.add(usages.get(usageId));
                continue;
            }

            if (nodeId < cfg.size()) {
                for (int predecessorId : cfg.incomingEdges(nodeId)) {
                    if (!nodes.get(predecessorId).usages.contains(usageId)) {
                        stack.push(usageId);
                        stack.push(predecessorId);
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
            var nodeAfterBody = createNode();
            int handlerNode = createNode();
            int nodeAfter = createNode();
            Node handlerNodeData = nodes.get(handlerNode);
            handlerNodeData.catchStatement = statement;
            var nodeBefore = lastNode;
            var entryNode = createNode();
            nodes.get(entryNode).leavingCatchStatement = statement;
            connect(nodeBefore, entryNode);
            exceptionHandlerStack.push(nodeAfterBody);
            lastNode = entryNode;
            visit(statement.getProtectedBody());
            exceptionHandlerStack.pop();
            connect(lastNode, nodeAfterBody);
            connect(entryNode, nodeAfterBody);

            lastNode = handlerNode;
            connect(nodeAfterBody, handlerNode);
            visit(statement.getHandler());
            connect(lastNode, nodeAfter);
            connect(handlerNode, nodeAfter);

            lastNode = nodeAfter;
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
            definitionsByStatements.put(statement, definition);
            definitionIds.put(definition, definition.id);
            var nodeId = createNode();
            var node = nodes.get(nodeId);
            connect(lastNode, nodeId);
            for (var cursor : exceptionHandlerStack) {
                graphBuilder.addEdge(nodeId, cursor.value);
            }
            lastNode = nodeId;
            node.definitions.put(definition.variableIndex, definition.id);
            return true;
        }

        @Override
        public void visit(VariableExpr expr) {
            if (lastNode < 0) {
                return;
            }

            Node node = nodes.get(lastNode);
            int definitionId = node.definitions.getOrDefault(expr.getIndex(), -1);
            int id = usages.size();
            var usage = new Usage(expr);
            usages.add(usage);
            if (definitionId >= 0) {
                Definition definition = definitions.get(definitionId);
                definition.usages.add(usage);
                node.usages.add(id);
            } else {
                stack.push(id);
                stack.push(lastNode);
            }
        }
    };

    private static class Node {
        IntIntMap definitions = new IntIntHashMap();
        IntSet usages = new IntHashSet();
        TryCatchStatement catchStatement;
        TryCatchStatement leavingCatchStatement;
    }

    public static class Definition {
        private AssignmentStatement statement;
        private int id;
        private int variableIndex;
        private final Set<Usage> usages = new LinkedHashSet<>();
        private Set<? extends Usage> readonlyUsages = Collections.unmodifiableSet(usages);

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

        public Collection<? extends Usage> getUsages() {
            return readonlyUsages;
        }
    }

    public static class Usage {
        private final VariableExpr expr;
        private List<TryCatchStatement> liveInCatches = new ArrayList<>();
        private List<? extends TryCatchStatement> readonlyLiveInCatches = Collections.unmodifiableList(liveInCatches);

        private Usage(VariableExpr expr) {
            this.expr = expr;
        }

        public VariableExpr getExpr() {
            return expr;
        }

        public List<? extends TryCatchStatement> getLiveInCatches() {
            return readonlyLiveInCatches;
        }
    }
}
