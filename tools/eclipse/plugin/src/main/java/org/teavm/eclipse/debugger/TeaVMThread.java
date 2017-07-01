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

public abstract class TeaVMThread extends TeaVMDebugElement implements IThread {
    TeaVMDebugTarget debugTarget;
    protected volatile TeaVMStackFrame[] stackTrace;

    public TeaVMThread(TeaVMDebugTarget debugTarget) {
        super(debugTarget);
        this.debugTarget = debugTarget;
    }

    protected void updateStackTrace() {
        fireChangeEvent(0);
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

    @Override
    public boolean canResume() {
        return debugTarget.canResume();
    }

    @Override
    public boolean canSuspend() {
        return debugTarget.canSuspend();
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
    public IBreakpoint[] getBreakpoints() {
        return debugTarget.breakpointMap.keySet().toArray(new IBreakpoint[0]);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public abstract String getName();

    @Override
    public IStackFrame[] getStackFrames() {
        if (isTerminated()) {
            return new IStackFrame[0];
        }
        TeaVMStackFrame[] stackTrace = this.stackTrace;
        return stackTrace != null ? stackTrace.clone() : new IStackFrame[0];
    }

    @Override
    public IStackFrame getTopStackFrame() {
        if (isTerminated()) {
            return null;
        }
        TeaVMStackFrame[] stackTrace = this.stackTrace;
        return stackTrace != null && stackTrace.length > 0 ? stackTrace[0] : null;
    }

    @Override
    public boolean hasStackFrames() throws DebugException {
        return !isTerminated() && stackTrace != null;
    }
}
