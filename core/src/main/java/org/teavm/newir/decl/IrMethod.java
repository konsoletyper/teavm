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

import org.teavm.newir.type.IrType;

public final class IrMethod extends IrCallable<IrMethod> {
    private IrType[] parameterTypes;

    public IrMethod(IrType type, IrType... parameterTypes) {
        super(type);
        this.parameterTypes = parameterTypes != null && parameterTypes.length > 0 ? parameterTypes.clone() : null;
    }

    @Override
    public int getParameterCount() {
        return parameterTypes == null ? 1 : parameterTypes.length + 1;
    }

    @Override
    public IrType getParameterType(int index) {
        if (index == 0) {
            return IrType.OBJECT;
        } else {
            if (parameterTypes == null) {
                throw new IndexOutOfBoundsException();
            }
            return parameterTypes[index];
        }
    }
}
