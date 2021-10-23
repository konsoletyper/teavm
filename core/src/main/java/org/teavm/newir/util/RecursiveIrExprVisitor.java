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
package org.teavm.newir.util;

import org.teavm.newir.expr.IrArrayElementExpr;
import org.teavm.newir.expr.IrBlockExpr;
import org.teavm.newir.expr.IrCallExpr;
import org.teavm.newir.expr.IrCastExpr;
import org.teavm.newir.expr.IrCaughtExceptionExpr;
import org.teavm.newir.expr.IrCaughtValueExpr;
import org.teavm.newir.expr.IrConditionalExpr;
import org.teavm.newir.expr.IrContinueLoopExpr;
import org.teavm.newir.expr.IrDoubleConstantExpr;
import org.teavm.newir.expr.IrExitBlockExpr;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrExprVisitor;
import org.teavm.newir.expr.IrFloatConstantExpr;
import org.teavm.newir.expr.IrGetFieldExpr;
import org.teavm.newir.expr.IrGetGlobalExpr;
import org.teavm.newir.expr.IrGetVariableExpr;
import org.teavm.newir.expr.IrInstanceOfExpr;
import org.teavm.newir.expr.IrIntConstantExpr;
import org.teavm.newir.expr.IrLongConstantExpr;
import org.teavm.newir.expr.IrLoopExpr;
import org.teavm.newir.expr.IrLoopHeaderExpr;
import org.teavm.newir.expr.IrNewArrayExpr;
import org.teavm.newir.expr.IrNewObjectExpr;
import org.teavm.newir.expr.IrOperationExpr;
import org.teavm.newir.expr.IrParameterExpr;
import org.teavm.newir.expr.IrSetArrayElementExpr;
import org.teavm.newir.expr.IrSetCaughtValueExpr;
import org.teavm.newir.expr.IrSetFieldExpr;
import org.teavm.newir.expr.IrSetGlobalExpr;
import org.teavm.newir.expr.IrSetVariableExpr;
import org.teavm.newir.expr.IrStringConstantExpr;
import org.teavm.newir.expr.IrThrowExpr;
import org.teavm.newir.expr.IrTryCatchExpr;
import org.teavm.newir.expr.IrTryCatchStartExpr;
import org.teavm.newir.expr.IrTupleComponentExpr;
import org.teavm.newir.expr.IrTupleExpr;

public class RecursiveIrExprVisitor implements IrExprVisitor {
    protected final void visitDependencies(IrExpr expr) {
        for (int i = 0; i < expr.getDependencyCount(); ++i) {
            expr.getDependency(i).acceptVisitor(this);
        }
    }

    protected void visitDefault(IrExpr expr) {
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
    public void visit(IrOperationExpr expr) {
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
    public void visit(IrSetCaughtValueExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrTryCatchStartExpr expr) {
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
    public void visit(IrGetGlobalExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrSetFieldExpr expr) {
        visitDefault(expr);
    }

    @Override
    public void visit(IrSetGlobalExpr expr) {
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
    public void visit(IrGetVariableExpr expr) {
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
