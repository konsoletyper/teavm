/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.expr;

public class RecursiveIrExprVisitor implements IrExprVisitor {
    protected final void visitInputs(IrExpr expr) {
        for (int i = 0; i < expr.getInputCount(); ++i) {
            expr.getInput(i).acceptVisitor(this);
        }
    }

    protected void visitDefault(IrExpr expr) {
    }

    @Override
    public void visit(IrSequenceExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrBlockExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrExitBlockExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrLoopExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrLoopHeaderExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrExitLoopExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrContinueLoopExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrTupleExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrTupleComponentExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrConditionalExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrNullaryExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrUnaryExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrBinaryExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrArrayElementExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrSetArrayElementExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrThrowExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrTryCatchExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrCaughtExceptionExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrCaughtValueExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrBoundsCheckExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrCastExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrInstanceOfExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrCallExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrGetFieldExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrGetStaticFieldExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrSetFieldExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrSetStaticFieldExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrNewObjectExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrNewArrayExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrParameterExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrVariableExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrSetVariableExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrIntConstantExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrLongConstantExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrFloatConstantExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrDoubleConstantExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrStringConstantExpr expr) {
        visitDefault(expr);
    }
}
