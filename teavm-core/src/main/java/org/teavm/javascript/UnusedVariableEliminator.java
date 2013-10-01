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

import java.util.Arrays;
import org.teavm.javascript.ast.*;

/**
 *
 * @author Alexey Andreev
 */
class UnusedVariableEliminator implements ExprVisitor, StatementVisitor {
    int[] indexes;
    int lastIndex;

    public UnusedVariableEliminator(int parameterCount, int variableCount) {
        indexes = new int[variableCount];
        Arrays.fill(indexes, -1);
        for (int i = 0; i <= parameterCount; ++i) {
            indexes[i] = lastIndex++;
        }
    }

    @Override
    public void visit(AssignmentStatement statement) {
        if (statement.getLeftValue() != null) {
            statement.getLeftValue().acceptVisitor(this);
        }
        statement.getRightValue().acceptVisitor(this);
    }

    @Override
    public void visit(SequentialStatement statement) {
        for (Statement part : statement.getSequence()) {
            part.acceptVisitor(this);
        }
    }

    @Override
    public void visit(ConditionalStatement statement) {
        statement.getCondition().acceptVisitor(this);
        statement.getConsequent().acceptVisitor(this);
        if (statement.getAlternative() != null) {
            statement.getAlternative().acceptVisitor(this);
        }
    }

    @Override
    public void visit(SwitchStatement statement) {
        statement.getValue().acceptVisitor(this);
        for (SwitchClause clause : statement.getClauses()) {
            clause.getStatement().acceptVisitor(this);
        }
        statement.getDefaultClause().acceptVisitor(this);
    }

    @Override
    public void visit(WhileStatement statement) {
        if (statement.getCondition() != null) {
            statement.getCondition().acceptVisitor(this);
        }
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
    public void visit(ForStatement statement) {
    }

    @Override
    public void visit(BreakStatement statement) {
    }

    @Override
    public void visit(ContinueStatement statement) {
    }

    @Override
    public void visit(ReturnStatement statement) {
        if (statement.getResult() != null) {
            statement.getResult().acceptVisitor(this);
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        statement.getException().acceptVisitor(this);
    }

    private int renumber(int var) {
        int index = indexes[var];
        if (index == -1) {
            index = lastIndex++;
            indexes[var] = index;
        }
        return index;
    }

    @Override
    public void visit(IncrementStatement statement) {
        statement.setVar(renumber(statement.getVar()));
    }

    @Override
    public void visit(BinaryExpr expr) {
        expr.getFirstOperand().acceptVisitor(this);
        expr.getSecondOperand().acceptVisitor(this);
    }

    @Override
    public void visit(UnaryExpr expr) {
        expr.getOperand().acceptVisitor(this);
    }

    @Override
    public void visit(ConditionalExpr expr) {
        expr.getCondition().acceptVisitor(this);
        expr.getConsequent().acceptVisitor(this);
        expr.getAlternative().acceptVisitor(this);
    }

    @Override
    public void visit(ConstantExpr expr) {
    }

    @Override
    public void visit(VariableExpr expr) {
        expr.setIndex(renumber(expr.getIndex()));
    }

    @Override
    public void visit(SubscriptExpr expr) {
        expr.getArray().acceptVisitor(this);
        expr.getIndex().acceptVisitor(this);
    }

    @Override
    public void visit(InvocationExpr expr) {
        for (Expr arg : expr.getArguments()) {
            arg.acceptVisitor(this);
        }
    }

    @Override
    public void visit(QualificationExpr expr) {
        expr.getQualified().acceptVisitor(this);
    }

    @Override
    public void visit(NewExpr expr) {
    }

    @Override
    public void visit(NewArrayExpr expr) {
        expr.getLength().acceptVisitor(this);
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        for (Expr dimension : expr.getDimensions()) {
            dimension.acceptVisitor(this);
        }
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        expr.getExpr().acceptVisitor(this);
    }

    @Override
    public void visit(StaticClassExpr expr) {
    }
}
