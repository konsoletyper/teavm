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

public final class IrBlockExpr extends IrExpr {
    private IrExpr body = IrExpr.VOID;

    @Override
    public IrType getType() {
        return body.getType();
    }

    @Override
    public int getInputCount() {
        return 1;
    }

    public IrExpr getBody() {
        return body;
    }

    public void setBody(IrExpr body) {
        this.body = body;
    }

    @Override
    public IrExpr getInput(int index) {
        return index == 0 ? body : super.getInput(index);
    }

    @Override
    public void setInput(int index, IrExpr value) {
        if (index == 0) {
            body = value;
        } else {
            super.setInput(index, value);
        }
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
