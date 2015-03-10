/*
 *  Copyright 2012 Alexey Andreev.
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

import org.teavm.javascript.ast.*;

/**
 *
 * @author Alexey Andreev
 */
class ReferenceCountingVisitor implements StatementVisitor {
    private IdentifiedStatement target;
    public int count;

    public ReferenceCountingVisitor(IdentifiedStatement target) {
        this.target = target;
    }

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
        for (Statement part : statement.getConsequent()) {
            part.acceptVisitor(this);
        }
        for (Statement part : statement.getAlternative()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(SwitchStatement statement) {
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
        for (Statement part : statement.getBody()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(BlockStatement statement) {
        for (Statement part : statement.getBody()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(BreakStatement statement) {
        if (statement.getTarget() == target) {
            ++count;
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
        if (statement.getTarget() == target) {
            ++count;
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
        for (Statement part : statement.getProtectedBody()) {
            part.acceptVisitor(this);
        }
        for (Statement part : statement.getHandler()) {
            part.acceptVisitor(this);
        }
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
