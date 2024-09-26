/*
 *  Copyright 2024 konsoletyper.
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
package org.teavm.backend.wasm.generate.gc.methods;

import java.util.Arrays;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.WhileStatement;

class NonNullVarsCalculator extends RecursiveVisitor {
    private boolean[] nonNullVars;
    private boolean[] definedVars;

    public NonNullVarsCalculator(boolean[] nonNullVars) {
        this.nonNullVars = nonNullVars;
        definedVars = new boolean[nonNullVars.length];
    }

    @Override
    public void visit(VariableExpr expr) {
        use(expr.getIndex());
    }

    @Override
    public void visit(AssignmentStatement statement) {
        statement.getRightValue().acceptVisitor(this);
        if (statement.getLeftValue() instanceof VariableExpr) {
            var index = ((VariableExpr) statement.getLeftValue()).getIndex();
            define(index);
        } else if (statement.getLeftValue() != null) {
            statement.getLeftValue().acceptVisitor(this);
        }
    }

    @Override
    public void visit(BlockStatement statement) {
        runInBlock(() -> super.visit(statement));
    }

    @Override
    public void visit(WhileStatement statement) {
        runInBlock(() -> super.visit(statement));
    }

    @Override
    public void visit(SwitchStatement statement) {
        statement.getValue().acceptVisitor(this);
        for (var clause : statement.getClauses()) {
            runInBlock(() -> visit(clause.getBody()));
        }
        runInBlock(() -> visit(statement.getDefaultClause()));
    }

    @Override
    public void visit(TryCatchStatement statement) {
        runInBlock(() -> visit(statement.getProtectedBody()));
        runInBlock(() -> visit(statement.getHandler()));
    }

    @Override
    public void visit(ConditionalStatement statement) {
        statement.getCondition().acceptVisitor(this);
        runInBlock(() -> visit(statement.getConsequent()));
        runInBlock(() -> visit(statement.getAlternative()));
    }

    @Override
    public void visit(ConditionalExpr expr) {
        expr.getCondition().acceptVisitor(this);
        runInBlock(() -> expr.getConsequent().acceptVisitor(this));
        runInBlock(() -> expr.getAlternative().acceptVisitor(this));
    }

    @Override
    public void visit(BinaryExpr expr) {
        switch (expr.getOperation()) {
            case AND:
            case OR:
                expr.getFirstOperand().acceptVisitor(this);
                runInBlock(() -> expr.getSecondOperand().acceptVisitor(this));
                break;
            default:
                super.visit(expr);
                break;
        }
    }

    private void runInBlock(Runnable action) {
        var oldDefinedVars = definedVars;
        definedVars = Arrays.copyOf(definedVars, definedVars.length);
        action.run();
        definedVars = oldDefinedVars;
    }

    private void use(int index) {
        if (!definedVars[index]) {
            nonNullVars[index] = false;
        }
    }

    private void define(int index) {
        definedVars[index] = true;
    }
}
