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

public final class IrNullaryExpr extends IrExpr {
    private IrNullaryOperation operation;

    public IrNullaryExpr(IrNullaryOperation operation) {
        this.operation = operation;
    }

    public IrNullaryOperation getOperation() {
        return operation;
    }

    @Override
    public IrType getType() {
        switch (operation) {
            case NULL:
                return IrType.OBJECT;
            case VOID:
                return IrType.VOID;
            case UNREACHABLE:
            case THROW_NPE:
            case THROW_AIIOBE:
                return IrType.UNREACHABLE;
        }
        throw new IllegalStateException();
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
