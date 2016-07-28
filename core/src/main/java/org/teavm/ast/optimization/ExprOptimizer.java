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

import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BinaryOperation;
import org.teavm.ast.Expr;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.UnaryOperation;

final class ExprOptimizer {
    private ExprOptimizer() {
    }

    public static Expr invert(Expr expr) {
        if (expr instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr) expr;
            if (unary.getOperation() == UnaryOperation.NOT) {
                return unary.getOperand();
            }
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr) expr;
            Expr a = binary.getFirstOperand();
            Expr b = binary.getSecondOperand();
            switch (binary.getOperation()) {
                case EQUALS:
                    return Expr.binary(BinaryOperation.NOT_EQUALS, binary.getType(), a, b, expr.getLocation());
                case NOT_EQUALS:
                    return Expr.binary(BinaryOperation.EQUALS, binary.getType(), a, b, expr.getLocation());
                case LESS:
                    return Expr.binary(BinaryOperation.GREATER_OR_EQUALS, binary.getType(), a, b, expr.getLocation());
                case LESS_OR_EQUALS:
                    return Expr.binary(BinaryOperation.GREATER, binary.getType(), a, b);
                case GREATER:
                    return Expr.binary(BinaryOperation.LESS_OR_EQUALS, binary.getType(), a, b, expr.getLocation());
                case GREATER_OR_EQUALS:
                    return Expr.binary(BinaryOperation.LESS, binary.getType(), a, b, expr.getLocation());
                default:
                    break;
            }
        }
        return Expr.invert(expr);
    }
}
