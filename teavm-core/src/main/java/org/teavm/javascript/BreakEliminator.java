/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.javascript;

import java.util.*;
import org.teavm.javascript.ast.*;

/**
 *
 * @author Alexey Andreev
 */
class BreakEliminator implements StatementVisitor {
    private Map<BlockStatement, List<Statement>> blockSuccessors = new HashMap<>();
    private Set<IdentifiedStatement> outerStatements = new HashSet<>();
    private List<Statement> currentSequence;
    private boolean sequenceEscapes;
    private int currentIndex;
    private AllBlocksCountVisitor usageCounter;

    public void eliminate(Statement statement) {
        usageCounter = new AllBlocksCountVisitor();
        statement.acceptVisitor(usageCounter);
        statement.acceptVisitor(this);
    }

    private void processSequence(List<Statement> statements) {
        List<Statement> oldSequence = currentSequence;
        int oldIndex = currentIndex;
        boolean oldEscapes = sequenceEscapes;

        sequenceEscapes = escapes(statements);
        currentSequence = statements;
        for (currentIndex = 0; currentIndex < currentSequence.size(); ++currentIndex) {
            statements.get(currentIndex).acceptVisitor(this);
        }

        sequenceEscapes = oldEscapes;
        currentIndex = oldIndex;
        currentSequence = oldSequence;
    }

    @Override
    public void visit(AssignmentStatement statement) {
    }

    @Override
    public void visit(SequentialStatement statement) {
        if (currentSequence == null) {
            processSequence(statement.getSequence());
            return;
        }
        --currentIndex;
        currentSequence.remove(currentIndex);
        currentSequence.addAll(currentIndex, statement.getSequence());
    }

    @Override
    public void visit(ConditionalStatement statement) {
        processSequence(statement.getConsequent());
        processSequence(statement.getAlternative());
    }

    @Override
    public void visit(SwitchStatement statement) {
        outerStatements.add(statement);
        for (SwitchClause clause : statement.getClauses()) {
            processSequence(clause.getBody());
        }
        processSequence(statement.getDefaultClause());
        outerStatements.remove(statement);
    }

    @Override
    public void visit(WhileStatement statement) {
        outerStatements.add(statement);
        processSequence(statement.getBody());
        outerStatements.remove(statement);
    }

    @Override
    public void visit(BlockStatement statement) {
        outerStatements.add(statement);
        if (!sequenceEscapes && !escapes(statement.getBody())) {
            blockSuccessors.put(statement, currentSequence.subList(currentIndex + 1, currentSequence.size()));
        }
        processSequence(statement.getBody());
        blockSuccessors.remove(statement);
        outerStatements.remove(statement);
    }

    @Override
    public void visit(BreakStatement statement) {
        if (blockSuccessors.containsKey(statement.getTarget())) {
            if (usageCounter.getCount(statement.getTarget()) == 1) {
                currentSequence.subList(currentIndex, currentSequence.size()).clear();
                List<Statement> successors = blockSuccessors.remove(statement.getTarget());
                currentSequence.addAll(successors);
                successors.clear();
                --currentIndex;
                sequenceEscapes = escapes(currentSequence);
                return;
            }
        }
        currentSequence.subList(currentIndex + 1, currentSequence.size()).clear();
    }

    @Override
    public void visit(ContinueStatement statement) {
        currentSequence.subList(currentIndex + 1, currentSequence.size()).clear();
    }

    @Override
    public void visit(ReturnStatement statement) {
        currentSequence.subList(currentIndex + 1, currentSequence.size()).clear();
    }

    @Override
    public void visit(ThrowStatement statement) {
        currentSequence.subList(currentIndex + 1, currentSequence.size()).clear();
    }

    @Override
    public void visit(InitClassStatement statement) {
    }

    @Override
    public void visit(TryCatchStatement statement) {
        Map<BlockStatement, List<Statement>> oldBlockSuccessors = blockSuccessors;
        Set<IdentifiedStatement> oldOuterStatements = outerStatements;
        outerStatements = new HashSet<>();
        blockSuccessors = new HashMap<>();
        processSequence(statement.getProtectedBody());
        outerStatements = oldOuterStatements;
        blockSuccessors = oldBlockSuccessors;
        processSequence(statement.getHandler());
    }

    @Override
    public void visit(GotoPartStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
    }

    @Override
    public void visit(MonitorExitStatement statement) {
    }

    private boolean escapes(List<Statement> statements) {
        return new EscapingStatementFinder(outerStatements).check(statements);
    }
}
