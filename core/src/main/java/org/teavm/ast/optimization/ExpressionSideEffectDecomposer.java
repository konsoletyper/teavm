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
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.NewExpr;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.Statement;

public class ExpressionSideEffectDecomposer extends RecursiveVisitor {
    private List<Statement> target;

    public ExpressionSideEffectDecomposer(List<Statement> target) {
        this.target = target;
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
    public void visit(InvocationExpr expr) {
        target.add(Statement.assign(null, expr));
    }

    @Override
    public void visit(NewExpr expr) {
        target.add(Statement.assign(null, expr));
    }
}
