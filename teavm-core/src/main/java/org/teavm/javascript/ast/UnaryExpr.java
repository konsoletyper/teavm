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
package org.teavm.javascript.ast;

import java.util.Map;

/**
 *
 * @author Alexey Andreev
 */
public class UnaryExpr extends Expr {
    private UnaryOperation operation;
    private Expr operand;

    public UnaryOperation getOperation() {
        return operation;
    }

    public void setOperation(UnaryOperation operation) {
        this.operation = operation;
    }

    public Expr getOperand() {
        return operand;
    }

    public void setOperand(Expr operand) {
        this.operand = operand;
    }

    @Override
    public void acceptVisitor(ExprVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Expr clone(Map<Expr, Expr> cache) {
        Expr known = cache.get(this);
        if (known != null) {
            return known;
        }
        UnaryExpr copy = new UnaryExpr();
        copy.setOperation(operation);
        copy.setOperand(operand != null ? operand.clone(cache) : null);
        return copy;
    }
}
