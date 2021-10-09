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
package org.teavm.newir.decl;

import org.teavm.newir.expr.IrFunctionCallTarget;
import org.teavm.newir.expr.IrProgram;
import org.teavm.newir.type.IrType;

public final class IrFunction extends IrCallable<IrFunction> {
    private IrType[] parameterTypes;
    private String[] parameterNameHints;
    private IrFunctionCallTarget callTarget;

    public IrFunction(IrType type, IrType... parameterTypes) {
        super(type);
        this.parameterTypes = parameterTypes != null && parameterTypes.length > 0 ? parameterTypes.clone() : null;
    }

    @Override
    public int getParameterCount() {
        return parameterTypes == null ? 0 : parameterTypes.length;
    }

    @Override
    public IrType getParameterType(int index) {
        if (parameterTypes == null) {
            throw new IndexOutOfBoundsException();
        }
        return parameterTypes[index];
    }

    public String getParameterNameHint(int index) {
        if (parameterNameHints == null) {
            if (index < 0 || index >= parameterTypes.length) {
                throw new IndexOutOfBoundsException();
            }
            return null;
        } else {
            return parameterNameHints[index];
        }
    }

    public void setParameterNameHint(int index, String name) {
        if (name == null && parameterNameHints == null) {
            if (index < 0 || index >= parameterTypes.length) {
                throw new IndexOutOfBoundsException();
            }
        }
        parameterNameHints = new String[parameterTypes.length];
        parameterNameHints[index] = name;
    }

    public IrFunctionCallTarget getCallTarget() {
        if (callTarget == null) {
            callTarget = new IrFunctionCallTarget(this);
        }
        return callTarget;
    }

    public IrProgram createProgram() {
        return new IrProgram(parameterTypes);
    }
}
