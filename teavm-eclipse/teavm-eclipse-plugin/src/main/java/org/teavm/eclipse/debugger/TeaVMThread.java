package org.teavm.eclipse.debugger;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
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
