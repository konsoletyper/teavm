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
import org.eclipse.debug.core.model.IVariable;
import org.teavm.debugging.CallFrame;
import org.teavm.debugging.Debugger;

public class TeaVMJavaStackFrame extends TeaVMStackFrame {
    Debugger teavmDebugger;
    CallFrame callFrame;
    private TeaVMJavaVariablesHolder variablesHolder;

    public TeaVMJavaStackFrame(TeaVMThread thread, Debugger teavmDebugger, CallFrame callFrame) {
        super(thread);
        this.callFrame = callFrame;
        this.teavmDebugger = teavmDebugger;
        this.variablesHolder = new TeaVMJavaVariablesHolder("", thread.debugTarget, callFrame.getVariables().values());
    }

    public CallFrame getCallFrame() {
        return callFrame;
    }

    @Override
    public void stepInto() throws DebugException {
        teavmDebugger.stepInto();
    }

    @Override
    public void stepOver() throws DebugException {
        teavmDebugger.stepOver();
    }

    @Override
    public void stepReturn() throws DebugException {
        teavmDebugger.stepOut();
    }

    @Override
    public int getLineNumber() throws DebugException {
        return callFrame.getLocation() != null && callFrame.getLocation().getLine() >= 0 ?
                callFrame.getLocation().getLine() : callFrame.getOriginalLocation().getLine() + 1;
    }

    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder();
        String fileName = callFrame.getLocation() != null ? callFrame.getLocation().getFileName() : null;
        sb.append(fileName != null ? fileName : "unknown");
        if (callFrame.getLocation() != null) {
            sb.append(":").append(callFrame.getLocation().getLine());
        }
        return sb.toString();
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        return variablesHolder.getVariables();
    }
}
