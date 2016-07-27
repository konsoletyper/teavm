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

import java.util.Map;

public class BinaryExpr extends Expr {
    private BinaryOperation operation;
    private OperationType type;
    private Expr firstOperand;
    private Expr secondOperand;

    public BinaryOperation getOperation() {
        return operation;
    }

    public void setOperation(BinaryOperation operation) {
        this.operation = operation;
    }

    public Expr getFirstOperand() {
        return firstOperand;
    }

    public void setFirstOperand(Expr firstOperand) {
        this.firstOperand = firstOperand;
    }

    public Expr getSecondOperand() {
        return secondOperand;
    }

    public void setSecondOperand(Expr secondOperand) {
        this.secondOperand = secondOperand;
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
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
        BinaryExpr copy = new BinaryExpr();
        cache.put(this, copy);
        copy.setFirstOperand(firstOperand != null ? firstOperand.clone(cache) : null);
        copy.setSecondOperand(secondOperand != null ? secondOperand.clone(cache) : null);
        copy.setOperation(operation);
        copy.setType(type);
        return copy;
    }
}
