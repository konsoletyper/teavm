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

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.teavm.debugging.Breakpoint;
import org.teavm.debugging.CallFrame;
import org.teavm.debugging.Debugger;
import org.teavm.debugging.DebuggerListener;

public class TeaVMJavaThread extends TeaVMThread {
    private Debugger teavmDebugger;

    public TeaVMJavaThread(TeaVMDebugTarget debugTarget) {
        super(debugTarget);
        this.teavmDebugger = debugTarget.teavmDebugger;
        this.teavmDebugger.addListener(new DebuggerListener() {
            @Override
            public void resumed() {
                updateStackTrace();
                fireEvent(new DebugEvent(TeaVMJavaThread.this, DebugEvent.RESUME));
            }

            @Override
            public void paused(Breakpoint breakpoint) {
                updateStackTrace();
                fireEvent(new DebugEvent(TeaVMJavaThread.this, DebugEvent.SUSPEND));
            }

            @Override
            public void detached() {
            }

            @Override
            public void breakpointStatusChanged(Breakpoint breakpoint) {
            }

            @Override
            public void attached() {
            }
        });
    }

    @Override
    protected void updateStackTrace() {
        if (teavmDebugger.getCallStack() == null) {
            this.stackTrace = null;
        } else {
            CallFrame[] teavmCallStack = teavmDebugger.getCallStack();
            TeaVMStackFrame[] stackTrace = new TeaVMStackFrame[teavmCallStack.length];
            for (int i = 0; i < teavmCallStack.length; ++i) {
                CallFrame teavmFrame = teavmCallStack[i];
                if (teavmFrame.getLocation() != null && teavmFrame.getLocation().getFileName() != null) {
                    stackTrace[i] = new TeaVMJavaStackFrame(this, teavmDebugger, teavmFrame);
                } else {
                    stackTrace[i] = new TeaVMJSStackFrame(this, teavmDebugger.getJavaScriptDebugger(),
                            teavmFrame.getOriginalCallFrame());
                }
            }
            this.stackTrace = stackTrace;
        }
        fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
    }


    @Override
    public boolean isSuspended() {
        return teavmDebugger.isSuspended();
    }

    @Override
    public void resume() throws DebugException {
        teavmDebugger.resume();
    }

    @Override
    public void suspend() throws DebugException {
        teavmDebugger.suspend();
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
    public String getName() {
        return "main";
    }
}
