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

public abstract class IrSingeInputExpr extends IrExpr {
    private IrExpr argument;

    IrSingeInputExpr(IrExpr argument) {
        this.argument = argument;
    }

    public IrExpr getArgument() {
        return argument;
    }

    public void setArgument(IrExpr argument) {
        this.argument = argument;
    }

    @Override
    public int getInputCount() {
        return 1;
    }

    @Override
    public IrExpr getInput(int index) {
        return index == 0 ? argument : super.getInput(index);
    }

    @Override
    public void setInput(int index, IrExpr value) {
        if (index == 0) {
            argument = value;
        } else {
            super.setInput(index, value);
        }
    }
}
