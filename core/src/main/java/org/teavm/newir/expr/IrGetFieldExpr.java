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

import org.teavm.newir.decl.IrField;
import org.teavm.newir.type.IrType;

public final class IrGetFieldExpr extends IrSingeInputExpr {
    private IrField field;

    public IrGetFieldExpr(IrExpr argument, IrField field) {
        super(argument);
        this.field = field;
    }

    public IrField getField() {
        return field;
    }

    @Override
    public IrType getType() {
        return field.getType();
    }

    @Override
    public IrType getInputType(int index) {
        return index == 0 ? IrType.OBJECT : super.getInputType(index);
    }

    @Override
    public boolean needsOrdering() {
        return true;
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
