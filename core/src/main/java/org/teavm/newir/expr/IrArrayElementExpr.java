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

public class IrArrayElementExpr extends IrExpr {
    private IrType elementType;
    private IrExpr array;
    private IrExpr index;

    public IrArrayElementExpr(IrType elementType, IrExpr array, IrExpr index) {
        this.elementType = elementType;
        this.array = array;
        this.index = index;
    }

    public IrType getElementType() {
        return elementType;
    }

    public void setElementType(IrType elementType) {
        this.elementType = elementType;
    }

    public IrExpr getArray() {
        return array;
    }

    public void setArray(IrExpr array) {
        this.array = array;
    }

    public IrExpr getIndex() {
        return index;
    }

    public void setIndex(IrExpr index) {
        this.index = index;
    }

    @Override
    public int getInputCount() {
        return 2;
    }

    @Override
    public IrExpr getInput(int index) {
        switch (index) {
            case 0:
                return array;
            case 1:
                return this.index;
            default:
                return super.getInput(index);
        }
    }

    @Override
    public void setInput(int index, IrExpr value) {
        switch (index) {
            case 0:
                array = value;
                break;
            case 1:
                this.index = value;
                break;
            default:
                super.setInput(index, value);
        }
    }

    @Override
    public IrType getType() {
        return elementType;
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
