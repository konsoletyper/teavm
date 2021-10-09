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

public final class IrParameter {
    private final IrProgram function;
    private final int index;
    private IrType type;

    IrParameter(IrProgram function, int index, IrType type) {
        this.function = function;
        this.index = index;
        this.type = type;
    }

    public IrProgram getFunction() {
        return function;
    }

    public int getIndex() {
        return index;
    }

    public IrType getType() {
        return type;
    }
}
