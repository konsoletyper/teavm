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

public final class IrUnaryExpr extends IrSingeInputExpr {
    private IrUnaryOperation operation;

    public IrUnaryExpr(IrExpr argument, IrUnaryOperation operation) {
        super(argument);
        this.operation = operation;
    }

    public IrUnaryOperation getOperation() {
        return operation;
    }

    public void setOperation(IrUnaryOperation operation) {
        this.operation = operation;
    }

    @Override
    public IrType getType() {
        switch (operation) {
            case INT_TO_BOOLEAN:
                return IrType.BOOLEAN;
            case INT_TO_BYTE:
                return IrType.BYTE;
            case INT_TO_SHORT:
                return IrType.SHORT;
            case INT_TO_CHAR:
                return IrType.CHAR;
            case BOOLEAN_TO_INT:
            case BYTE_TO_INT:
            case SHORT_TO_INT:
            case CHAR_TO_INT:
            case LONG_TO_INT:
            case FLOAT_TO_INT:
            case DOUBLE_TO_INT:
                return IrType.INT;
            case INT_TO_LONG:
            case FLOAT_TO_LONG:
            case DOUBLE_TO_LONG:
                return IrType.LONG;
            case INT_TO_FLOAT:
            case LONG_TO_FLOAT:
            case DOUBLE_TO_FLOAT:
                return IrType.FLOAT;
            case INT_TO_DOUBLE:
            case LONG_TO_DOUBLE:
            case FLOAT_TO_DOUBLE:
                return IrType.DOUBLE;

            case NOT:
                return IrType.BOOLEAN;

            case ARRAY_LENGTH:
                return IrType.INT;

            case INEG:
            case IINV:
                return IrType.INT;

            case LNEG:
            case LINV:
                return IrType.LONG;

            case FNEG:
                return IrType.FLOAT;

            case DNEG:
                return IrType.DOUBLE;

            case IGNORE:
                return IrType.VOID;

            case NULL_CHECK:
                return IrType.OBJECT;
        }

        throw new IllegalArgumentException();
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
