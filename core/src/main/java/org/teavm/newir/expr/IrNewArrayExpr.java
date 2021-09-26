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

import org.teavm.model.ValueType;

public abstract class IrNewArrayExpr extends IrExpr {
    private ValueType arrayType;

    public static IrNewArrayExpr of(ValueType arrayType, IrExpr size) {
        return new Impl1(arrayType, size);
    }

    public static IrNewArrayExpr of(ValueType arrayType, IrExpr[] dimensions) {
        return new ImplN(arrayType, dimensions.clone());
    }

    IrNewArrayExpr(ValueType arrayType) {
        this.arrayType = arrayType;
    }

    public ValueType getArrayType() {
        return arrayType;
    }

    public void setArrayType(ValueType arrayType) {
        this.arrayType = arrayType;
    }

    @Override
    public final IrType getType() {
        return IrType.OBJECT;
    }

    @Override
    public final void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }

    static final class Impl1 extends IrNewArrayExpr {
        IrExpr a1;

        Impl1(ValueType arrayType, IrExpr a1) {
            super(arrayType);
            this.a1 = a1;
        }

        @Override
        public int getInputCount() {
            return 1;
        }

        @Override
        public IrExpr getInput(int index) {
            return index == 0 ? a1 : super.getInput(index);
        }

        @Override
        public void setInput(int index, IrExpr value) {
            if (index == 0) {
                a1 = value;
            } else {
                super.setInput(index, value);
            }
        }
    }

    static final class ImplN extends IrNewArrayExpr {
        IrExpr[] arguments;

        ImplN(ValueType arrayType, IrExpr[] arguments) {
            super(arrayType);
            this.arguments = arguments;
        }

        @Override
        public int getInputCount() {
            return arguments.length;
        }

        @Override
        public IrExpr getInput(int index) {
            return arguments[index];
        }

        @Override
        public void setInput(int index, IrExpr value) {
            arguments[index] = value;
        }
    }
}
