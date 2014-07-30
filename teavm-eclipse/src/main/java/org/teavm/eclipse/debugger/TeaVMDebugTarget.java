package org.teavm.eclipse.debugger;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
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
public class TeaVMDebugTarget implements IDebugTarget {
    private ILaunch launch;
    private Debugger teavmDebugger;
    private ChromeRDPServer server;
    private boolean terminated;

    public TeaVMDebugTarget(ILaunch launch, Debugger teavmDebugger, ChromeRDPServer server) {
        this.launch = launch;
        this.teavmDebugger = teavmDebugger;
        this.server = server;
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
            public void breakpointStatusChanged(Breakpoint breakpoint) {
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
    }

    @Override
    public Object getAdapter(Class arg0) {
        return null;
    }

    @Override
    public void breakpointAdded(IBreakpoint breakpoint) {
        if (!(breakpoint instanceof ILineBreakpoint)) {
            return;
        }
        IJavaLineBreakpoint lineBreakpoint = (IJavaLineBreakpoint)breakpoint;
        try {
            lineBreakpoint.setRegistered(true);
            String fileName = lineBreakpoint.getTypeName().replace('.', '/') + ".java";
            teavmDebugger.createBreakpoint(fileName, lineBreakpoint.getLineNumber());
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void breakpointChanged(IBreakpoint arg0, IMarkerDelta arg1) {
    }

    @Override
    public void breakpointRemoved(IBreakpoint arg0, IMarkerDelta arg1) {
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
        return "";
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
        return teavmDebugger.isAttached();
    }

    @Override
    public String getName() throws DebugException {
        return "TeaVM debugger";
    }

    @Override
    public IProcess getProcess() {
        return null;
    }

    @Override
    public IThread[] getThreads() throws DebugException {
        return null;
    }

    @Override
    public boolean hasThreads() throws DebugException {
        return false;
    }

    @Override
    public boolean supportsBreakpoint(IBreakpoint breakpoint) {
        return breakpoint instanceof IJavaLineBreakpoint;
    }
}
