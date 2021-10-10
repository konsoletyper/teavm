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

public class IrOperationExpr extends IrExpr {
    IrOperation operation;

    IrOperationExpr(IrOperation operation) {
        this.operation = operation;
    }

    public static IrOperationExpr of(IrOperation operation) {
        if (operation.getOperandCount() != 0) {
            throw new IllegalArgumentException("Operation " + operation.name() + " should take "
                    + operation.getOperandCount() + " inputs, no inputs given");
        }
        return new Impl0(operation);
    }

    public static IrOperationExpr of(IrOperation operation, IrExpr a) {
        if (operation.getOperandCount() != 1) {
            throw new IllegalArgumentException("Operation " + operation.name() + " should take "
                    + operation.getOperandCount() + " inputs, 1 input given");
        }
        return new Impl1(operation, a);
    }

    public static IrOperationExpr of(IrOperation operation, IrExpr a, IrExpr b) {
        if (operation.getOperandCount() != 2) {
            throw new IllegalArgumentException("Operation " + operation.name() + " should take "
                    + operation.getOperandCount() + " inputs, 2 inputs given");
        }
        return new Impl2(operation, a, b);
    }

    public final IrOperation getOperation() {
        return operation;
    }

    @Override
    public final IrType getType() {
        return operation.getType();
    }

    @Override
    public final int getInputCount() {
        return operation.getOperandCount();
    }

    @Override
    public final IrType getInputType(int index) {
        return operation.getOperandType(index);
    }

    @Override
    public boolean needsOrdering() {
        return operation.needsOrdering();
    }

    @Override
    public final void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }

    static class Impl0 extends IrOperationExpr {
        Impl0(IrOperation operation) {
            super(operation);
        }
    }

    static final class Impl1 extends IrOperationExpr {
        private IrExpr input;

        Impl1(IrOperation operation, IrExpr input) {
            super(operation);
            this.input = input;
        }

        @Override
        public IrExpr getInput(int index) {
            return index == 0 ? input : super.getInput(index);
        }

        @Override
        public void setInput(int index, IrExpr value) {
            if (index == 0) {
                input = value;
            } else {
                super.setInput(index, value);
            }
        }
    }

    static final class Impl2 extends IrOperationExpr {
        private IrExpr input1;
        private IrExpr input2;

        Impl2(IrOperation operation, IrExpr input1, IrExpr input2) {
            super(operation);
            this.input1 = input1;
            this.input2 = input2;
        }

        @Override
        public IrExpr getInput(int index) {
            switch (index) {
                case 0:
                    return input1;
                case 1:
                    return input2;
                default:
                    return super.getInput(index);
            }
        }

        @Override
        public void setInput(int index, IrExpr value) {
            switch (index) {
                case 0:
                    input1 = value;
                    break;
                case 1:
                    input2 = value;
                    break;
                default:
                    super.setInput(index, value);
            }
        }

        @Override
        public boolean needsOrdering() {
            return true;
        }
    }
}
