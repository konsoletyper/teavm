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
package org.teavm.javascript;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.javascript.ast.*;

/**
 *
 * @author Alexey Andreev
 */
class RedundantLabelEliminator implements StatementVisitor {
    private IdentifiedStatement currentBlock;
    private Set<IdentifiedStatement> hasRefs = new HashSet<>();

    void visitSequence(List<Statement> statements) {
        for (Statement statement : statements) {
            statement.acceptVisitor(this);
        }
    }

    @Override
    public void visit(AssignmentStatement statement) {
    }

    @Override
    public void visit(SequentialStatement statement) {
        visitSequence(statement.getSequence());
    }

    @Override
    public void visit(ConditionalStatement statement) {
        visitSequence(statement.getConsequent());
        visitSequence(statement.getAlternative());
    }

    @Override
    public void visit(SwitchStatement statement) {
        IdentifiedStatement currentBlockBackup = currentBlock;
        currentBlock = statement;
        for (SwitchClause clause : statement.getClauses()) {
            visitSequence(clause.getBody());
        }
        visitSequence(statement.getDefaultClause());
        if (!hasRefs.contains(currentBlock)) {
            currentBlock.setId(null);
        }
        currentBlock = currentBlockBackup;
    }

    @Override
    public void visit(WhileStatement statement) {
        IdentifiedStatement currentBlockBackup = currentBlock;
        currentBlock = statement;
        visitSequence(statement.getBody());
        if (!hasRefs.contains(currentBlock)) {
            currentBlock.setId(null);
        }
        currentBlock = currentBlockBackup;
    }

    @Override
    public void visit(BlockStatement statement) {
        IdentifiedStatement currentBlockBackup = currentBlock;
        currentBlock = null;
        visitSequence(statement.getBody());
        currentBlock = currentBlockBackup;
    }

    @Override
    public void visit(BreakStatement statement) {
        if (statement.getTarget() == currentBlock) {
            statement.setTarget(null);
        } else {
            hasRefs.add(statement.getTarget());
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
        if (statement.getTarget() == currentBlock) {
            statement.setTarget(null);
        } else {
            hasRefs.add(statement.getTarget());
        }
    }

    @Override
    public void visit(ReturnStatement statement) {
    }

    @Override
    public void visit(ThrowStatement statement) {
    }

    @Override
    public void visit(InitClassStatement statement) {
    }

    @Override
    public void visit(TryCatchStatement statement) {
        visitSequence(statement.getProtectedBody());
        visitSequence(statement.getHandler());
    }

    @Override
    public void visit(RestoreAsyncStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        
    }
}
