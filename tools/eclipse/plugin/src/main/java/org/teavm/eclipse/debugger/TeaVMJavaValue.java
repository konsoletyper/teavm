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
import org.teavm.debugging.Value;

public class TeaVMJavaValue extends TeaVMValue {
    private Value teavmValue;
    private boolean innerStructure;

    public TeaVMJavaValue(String id, TeaVMDebugTarget debugTarget, Value teavmValue) {
        super(id, debugTarget, new TeaVMJavaVariablesHolder(id, debugTarget, teavmValue.getProperties().values()));
        this.teavmValue = teavmValue;
        this.innerStructure = teavmValue.hasInnerStructure();
    }

    public Value getTeavmValue() {
        return teavmValue;
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
        return teavmValue.getType();
    }

    @Override
    public String getValueString() throws DebugException {
        if (teavmValue.getInstanceId() != null) {
            return teavmValue.getType() + " (id: " + getDebugTarget().getId(teavmValue.getInstanceId()) + ")";
        } else {
            return teavmValue.getRepresentation();
        }
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return innerStructure;
    }

    @Override
    public String getDescription() {
        return teavmValue.getRepresentation();
    }
}
