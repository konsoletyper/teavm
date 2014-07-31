package org.teavm.eclipse.debugger;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IRegisterGroup;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IVariable;
import org.teavm.debugging.CallFrame;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMStackFrame implements IStackFrame {
    private TeaVMThread thread;
    CallFrame callFrame;

    public TeaVMStackFrame(TeaVMThread thread, CallFrame callFrame) {
        this.thread = thread;
        this.callFrame = callFrame;
    }

    @Override
    public boolean canTerminate() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public void terminate() throws DebugException {
    }

    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") Class type) {
        return null;
    }

    @Override
    public boolean canStepInto() {
        return false;
    }

    @Override
    public boolean canStepOver() {
        return false;
    }

    @Override
    public boolean canStepReturn() {
        return false;
    }

    @Override
    public boolean isStepping() {
        return false;
    }

    @Override
    public void stepInto() throws DebugException {
    }

    @Override
    public void stepOver() throws DebugException {
    }

    @Override
    public void stepReturn() throws DebugException {
    }

    @Override
    public boolean canResume() {
        return false;
    }

    @Override
    public boolean canSuspend() {
        return false;
    }

    @Override
    public boolean isSuspended() {
        return false;
    }

    @Override
    public void resume() throws DebugException {
    }

    @Override
    public void suspend() throws DebugException {
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
        return thread.getModelIdentifier();
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
        return callFrame.getLocation() != null ? callFrame.getLocation().getLine() : -1;
    }

    @Override
    public String getName() throws DebugException {
        return callFrame.getLocation() != null ? callFrame.getLocation().getFileName() : "unknown";
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
        return new IVariable[0];
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
