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
import org.teavm.debugging.javascript.JavaScriptValue;

public class TeaVMJSValue extends TeaVMValue {
    private JavaScriptValue jsValue;
    private boolean innerStructure;

    public TeaVMJSValue(String id, TeaVMDebugTarget debugTarget, JavaScriptValue teavmValue) {
        super(id, debugTarget, new TeaVMJSVariablesHolder(id, debugTarget, teavmValue.getProperties().values(),
                null, null));
        this.jsValue = teavmValue;
        this.innerStructure = teavmValue.hasInnerStructure();
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
        return jsValue.getClassName();
    }

    @Override
    public String getValueString() throws DebugException {
        if (jsValue.getInstanceId() != null) {
            return jsValue.getClassName() + " (id: " + getDebugTarget().getId(jsValue.getInstanceId()) + ")";
        } else {
            return jsValue.getRepresentation();
        }
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return innerStructure;
    }

    public JavaScriptValue getJavaScriptValue() {
        return jsValue;
    }

    @Override
    public String getDescription() {
        return jsValue.getRepresentation();
    }
}
