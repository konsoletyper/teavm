/*
 *  Copyright 2017 Alexey Andreev.
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
import org.teavm.ast.AbstractStatementVisitor;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.SequentialStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.TryCatchStatement;

class RedundantReturnElimination extends AbstractStatementVisitor {
    @Override
    public void visit(SequentialStatement statement) {
        handleList(statement.getSequence());
    }

    @Override
    public void visit(ConditionalStatement statement) {
        handleList(statement.getConsequent());
        handleList(statement.getAlternative());
    }

    @Override
    public void visit(SwitchStatement statement) {
        handleList(statement.getDefaultClause());
        for (SwitchClause clause : statement.getClauses()) {
            handleList(clause.getBody());
        }
    }

    @Override
    public void visit(BlockStatement statement) {
        handleList(statement.getBody());
    }

    @Override
    public void visit(TryCatchStatement statement) {
        handleList(statement.getProtectedBody());
        handleList(statement.getHandler());
    }

    private void handleList(List<Statement> statements) {
        if (statements.isEmpty()) {
            return;
        }
        Statement last = statements.get(statements.size() - 1);
        if (last instanceof ReturnStatement && ((ReturnStatement) last).getResult() == null) {
            statements.remove(statements.size() - 1);
        } else {
            last.acceptVisitor(this);
        }
    }
}
