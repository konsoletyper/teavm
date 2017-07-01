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

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.teavm.eclipse.TeaVMEclipsePlugin;

public abstract class TeaVMVariable extends TeaVMDebugElement implements IVariable {
    private String id;
    private TeaVMValue value;

    public TeaVMVariable(String id, TeaVMDebugTarget debugTarget, TeaVMValue value) {
        super(debugTarget);
        this.id = id;
        this.value = value;
    }

    @Override
    public void setValue(IValue value) throws DebugException {
        throw new DebugException(new Status(Status.ERROR, TeaVMEclipsePlugin.ID, "Can't set value"));
    }

    @Override
    public void setValue(String value) throws DebugException {
        throw new DebugException(new Status(Status.ERROR, TeaVMEclipsePlugin.ID, "Can't set value"));
    }

    @Override
    public boolean supportsValueModification() {
        return false;
    }

    @Override
    public boolean verifyValue(IValue value) throws DebugException {
        return false;
    }

    @Override
    public boolean verifyValue(String value) throws DebugException {
        return false;
    }

    @Override
    public TeaVMValue getValue(){
        return value;
    }

    @Override
    public boolean hasValueChanged() throws DebugException {
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TeaVMVariable)) {
            return false;
        }
        TeaVMVariable other = (TeaVMVariable)obj;
        return id.equals(other.id);
    }
}
