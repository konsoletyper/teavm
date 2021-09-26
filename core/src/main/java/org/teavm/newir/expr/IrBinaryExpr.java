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

public final class IrBinaryExpr extends IrDoubleInputExpr {
    private IrBinaryOperation operation;

    public IrBinaryExpr(IrExpr first, IrExpr second, IrBinaryOperation operation) {
        super(first, second);
        this.operation = operation;
    }

    public IrBinaryOperation getOperation() {
        return operation;
    }

    public void setOperation(IrBinaryOperation operation) {
        this.operation = operation;
    }

    @Override
    public IrType getType() {
        switch (operation) {
            case IEQ:
            case INE:
            case ILT:
            case ILE:
            case IGE:
            case IGT:
            case REF_EQ:
            case REF_NE:
            case LOGICAL_AND:
            case LOGICAL_OR:
                return IrType.BOOLEAN;

            case LCMP:
            case FCMP:
            case DCMP:
                return IrType.INT;

            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IDIV_SAFE:
            case IREM:
            case IREM_SAFE:
            case IAND:
            case IOR:
            case IXOR:
            case ISHL:
            case ISHR:
            case ISHRU:
                return IrType.INT;

            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LDIV_SAFE:
            case LREM:
            case LREM_SAFE:
            case LAND:
            case LOR:
            case LXOR:
            case LSHL:
            case LSHR:
            case LSHRU:
                return IrType.LONG;

            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
                return IrType.FLOAT;

            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                return IrType.FLOAT;
        }

        throw new IllegalStateException();
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
