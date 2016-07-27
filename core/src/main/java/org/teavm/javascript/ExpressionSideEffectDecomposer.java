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
package org.teavm.javascript;

import java.util.List;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.CastExpr;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.Expr;
import org.teavm.ast.ExprVisitor;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.NewExpr;
import org.teavm.ast.NewMultiArrayExpr;
import org.teavm.ast.PrimitiveCastExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.Statement;
import org.teavm.ast.SubscriptExpr;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;

public class ExpressionSideEffectDecomposer implements ExprVisitor {
    private List<Statement> target;

    public ExpressionSideEffectDecomposer(List<Statement> target) {
        this.target = target;
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
        ConditionalStatement statement = new ConditionalStatement();
        statement.setCondition(expr.getCondition());
        expr.getCondition().acceptVisitor(new ExpressionSideEffectDecomposer(statement.getConsequent()));
        expr.getAlternative().acceptVisitor(new ExpressionSideEffectDecomposer(statement.getAlternative()));
        target.add(statement);
    }

    @Override
    public void visit(ConstantExpr expr) {
    }

    @Override
    public void visit(VariableExpr expr) {
    }

    @Override
    public void visit(SubscriptExpr expr) {
        expr.getArray().acceptVisitor(this);
        expr.getIndex().acceptVisitor(this);
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        expr.getArray().acceptVisitor(this);
    }

    @Override
    public void visit(InvocationExpr expr) {
        target.add(Statement.assign(null, expr));
    }

    @Override
    public void visit(QualificationExpr expr) {
        if (expr.getQualified() != null) {
            expr.getQualified().acceptVisitor(this);
        }
    }

    @Override
    public void visit(NewExpr expr) {
        target.add(Statement.assign(null, expr));
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
    public void visit(CastExpr expr) {
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(PrimitiveCastExpr expr) {
        expr.getValue().acceptVisitor(this);
    }
}
