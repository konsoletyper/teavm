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
import org.eclipse.debug.core.model.*;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptDebugger;

public class TeaVMJSStackFrame extends TeaVMStackFrame {
    JavaScriptCallFrame callFrame;
    JavaScriptDebugger jsDebugger;
    private TeaVMJSVariablesHolder variablesHolder;

    public TeaVMJSStackFrame(TeaVMThread thread, JavaScriptDebugger jsDebugger, JavaScriptCallFrame callFrame) {
        super(thread);
        this.callFrame = callFrame;
        this.jsDebugger = jsDebugger;
        this.variablesHolder = new TeaVMJSVariablesHolder("", thread.debugTarget, callFrame.getVariables().values(),
                callFrame.getThisVariable(), callFrame.getClosureVariable());
    }

    public JavaScriptCallFrame getCallFrame() {
        return callFrame;
    }

    @Override
    public void stepInto() throws DebugException {
        jsDebugger.stepInto();
    }

    @Override
    public void stepOver() throws DebugException {
        jsDebugger.stepOver();
    }

    @Override
    public void stepReturn() throws DebugException {
        jsDebugger.stepOut();
    }

    @Override
    public int getLineNumber() throws DebugException {
        return callFrame.getLocation() != null ? callFrame.getLocation().getLine() + 1 : -1;
    }

    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder();
        String fileName = callFrame.getLocation() != null ? callFrame.getLocation().getScript(): null;
        sb.append(fileName != null ? fileName : "unknown");
        if (callFrame.getLocation() != null) {
            sb.append(" at ").append(callFrame.getLocation().getLine() + 1).append(";").append(
                    callFrame.getLocation().getColumn() + 1);
        }
        return sb.toString();
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        return variablesHolder.getVariables();
    }
}
