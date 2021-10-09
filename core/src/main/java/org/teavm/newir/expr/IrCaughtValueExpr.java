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

import org.teavm.newir.type.IrType;

public final class IrCaughtValueExpr extends IrExpr {
    IrTryCatchExpr tryCatchExpr;
    int index;
    IrType type;
    private IrExpr[] inputs;

    IrCaughtValueExpr(IrTryCatchExpr tryCatchExpr, int index, IrType type, IrExpr[] inputs) {
        this.tryCatchExpr = tryCatchExpr;
        this.index = index;
        this.type = type;
        this.inputs = inputs;
    }

    public IrTryCatchExpr getTryCatch() {
        return tryCatchExpr;
    }

    @Override
    public int getInputCount() {
        return inputs.length;
    }

    @Override
    public IrExpr getInput(int index) {
        return inputs[index];
    }

    @Override
    public void setInput(int index, IrExpr value) {
        inputs[index] = value;
    }

    @Override
    public IrType getType() {
        return inputs[0].getType();
    }

    @Override
    public IrType getInputType(int index) {
        if (index < 0 || index > inputs.length) {
            return super.getInputType(index);
        }
        return type;
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
