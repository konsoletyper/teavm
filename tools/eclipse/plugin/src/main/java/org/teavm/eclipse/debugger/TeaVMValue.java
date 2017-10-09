/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.eclipse.debugger;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

public abstract class TeaVMValue extends TeaVMDebugElement implements IValue {
    private String id;
    private TeaVMVariablesHolder variablesHolder;

    public TeaVMValue(String id, TeaVMDebugTarget debugTarget, TeaVMVariablesHolder variablesHolder) {
        super(debugTarget);
        this.id = id;
        this.variablesHolder = variablesHolder;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        return variablesHolder.getVariables();
    }

    @Override
    public boolean isAllocated() throws DebugException {
        return true;
    }

    public abstract String getDescription();

    @Override
    public int hashCode() {
        return 31 * id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TeaVMValue)) {
            return false;
        }
        TeaVMValue other = (TeaVMValue)obj;
        return id.equals(other.id);
    }
}
