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
package org.teavm.ast.optimization;

import java.util.List;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.GotoPartStatement;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.SequentialStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.StatementVisitor;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.WhileStatement;

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
        if (statements == null) {
            return;
        }
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
    public void visit(BreakStatement statement) {
        if (statement.getTarget() == replacedBreak) {
            replaceBy = new ContinueStatement();
            replaceBy.setTarget(replacement);
            replaceBy.setLocation(statement.getLocation());
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
        if (statement.getTarget() == replacedBreak) {
            statement.setTarget(replacement);
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
    public void visit(GotoPartStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
    }

    @Override
    public void visit(MonitorExitStatement statement) {
    }
}
