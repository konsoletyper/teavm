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

public interface IrExprVisitor {
    void visit(IrSequenceExpr expr);

    void visit(IrBlockExpr expr);

    void visit(IrExitBlockExpr expr);

    void visit(IrLoopExpr expr);

    void visit(IrLoopHeaderExpr expr);

    void visit(IrExitLoopExpr expr);

    void visit(IrContinueLoopExpr expr);

    void visit(IrTupleExpr expr);

    void visit(IrTupleComponentExpr expr);

    void visit(IrConditionalExpr expr);

    void visit(IrNullaryExpr expr);

    void visit(IrUnaryExpr expr);

    void visit(IrBinaryExpr expr);

    void visit(IrArrayElementExpr expr);

    void visit(IrSetArrayElementExpr expr);

    void visit(IrThrowExpr expr);

    void visit(IrTryCatchExpr expr);

    void visit(IrCaughtExceptionExpr expr);

    void visit(IrCaughtValueExpr expr);

    void visit(IrBoundsCheckExpr expr);

    void visit(IrCastExpr expr);

    void visit(IrInstanceOfExpr expr);

    void visit(IrCallExpr expr);

    void visit(IrGetFieldExpr expr);

    void visit(IrGetStaticFieldExpr expr);

    void visit(IrSetFieldExpr expr);

    void visit(IrSetStaticFieldExpr expr);

    void visit(IrNewObjectExpr expr);

    void visit(IrNewArrayExpr expr);

    void visit(IrParameterExpr expr);

    void visit(IrVariableExpr expr);

    void visit(IrSetVariableExpr expr);

    void visit(IrIntConstantExpr expr);

    void visit(IrLongConstantExpr expr);

    void visit(IrFloatConstantExpr expr);

    void visit(IrDoubleConstantExpr expr);

    void visit(IrStringConstantExpr expr);
}
