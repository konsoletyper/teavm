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
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.*;
import org.teavm.debugging.CallFrame;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMStackFrame implements IStackFrame {
    TeaVMThread thread;
    CallFrame callFrame;
    private TeaVMVariablesHolder variablesHolder;

    public TeaVMStackFrame(TeaVMThread thread, CallFrame callFrame) {
        this.thread = thread;
        this.callFrame = callFrame;
        this.variablesHolder = new TeaVMVariablesHolder(thread.debugTarget, callFrame.getVariables().values());
    }

    public CallFrame getCallFrame() {
        return callFrame;
    }

    @Override
    public boolean canTerminate() {
        return thread.canTerminate();
    }

    @Override
    public boolean isTerminated() {
        return thread.isTerminated();
    }

    @Override
    public void terminate() throws DebugException {
        thread.terminate();
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        if (type.equals(ILaunch.class)) {
            return thread.debugTarget.getLaunch();
        } else if (type.equals(IDebugElement.class)) {
            return this;
        }
        return null;
    }

    @Override
    public boolean canStepInto() {
        return thread.getTopStackFrame() == this;
    }

    @Override
    public boolean canStepOver() {
        return thread.getTopStackFrame() == this;
    }

    @Override
    public boolean canStepReturn() {
        return thread.getTopStackFrame() == this;
    }

    @Override
    public boolean isStepping() {
        return false;
    }

    @Override
    public void stepInto() throws DebugException {
        thread.stepInto();
    }

    @Override
    public void stepOver() throws DebugException {
        thread.stepOver();
    }

    @Override
    public void stepReturn() throws DebugException {
        thread.stepReturn();
    }

    @Override
    public boolean canResume() {
        return thread.getTopStackFrame() == this;
    }

    @Override
    public boolean canSuspend() {
        return thread.getTopStackFrame() == this;
    }

    @Override
    public boolean isSuspended() {
        return thread.isSuspended();
    }

    @Override
    public void resume() throws DebugException {
        thread.resume();
    }

    @Override
    public void suspend() throws DebugException {
        thread.suspend();
    }

    @Override
    public IDebugTarget getDebugTarget() {
        return thread.getDebugTarget();
    }

    @Override
    public ILaunch getLaunch() {
        return thread.getLaunch();
    }

    @Override
    public String getModelIdentifier() {
        return TeaVMDebugConstants.STACK_FRAME_ID;
    }

    @Override
    public int getCharEnd() throws DebugException {
        return -1;
    }

    @Override
    public int getCharStart() throws DebugException {
        return -1;
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
    public IRegisterGroup[] getRegisterGroups() throws DebugException {
        return null;
    }

    @Override
    public IThread getThread() {
        return thread;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        return variablesHolder.getVariables();
    }

    @Override
    public boolean hasRegisterGroups() throws DebugException {
        return false;
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return true;
    }
}
