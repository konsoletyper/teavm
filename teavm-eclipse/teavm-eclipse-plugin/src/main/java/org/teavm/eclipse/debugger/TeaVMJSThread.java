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
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.teavm.debugging.javascript.JavaScriptBreakpoint;
import org.teavm.debugging.javascript.JavaScriptCallFrame;
import org.teavm.debugging.javascript.JavaScriptDebugger;
import org.teavm.debugging.javascript.JavaScriptDebuggerListener;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMJSThread implements IThread {
    private JavaScriptDebugger jsDebugger;
    TeaVMDebugTarget debugTarget;
    private volatile TeaVMJSStackFrame[] stackTrace;

    public TeaVMJSThread(TeaVMDebugTarget debugTarget) {
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
            public void paused() {
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

    private void updateStackTrace() {
        if (jsDebugger.getCallStack() == null) {
            this.stackTrace = null;
        } else {
            JavaScriptCallFrame[] jsCallStack = jsDebugger.getCallStack();
            TeaVMJSStackFrame[] stackTrace = new TeaVMJSStackFrame[jsCallStack.length];
            for (int i = 0; i < jsCallStack.length; ++i) {
                JavaScriptCallFrame jsFrame = jsCallStack[i];
                stackTrace[i] = new TeaVMJSStackFrame(this, jsFrame);
            }
            this.stackTrace = stackTrace;
        }
        fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
    }

    private void fireEvent(DebugEvent event) {
        DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] { event });
    }

    @Override
    public boolean canTerminate() {
        return debugTarget.canTerminate();
    }

    @Override
    public boolean isTerminated() {
        return debugTarget.isTerminated();
    }

    @Override
    public void terminate() throws DebugException {
        debugTarget.terminate();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class type) {
        return null;
    }

    @Override
    public boolean canResume() {
        return debugTarget.canResume();
    }

    @Override
    public boolean canSuspend() {
        return debugTarget.canSuspend();
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
    public IDebugTarget getDebugTarget() {
        return debugTarget;
    }

    @Override
    public ILaunch getLaunch() {
        return debugTarget.launch;
    }

    @Override
    public String getModelIdentifier() {
        return TeaVMDebugConstants.THREAD_ID;
    }

    @Override
    public IBreakpoint[] getBreakpoints() {
        return debugTarget.breakpointMap.keySet().toArray(new IBreakpoint[0]);
    }

    @Override
    public String getName() throws DebugException {
        return "JavaScript";
    }

    @Override
    public int getPriority() throws DebugException {
        return 0;
    }

    @Override
    public IStackFrame[] getStackFrames() throws DebugException {
        if (isTerminated()) {
            return new IStackFrame[0];
        }
        TeaVMJSStackFrame[] stackTrace = this.stackTrace;
        return stackTrace != null ? stackTrace.clone() : new IStackFrame[0];
    }

    @Override
    public IStackFrame getTopStackFrame() {
        if (isTerminated()) {
            return null;
        }
        TeaVMJSStackFrame[] stackTrace = this.stackTrace;
        return stackTrace != null && stackTrace.length > 0 ? stackTrace[0] : null;
    }

    @Override
    public boolean hasStackFrames() throws DebugException {
        return !isTerminated() && stackTrace != null;
    }
}
