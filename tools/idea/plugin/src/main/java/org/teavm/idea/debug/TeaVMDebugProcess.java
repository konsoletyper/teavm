/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.idea.debug;

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XSuspendContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.teavm.chromerdp.ChromeRDPDebugger;
import org.teavm.chromerdp.ChromeRDPServer;
import org.teavm.debugging.Breakpoint;
import org.teavm.debugging.Debugger;
import org.teavm.debugging.DebuggerListener;
import org.teavm.debugging.information.URLDebugInformationProvider;

public class TeaVMDebugProcess extends XDebugProcess {
    public static final Key<Breakpoint> INNER_BREAKPOINT_KEY = new Key<>("TeaVM breakpoint");
    private TeaVMDebuggerEditorsProvider editorsProvider;
    private final Debugger innerDebugger;
    private final List<TeaVMLineBreakpointHandler<?>> breakpointHandlers = new ArrayList<>();
    private final int port;
    private ChromeRDPServer debugServer;
    ConcurrentMap<Breakpoint, XBreakpoint<?>> breakpointMap = new ConcurrentHashMap<>();
    public ExecutionConsole console;

    public TeaVMDebugProcess(@NotNull XDebugSession session, int port) {
        super(session);
        this.port = port;
        innerDebugger = initDebugger();
        innerDebugger.addListener(new DebuggerListener() {
            @Override
            public void resumed() {
            }

            @Override
            public void paused(Breakpoint breakpoint) {
                XBreakpoint<?> xBreakpoint = breakpoint != null ? breakpointMap.get(breakpoint) : null;
                if (xBreakpoint != null) {
                    getSession().breakpointReached(xBreakpoint, null,
                            new TeaVMSuspendContext(innerDebugger, getSession().getProject()));
                } else {
                    handlePaused();
                }
            }

            @Override
            public void breakpointStatusChanged(Breakpoint breakpoint) {
                updateBreakpointStatus(breakpoint);
            }

            @Override
            public void attached() {
                updateAllBreakpoints();
            }

            @Override
            public void detached() {
                updateAllBreakpoints();
            }
        });

        breakpointHandlers.add(new TeaVMLineBreakpointHandler<>(JavaLineBreakpointType.class, session.getProject(),
                innerDebugger, this));

        ExtensionPoint<TeaVMBreakpointProvider<?>> breakpointProvider = Extensions.getArea(
                session.getProject()).getExtensionPoint("org.teavm.extensions.breakpointProvider");
        if (breakpointProvider != null) {
            for (TeaVMBreakpointProvider<?> provider : breakpointProvider.getExtensions()) {
                breakpointHandlers.add(new TeaVMLineBreakpointHandler<>(provider.getBreakpointType(),
                        session.getProject(), innerDebugger, this));
            }
        }
    }

    private Debugger initDebugger() {
        debugServer = new ChromeRDPServer();
        debugServer.setPort(port);
        Application application = ApplicationManager.getApplication();
        ChromeRDPDebugger chromeDebugger = new ChromeRDPDebugger(application::invokeLater);
        debugServer.setExchangeConsumer(chromeDebugger);
        editorsProvider = new TeaVMDebuggerEditorsProvider();

        new Thread(debugServer::start).start();
        return new Debugger(chromeDebugger, new URLDebugInformationProvider(""));
    }

    @NotNull
    @Override
    public XDebuggerEditorsProvider getEditorsProvider() {
        return editorsProvider;
    }

    @Override
    public void startStepOver(@Nullable XSuspendContext context) {
        innerDebugger.stepOver();
    }

    @Override
    public void startStepInto(@Nullable XSuspendContext context) {
        innerDebugger.stepInto();
    }

    @Override
    public void startStepOut(@Nullable XSuspendContext context) {
        innerDebugger.stepOut();
    }

    @Override
    public void resume(@Nullable XSuspendContext context) {
        innerDebugger.resume();
    }

    @Override
    public void stop() {
        debugServer.stop();
    }

    @Override
    public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext context) {
        innerDebugger.continueToLocation(position.getFile().getPath(), position.getLine());
    }

    private void handlePaused() {
        getSession().positionReached(new TeaVMSuspendContext(innerDebugger, getSession().getProject()));
    }

    @Override
    public boolean checkCanPerformCommands() {
        return innerDebugger.isSuspended() && innerDebugger.isAttached();
    }

    void updateBreakpointStatus(Breakpoint breakpoint) {
        XBreakpoint<?> xBreakpoint = breakpointMap.get(breakpoint);
        if (xBreakpoint instanceof XLineBreakpoint) {
            XLineBreakpoint<?> xLineBreakpoint = (XLineBreakpoint<?>) xBreakpoint;
            if (!innerDebugger.isAttached()) {
                getSession().updateBreakpointPresentation(xLineBreakpoint, null,
                        null);
            } else if (breakpoint.isValid()) {
                getSession().updateBreakpointPresentation(xLineBreakpoint, AllIcons.Debugger.Db_verified_breakpoint,
                        null);
            } else {
                getSession().updateBreakpointPresentation(xLineBreakpoint, AllIcons.Debugger.Db_invalid_breakpoint,
                        "Could not set breakpoint in remote process");
            }
        }
    }

    @Override
    public String getCurrentStateMessage() {
        return !innerDebugger.isAttached() ? "Detached" : super.getCurrentStateMessage();
    }

    private void updateAllBreakpoints() {
        for (Breakpoint breakpoint : breakpointMap.keySet().toArray(new Breakpoint[0])) {
            updateBreakpointStatus(breakpoint);
        }
    }

    @NotNull
    @Override
    public XBreakpointHandler<?>[] getBreakpointHandlers() {
        return breakpointHandlers.toArray(new XBreakpointHandler<?>[0]);
    }

    @NotNull
    @Override
    public ExecutionConsole createConsole() {
        return console != null ? console : super.createConsole();
    }
}
