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

import org.teavm.newir.decl.IrReferenceType;
import org.teavm.newir.type.IrType;

public final class IrCastExpr extends IrSingeInputExpr {
    private IrReferenceType targetType;

    public IrCastExpr(IrExpr argument, IrReferenceType targetType) {
        super(argument);
        this.targetType = targetType;
    }

    public IrReferenceType getTargetType() {
        return targetType;
    }

    public void setTargetType(IrReferenceType targetType) {
        this.targetType = targetType;
    }

    @Override
    public IrType getType() {
        return IrType.OBJECT;
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
