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
package org.teavm.ast;

public interface ExprVisitor {
    void visit(BinaryExpr expr);

    void visit(UnaryExpr expr);

    void visit(ConditionalExpr expr);

    void visit(ConstantExpr expr);

    void visit(VariableExpr expr);

    void visit(SubscriptExpr expr);

    void visit(UnwrapArrayExpr expr);

    void visit(InvocationExpr expr);

    void visit(QualificationExpr expr);

    void visit(NewExpr expr);

    void visit(NewArrayExpr expr);

    void visit(NewMultiArrayExpr expr);

    void visit(ArrayFromDataExpr expr);

    void visit(InstanceOfExpr expr);

    void visit(CastExpr expr);

    void visit(PrimitiveCastExpr expr);

    void visit(BoundCheckExpr expr);
}
