package org.teavm.eclipse.debugger;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public abstract class TeaVMStackFrame extends TeaVMDebugElement implements IStackFrame {
    TeaVMThread thread;

    public TeaVMStackFrame(TeaVMThread thread) {
        super(thread.getDebugTarget());
        this.thread = thread;
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
    public int getCharEnd() throws DebugException {
        return -1;
    }

    @Override
    public int getCharStart() throws DebugException {
        return -1;
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
    public boolean hasRegisterGroups() throws DebugException {
        return false;
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return true;
    }
}
