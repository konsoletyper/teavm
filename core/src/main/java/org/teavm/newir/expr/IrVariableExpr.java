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

public final class IrVariableExpr extends IrExpr {
    private IrVariable variable;

    public IrVariableExpr(IrVariable variable) {
        this.variable = variable;
    }

    public IrVariable getVariable() {
        return variable;
    }

    public void setVariable(IrVariable variable) {
        this.variable = variable;
    }

    @Override
    public IrType getType() {
        return variable.getType();
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
