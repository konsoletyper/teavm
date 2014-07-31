package org.teavm.eclipse.debugger;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.*;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.teavm.chromerdp.ChromeRDPServer;
import org.teavm.debugging.Breakpoint;
import org.teavm.debugging.Debugger;
import org.teavm.debugging.DebuggerListener;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@SuppressWarnings("rawtypes")
public class TeaVMDebugTarget implements IDebugTarget, IStep {
    ILaunch launch;
    Debugger teavmDebugger;
    private ChromeRDPServer server;
    private boolean terminated;
    private TeaVMDebugProcess process;
    private TeaVMThread thread;
    Map<IBreakpoint, Breakpoint> breakpointMap = new HashMap<>();
    Map<Breakpoint, IBreakpoint> breakpointBackMap = new HashMap<>();

    public TeaVMDebugTarget(ILaunch launch, Debugger teavmDebugger, ChromeRDPServer server) {
        this.launch = launch;
        this.teavmDebugger = teavmDebugger;
        this.server = server;
        this.process = new TeaVMDebugProcess(launch);
        this.thread = new TeaVMThread(this);
        DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
        for (IBreakpoint breakpoint : DebugPlugin.getDefault().getBreakpointManager().getBreakpoints()) {
            breakpointAdded(breakpoint);
        }
        teavmDebugger.addListener(new DebuggerListener() {
            @Override
            public void resumed() {
                fireEvent(new DebugEvent(TeaVMDebugTarget.this, DebugEvent.RESUME));
            }

            @Override
            public void paused() {
                fireEvent(new DebugEvent(TeaVMDebugTarget.this, DebugEvent.SUSPEND));
            }

            @Override
            public void detached() {
                fireEvent(new DebugEvent(TeaVMDebugTarget.this, DebugEvent.CHANGE));
            }

            @Override
            public void breakpointStatusChanged(Breakpoint teavmBreakpoint) {
                IBreakpoint breakpoint = breakpointBackMap.get(teavmBreakpoint);
                if (breakpoint != null) {
                    fireEvent(new DebugEvent(breakpoint, DebugEvent.CHANGE));
                }
            }

            @Override
            public void attached() {
                fireEvent(new DebugEvent(TeaVMDebugTarget.this, DebugEvent.CHANGE));
            }
        });
    }

    private void fireEvent(DebugEvent event) {
        DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] { event });
    }

    @Override
    public boolean canTerminate() {
        return true;
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public void terminate() throws DebugException {
        terminated = true;
        server.stop();
        fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
    }

    @Override
    public Object getAdapter(Class arg0) {
        return null;
    }

    @Override
    public void breakpointAdded(IBreakpoint breakpoint) {
        try {
            if (breakpoint instanceof IJavaLineBreakpoint) {
                IJavaLineBreakpoint lineBreakpoint = (IJavaLineBreakpoint)breakpoint;
                String fileName = lineBreakpoint.getTypeName().replace('.', '/') + ".java";
                Breakpoint teavmBreakpoint = teavmDebugger.createBreakpoint(fileName, lineBreakpoint.getLineNumber());
                breakpointMap.put(lineBreakpoint, teavmBreakpoint);
                breakpointBackMap.put(teavmBreakpoint, lineBreakpoint);
                breakpoint.setRegistered(true);
            } else {
                breakpoint.setRegistered(false);
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta markerDelta) {
        breakpointRemoved(breakpoint, markerDelta);
        breakpointAdded(breakpoint);
    }

    @Override
    public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta markerDelta) {
        Breakpoint teavmBreakpoint = breakpointMap.remove(breakpoint);
        if (teavmBreakpoint != null) {
            teavmBreakpoint.destroy();
            breakpointBackMap.remove(teavmBreakpoint);
        }
    }

    @Override
    public boolean canResume() {
        return true;
    }

    @Override
    public boolean canSuspend() {
        return true;
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
    public IMemoryBlock getMemoryBlock(long arg0, long arg1) throws DebugException {
        return null;
    }

    @Override
    public boolean supportsStorageRetrieval() {
        return false;
    }

    @Override
    public IDebugTarget getDebugTarget() {
        return this;
    }

    @Override
    public ILaunch getLaunch() {
        return launch;
    }

    @Override
    public String getModelIdentifier() {
        return "org.teavm.eclipse.debugger";
    }

    @Override
    public boolean canDisconnect() {
        return true;
    }

    @Override
    public void disconnect() throws DebugException {
    }

    @Override
    public boolean isDisconnected() {
        return !teavmDebugger.isAttached();
    }

    @Override
    public String getName() throws DebugException {
        return "TeaVM debugger";
    }

    @Override
    public IProcess getProcess() {
        return process;
    }

    @Override
    public IThread[] getThreads() throws DebugException {
        return new IThread[] { thread };
    }

    @Override
    public boolean hasThreads() throws DebugException {
        return true;
    }

    @Override
    public boolean supportsBreakpoint(IBreakpoint breakpoint) {
        return breakpoint instanceof IJavaLineBreakpoint;
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
}
