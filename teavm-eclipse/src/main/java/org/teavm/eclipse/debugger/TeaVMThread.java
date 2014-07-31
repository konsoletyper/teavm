package org.teavm.eclipse.debugger;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.teavm.debugging.Debugger;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMThread implements IThread {
    private Debugger teavmDebugger;
    private TeaVMDebugTarget debugTarget;

    public TeaVMThread(TeaVMDebugTarget debugTarget) {
        this.debugTarget = debugTarget;
        this.teavmDebugger = debugTarget.teavmDebugger;
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

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class type) {
        return null;
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
        return teavmDebugger.isSuspended();
    }

    @Override
    public void resume() throws DebugException {
    }

    @Override
    public void suspend() throws DebugException {
    }

    @Override
    public boolean canStepInto() {
        return true;
    }

    @Override
    public boolean canStepOver() {
        return true;
    }

    @Override
    public boolean canStepReturn() {
        return true;
    }

    @Override
    public boolean isStepping() {
        return false;
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
    public IDebugTarget getDebugTarget() {
        return debugTarget;
    }

    @Override
    public ILaunch getLaunch() {
        return debugTarget.launch;
    }

    @Override
    public String getModelIdentifier() {
        return debugTarget.getModelIdentifier();
    }

    @Override
    public IBreakpoint[] getBreakpoints() {
        return debugTarget.breakpointMap.keySet().toArray(new IBreakpoint[0]);
    }

    @Override
    public String getName() throws DebugException {
        return "main";
    }

    @Override
    public int getPriority() throws DebugException {
        return 0;
    }

    @Override
    public IStackFrame[] getStackFrames() throws DebugException {
        return null;
    }

    @Override
    public IStackFrame getTopStackFrame() throws DebugException {
        return null;
    }

    @Override
    public boolean hasStackFrames() throws DebugException {
        return true;
    }
}
