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

import static org.teavm.eclipse.debugger.TeaVMDebugConstants.DEBUG_TARGET_ID;
import static org.teavm.eclipse.debugger.TeaVMDebugConstants.JAVA_BREAKPOINT_INSTALL_COUNT;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
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
import org.teavm.debugging.javascript.JavaScriptDebugger;

public class TeaVMDebugTarget extends PlatformObject implements IDebugTarget, IStep {
    ILaunch launch;
    Debugger teavmDebugger;
    JavaScriptDebugger jsDebugger;
    private ChromeRDPServer server;
    private volatile boolean terminated;
    private TeaVMDebugProcess process;
    private TeaVMJavaThread thread;
    private TeaVMJSThread jsThread;
    ConcurrentMap<IBreakpoint, Breakpoint> breakpointMap = new ConcurrentHashMap<>();
    ConcurrentMap<Breakpoint, IJavaLineBreakpoint> breakpointBackMap = new ConcurrentHashMap<>();
    private Map<String, Integer> instanceIdMap = new WeakHashMap<>();

    public TeaVMDebugTarget(ILaunch launch, final Debugger teavmDebugger, JavaScriptDebugger jsDebugger,
            ChromeRDPServer server) {
        this.launch = launch;
        this.teavmDebugger = teavmDebugger;
        this.jsDebugger = jsDebugger;
        this.server = server;
        this.process = new TeaVMDebugProcess(this);
        this.thread = new TeaVMJavaThread(this);
        this.jsThread = new TeaVMJSThread(this);
        DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
        for (IBreakpoint breakpoint : DebugPlugin.getDefault().getBreakpointManager().getBreakpoints()) {
            breakpointAdded(breakpoint);
        }
        teavmDebugger.addListener(new DebuggerListener() {
            @Override
            public void resumed() {
                fireEvent(new DebugEvent(TeaVMDebugTarget.this, DebugEvent.RESUME));
                thread.fireResumeEvent(0);
                if (jsThread != null) {
                    jsThread.fireResumeEvent(0);
                }
            }

            @Override
            public void paused(Breakpoint breakpoint) {
                fireEvent(new DebugEvent(TeaVMDebugTarget.this, DebugEvent.SUSPEND));
                thread.fireSuspendEvent(0);
                thread.fireChangeEvent(0);
                if (jsThread != null) {
                    jsThread.fireSuspendEvent(0);
                    jsThread.fireChangeEvent(0);
                }
            }

            @Override
            public void detached() {
                fireEvent(new DebugEvent(TeaVMDebugTarget.this, DebugEvent.CHANGE));
                thread.fireChangeEvent(0);
                if (jsThread != null) {
                    jsThread.fireChangeEvent(0);
                }
                for (Breakpoint teavmBreakpoint : teavmDebugger.getBreakpoints()) {
                    updateBreakpoint(teavmBreakpoint);
                }
            }

            @Override
            public void breakpointStatusChanged(Breakpoint teavmBreakpoint) {
                updateBreakpoint(teavmBreakpoint);
            }

            @Override
            public void attached() {
                fireEvent(new DebugEvent(TeaVMDebugTarget.this, DebugEvent.CHANGE));
                for (Breakpoint teavmBreakpoint : teavmDebugger.getBreakpoints()) {
                    updateBreakpoint(teavmBreakpoint);
                }
            }
        });
    }

    private void updateBreakpoint(Breakpoint teavmBreakpoint) {
        IJavaLineBreakpoint breakpoint = breakpointBackMap.get(teavmBreakpoint);
        if (breakpoint != null) {
            try {
                if (!teavmBreakpoint.isValid() || !teavmDebugger.isAttached()) {
                    breakpoint.getMarker().setAttribute(JAVA_BREAKPOINT_INSTALL_COUNT, 0);
                } else {
                    breakpoint.getMarker().setAttribute(JAVA_BREAKPOINT_INSTALL_COUNT, 1);
                }
                DebugPlugin.getDefault().getBreakpointManager().fireBreakpointChanged(breakpoint);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
            fireEvent(new DebugEvent(breakpoint, DebugEvent.CHANGE));
        }
    }

    private void fireEvent(DebugEvent event) {
        DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] { event });
    }

    @Override
    public boolean canTerminate() {
        return !terminated;
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public void terminate() throws DebugException {
        terminated = true;
        server.stop();
        thread.fireTerminateEvent();
        jsThread.fireTerminateEvent();
        fireEvent(new DebugEvent(process, DebugEvent.TERMINATE));
        fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
    }

    @Override
    public void breakpointAdded(IBreakpoint breakpoint) {
        try {
            IJavaLineBreakpoint lineBreakpoint = (IJavaLineBreakpoint)breakpoint;
            String fileName = lineBreakpoint.getTypeName().replace('.', '/') + ".java";
            Breakpoint teavmBreakpoint = teavmDebugger.createBreakpoint(fileName, lineBreakpoint.getLineNumber());
            breakpointMap.put(lineBreakpoint, teavmBreakpoint);
            breakpointBackMap.put(teavmBreakpoint, lineBreakpoint);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta markerDelta) {
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
        return !terminated;
    }

    @Override
    public boolean canSuspend() {
        return !terminated;
    }

    @Override
    public boolean isSuspended() {
        return teavmDebugger.isSuspended() && !terminated;
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
        return DEBUG_TARGET_ID;
    }

    @Override
    public boolean canDisconnect() {
        return !terminated && !isDisconnected();
    }

    @Override
    public void disconnect() throws DebugException {
        teavmDebugger.detach();
    }

    @Override
    public boolean isDisconnected() {
        return !teavmDebugger.isAttached();
    }

    @Override
    public String getName() {
        return "TeaVM debugger";
    }

    @Override
    public IProcess getProcess() {
        return process;
    }

    @Override
    public IThread[] getThreads() throws DebugException {
        return !terminated ? new IThread[] { thread, jsThread } : new IThread[0];
    }

    @Override
    public boolean hasThreads() throws DebugException {
        return !terminated;
    }

    @Override
    public boolean supportsBreakpoint(IBreakpoint breakpoint) {
        return breakpoint instanceof IJavaLineBreakpoint;
    }

    @Override
    public boolean canStepInto() {
        return !terminated;
    }

    @Override
    public boolean canStepOver() {
        return !terminated;
    }

    @Override
    public boolean canStepReturn() {
        return !terminated;
    }

    @Override
    public boolean isStepping() {
        return !terminated;
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

    public int getId(String instanceId) {
        synchronized (instanceIdMap) {
            Integer id = instanceIdMap.get(instanceId);
            if (id == null) {
                id = instanceIdMap.size();
                instanceIdMap.put(instanceId, id);
            }
            return id;
        }
    }
}
