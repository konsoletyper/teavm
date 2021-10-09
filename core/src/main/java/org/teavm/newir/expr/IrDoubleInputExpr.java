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

abstract class IrDoubleInputExpr extends IrExpr {
    private IrExpr first;
    private IrExpr second;

    IrDoubleInputExpr(IrExpr first, IrExpr second) {
        this.first = first;
        this.second = second;
    }

    public IrExpr getFirst() {
        return first;
    }

    public void setFirst(IrExpr first) {
        this.first = first;
    }

    public IrExpr getSecond() {
        return second;
    }

    public void setSecond(IrExpr second) {
        this.second = second;
    }

    @Override
    public int getInputCount() {
        return 2;
    }

    @Override
    public IrExpr getInput(int index) {
        switch (index) {
            case 0:
                return first;
            case 1:
                return second;
            default:
                return super.getInput(index);
        }
    }

    @Override
    public void setInput(int index, IrExpr value) {
        switch (index) {
            case 0:
                first = value;
                break;
            case 1:
                second = value;
                break;
            default:
                super.setInput(index, value);
        }
    }
}
