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
final class ExprOptimizer {
    private ExprOptimizer() {
    }

    public static Expr invert(Expr expr) {
        if (expr instanceof UnaryExpr) {
            UnaryExpr unary = (UnaryExpr)expr;
            if (unary.getOperation() == UnaryOperation.NOT) {
                return unary.getOperand();
            }
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binary = (BinaryExpr)expr;
            Expr a = binary.getFirstOperand();
            Expr b = binary.getSecondOperand();
            switch (binary.getOperation()) {
                case EQUALS:
                    return Expr.binary(BinaryOperation.NOT_EQUALS, a, b);
                case NOT_EQUALS:
                    return Expr.binary(BinaryOperation.EQUALS, a, b);
                case LESS:
                    return Expr.binary(BinaryOperation.GREATER_OR_EQUALS, a, b);
                case LESS_OR_EQUALS:
                    return Expr.binary(BinaryOperation.GREATER, a, b);
                case GREATER:
                    return Expr.binary(BinaryOperation.LESS_OR_EQUALS, a, b);
                case GREATER_OR_EQUALS:
                    return Expr.binary(BinaryOperation.LESS, a, b);
                case STRICT_EQUALS:
                    return Expr.binary(BinaryOperation.STRICT_NOT_EQUALS, a, b);
                case STRICT_NOT_EQUALS:
                    return Expr.binary(BinaryOperation.STRICT_EQUALS, a, b);
                default:
                    break;
            }
        }
        return Expr.invert(expr);
    }
}
