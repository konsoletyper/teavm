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
import org.teavm.debugging.javascript.JavaScriptBreakpoint;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptDebugger;
import org.teavm.debugging.javascript.JavaScriptDebuggerListener;

public class TeaVMJSThread extends TeaVMThread {
    private JavaScriptDebugger jsDebugger;

    public TeaVMJSThread(TeaVMDebugTarget debugTarget) {
        super(debugTarget);
        this.debugTarget = debugTarget;
        this.jsDebugger = debugTarget.jsDebugger;
        jsDebugger.addListener(new JavaScriptDebuggerListener() {
            @Override
            public void scriptAdded(String name) {
            }
            @Override
            public void resumed() {
                updateStackTrace();
                fireEvent(new DebugEvent(TeaVMJSThread.this, DebugEvent.RESUME));
            }
            @Override
            public void paused(JavaScriptBreakpoint breakpoint) {
                updateStackTrace();
                fireEvent(new DebugEvent(TeaVMJSThread.this, DebugEvent.SUSPEND));
            }
            @Override
            public void detached() {
            }
            @Override
            public void breakpointChanged(JavaScriptBreakpoint breakpoint) {
            }
            @Override
            public void attached() {
            }
        });
    }

    @Override
    protected void updateStackTrace() {
        if (jsDebugger.getCallStack() == null) {
            this.stackTrace = null;
        } else {
            JavaScriptCallFrame[] jsCallStack = jsDebugger.getCallStack();
            TeaVMJSStackFrame[] stackTrace = new TeaVMJSStackFrame[jsCallStack.length];
            for (int i = 0; i < jsCallStack.length; ++i) {
                JavaScriptCallFrame jsFrame = jsCallStack[i];
                stackTrace[i] = new TeaVMJSStackFrame(this, jsDebugger, jsFrame);
            }
            this.stackTrace = stackTrace;
        }
        fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
    }

    @Override
    public boolean canStepInto() {
        return debugTarget.canStepInto();
    }

    @Override
    public boolean canStepOver() {
        return debugTarget.canStepOver();
    }

    @Override
    public boolean canStepReturn() {
        return debugTarget.canStepReturn();
    }

    @Override
    public boolean isStepping() {
        return debugTarget.isStepping();
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
    public String getName() {
        return "JavaScript";
    }

    @Override
    public boolean isSuspended() {
        return jsDebugger.isSuspended();
    }

    @Override
    public void resume() throws DebugException {
        jsDebugger.resume();
    }

    @Override
    public void suspend() throws DebugException {
        jsDebugger.suspend();
    }
}
