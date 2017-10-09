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
import org.teavm.debugging.javascript.JavaScriptVariable;

public class TeaVMJSVariable extends TeaVMVariable {
    private JavaScriptVariable var;

    public TeaVMJSVariable(String id, TeaVMDebugTarget debugTarget, JavaScriptVariable var) {
        super(id, debugTarget, new TeaVMJSValue(id, debugTarget, var.getValue()));
        this.var = var;
    }

    @Override
    public String getName() throws DebugException {
        return var.getName();
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
        return var.getValue().getClassName();
    }
}
