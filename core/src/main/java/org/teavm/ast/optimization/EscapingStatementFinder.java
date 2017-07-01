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

class EscapingStatementFinder implements StatementVisitor {
    AllBlocksCountVisitor blockCountVisitor;
    public boolean escaping;

    public EscapingStatementFinder(AllBlocksCountVisitor blockCountVisitor) {
        this.blockCountVisitor = blockCountVisitor;
    }

    private boolean isEmpty(Statement statement) {
        if (!(statement instanceof SequentialStatement)) {
            return false;
        }
        SequentialStatement seq = (SequentialStatement) statement;
        for (int i = seq.getSequence().size() - 1; i >= 0; --i) {
            if (!isEmpty(seq.getSequence().get(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean check(List<Statement> statements) {
        if (escaping) {
            return true;
        }
        if (statements.isEmpty()) {
            escaping = true;
            return true;
        }
        for (int i = statements.size() - 1; i >= 0; --i) {
            Statement stmt = statements.get(i);
            if (!isEmpty(stmt)) {
                stmt.acceptVisitor(this);
                return escaping;
            }
        }
        escaping = true;
        return true;
    }

    @Override
    public void visit(AssignmentStatement statement) {
        escaping |= true;
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
        if (blockCountVisitor.getCount(statement) > 0) {
            escaping = true;
            return;
        }
        for (SwitchClause clause : statement.getClauses()) {
            if (check(clause.getBody())) {
                break;
            }
        }
        if (!escaping) {
            check(statement.getDefaultClause());
        }
    }

    @Override
    public void visit(WhileStatement statement) {
        if (blockCountVisitor.getCount(statement) > 0) {
            escaping = true;
            return;
        }
        if (statement.getCondition() != null && check(statement.getBody())) {
            escaping = true;
        }
    }

    @Override
    public void visit(BlockStatement statement) {
        if (blockCountVisitor.getCount(statement) > 0) {
            escaping = true;
            return;
        }
        check(statement.getBody());
    }

    @Override
    public void visit(BreakStatement statement) {
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
    public void visit(InitClassStatement statement) {
        escaping = true;
    }

    @Override
    public void visit(TryCatchStatement statement) {
        if (!check(statement.getProtectedBody())) {
            check(statement.getHandler());
        }
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
