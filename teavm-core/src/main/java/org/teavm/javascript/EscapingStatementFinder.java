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

import java.util.List;
import java.util.Set;
import org.teavm.javascript.ast.*;

/**
 *
 * @author Alexey Andreev
 */
class EscapingStatementFinder implements StatementVisitor{
    public boolean escaping;
    private Set<IdentifiedStatement> outerStatements;

    public EscapingStatementFinder(Set<IdentifiedStatement> nestingStatements) {
        this.outerStatements = nestingStatements;
    }

    public boolean check(List<Statement> statements) {
        if (!escaping) {
            if (statements.isEmpty()) {
                escaping = true;
            } else {
                statements.get(statements.size() - 1).acceptVisitor(this);
            }
        }
        return escaping;
    }

    @Override
    public void visit(AssignmentStatement statement) {
        escaping = true;
    }

    @Override
    public void visit(SequentialStatement statement) {
        check(statement.getSequence());
    }

    @Override
    public void visit(ConditionalStatement statement) {
        if (!check(statement.getConsequent())) {
            check(statement.getAlternative());
        }
    }

    @Override
    public void visit(SwitchStatement statement) {
        outerStatements.add(statement);
        for (SwitchClause clause : statement.getClauses()) {
            if (check(clause.getBody())) {
                break;
            }
        }
        check(statement.getDefaultClause());
        outerStatements.remove(statement);
    }

    @Override
    public void visit(WhileStatement statement) {
        outerStatements.add(statement);
        check(statement.getBody());
        outerStatements.remove(statement);
    }

    @Override
    public void visit(BlockStatement statement) {
        outerStatements.add(statement);
        check(statement.getBody());
        outerStatements.remove(statement);
    }

    @Override
    public void visit(BreakStatement statement) {
        escaping = !outerStatements.contains(statement.getTarget());
    }

    @Override
    public void visit(ContinueStatement statement) {
        escaping = !outerStatements.contains(statement.getTarget());
    }

    @Override
    public void visit(ReturnStatement statement) {
    }

    @Override
    public void visit(ThrowStatement statement) {
    }

    @Override
    public void visit(InitClassStatement statement) {
        escaping = true;
    }

    @Override
    public void visit(TryCatchStatement statement) {
        check(statement.getProtectedBody());
        check(statement.getHandler());
    }

    @Override
    public void visit(GotoPartStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        escaping = true;
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        escaping = true;
    }
}
