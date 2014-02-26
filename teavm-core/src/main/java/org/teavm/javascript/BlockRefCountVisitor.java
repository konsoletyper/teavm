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

import java.util.HashMap;
import java.util.Map;
import org.teavm.javascript.ast.*;

/**
 *
 * @author Alexey Andreev
 */
class BlockRefCountVisitor implements StatementVisitor {
    Map<IdentifiedStatement, Integer> refs = new HashMap<>();

    @Override
    public void visit(AssignmentStatement statement) {
    }

    @Override
    public void visit(SequentialStatement statement) {
        for (Statement part : statement.getSequence()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(ConditionalStatement statement) {
        for (Statement stmt : statement.getConsequent()) {
            stmt.acceptVisitor(this);
        }
        for (Statement stmt : statement.getAlternative()) {
            stmt.acceptVisitor(this);
        }
    }

    @Override
    public void visit(SwitchStatement statement) {
        refs.put(statement, 0);
        for (SwitchClause clause : statement.getClauses()) {
            for (Statement part : clause.getBody()) {
                part.acceptVisitor(this);
            }
        }
        for (Statement part : statement.getDefaultClause()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(WhileStatement statement) {
        refs.put(statement, 0);
        for (Statement part : statement.getBody()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(BlockStatement statement) {
        refs.put(statement, 0);
        for (Statement part : statement.getBody()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(ForStatement statement) {
    }

    @Override
    public void visit(BreakStatement statement) {
        refs.put(statement.getTarget(), refs.get(statement.getTarget()) + 1);
    }

    @Override
    public void visit(ContinueStatement statement) {
        refs.put(statement.getTarget(), refs.get(statement.getTarget()) + 1);
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

    @Override
    public void visit(TryCatchStatement statement) {
        for (Statement part : statement.getProtectedBody()) {
            part.acceptVisitor(this);
        }
        for (Statement part : statement.getHandler()) {
            part.acceptVisitor(this);
        }
    }
}
