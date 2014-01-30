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

import java.util.List;
import org.teavm.javascript.ast.*;

/**
 *
 * @author Alexey Andreev
 */
class BreakToContinueReplacer implements StatementVisitor {
    private IdentifiedStatement replacedBreak;
    private IdentifiedStatement replacement;
    private ContinueStatement replaceBy;

    public BreakToContinueReplacer(IdentifiedStatement replacedBreak, IdentifiedStatement replacement) {
        this.replacedBreak = replacedBreak;
        this.replacement = replacement;
    }

    @Override
    public void visit(AssignmentStatement statement) {
    }

    public void visitSequence(List<Statement> statements) {
        for (int i = 0; i < statements.size(); ++i) {
            Statement stmt = statements.get(i);
            stmt.acceptVisitor(this);
            if (replaceBy != null) {
                statements.set(i, replaceBy);
                replaceBy = null;
            }
        }
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
        for (SwitchClause clause : statement.getClauses()) {
            visitSequence(clause.getBody());
        }
        visitSequence(statement.getDefaultClause());
    }

    @Override
    public void visit(WhileStatement statement) {
        visitSequence(statement.getBody());
    }

    @Override
    public void visit(BlockStatement statement) {
        visitSequence(statement.getBody());
    }

    @Override
    public void visit(ForStatement statement) {
    }

    @Override
    public void visit(BreakStatement statement) {
        if (statement.getTarget() == replacedBreak) {
            replaceBy = new ContinueStatement();
            replaceBy.setTarget(replacement);
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
    }

    @Override
    public void visit(ReturnStatement statement) {
    }

    @Override
    public void visit(ThrowStatement statement) {
    }

    @Override
    public void visit(IncrementStatement statement) {
    }

    @Override
    public void visit(InitClassStatement statement) {
    }

}
