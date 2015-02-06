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
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
class TryCatchFinder implements StatementVisitor {
    public boolean tryCatchFound;

    @Override
    public void visit(AssignmentStatement statement) {
    }

    private void visitSequence(List<Statement> statements) {
        if (tryCatchFound) {
            return;
        }
        for (Statement statement : statements) {
            statement.acceptVisitor(this);
            if (tryCatchFound) {
                return;
            }
        }
    }

    @Override
    public void visit(SequentialStatement statement) {
        if (tryCatchFound) {
            return;
        }
        visitSequence(statement.getSequence());
    }

    @Override
    public void visit(ConditionalStatement statement) {
        if (tryCatchFound) {
            return;
        }
        visitSequence(statement.getConsequent());
        visitSequence(statement.getAlternative());
    }

    @Override
    public void visit(SwitchStatement statement) {
        if (tryCatchFound) {
            return;
        }
        for (SwitchClause clause : statement.getClauses()) {
            visitSequence(clause.getBody());
            if (tryCatchFound) {
                return;
            }
        }
        visitSequence(statement.getDefaultClause());
    }

    @Override
    public void visit(WhileStatement statement) {
        if (!tryCatchFound) {
            visitSequence(statement.getBody());
        }
    }

    @Override
    public void visit(BlockStatement statement) {
        if (!tryCatchFound) {
            visitSequence(statement.getBody());
        }
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
    }

    @Override
    public void visit(TryCatchStatement statement) {
        tryCatchFound = true;
    }

    @Override
    public void visit(RestoreAsyncStatement statement) {
    }
}
