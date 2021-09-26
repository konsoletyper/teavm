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

public class IrConditionalExpr extends IrExpr {
    private IrExpr condition;
    private IrExpr thenExpr;
    private IrExpr elseExpr;

    public IrConditionalExpr(IrExpr condition, IrExpr thenExpr, IrExpr elseExpr) {
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }

    public IrExpr getCondition() {
        return condition;
    }

    public void setCondition(IrExpr condition) {
        this.condition = condition;
    }

    public IrExpr getThenExpr() {
        return thenExpr;
    }

    public void setThenExpr(IrExpr thenExpr) {
        this.thenExpr = thenExpr;
    }

    public IrExpr getElseExpr() {
        return elseExpr;
    }

    public void setElseExpr(IrExpr elseExpr) {
        this.elseExpr = elseExpr;
    }

    @Override
    public IrExpr getInput(int index) {
        switch (index) {
            case 0:
                return condition;
            case 1:
                return thenExpr;
            case 2:
                return elseExpr;
            default:
                return super.getInput(index);
        }
    }

    @Override
    public void setInput(int index, IrExpr value) {
        switch (index) {
            case 0:
                condition = value;
                break;
            case 1:
                thenExpr = value;
                break;
            case 2:
                elseExpr = value;
                break;
            default:
                super.setInput(index, value);
        }
    }

    @Override
    public IrType getType() {
        return thenExpr.getType();
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
