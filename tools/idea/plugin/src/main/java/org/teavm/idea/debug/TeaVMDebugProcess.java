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

import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
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
    private final TeaVMLineBreakpointHandler breakpointHandler;
    private final int port;
    private ChromeRDPServer debugServer;

    public TeaVMDebugProcess(@NotNull XDebugSession session, int port) {
        super(session);
        this.port = port;
        innerDebugger = initDebugger();
        innerDebugger.addListener(new DebuggerListener() {
            @Override
            public void resumed() {
            }

            @Override
            public void paused() {
                handlePaused();
            }

            @Override
            public void breakpointStatusChanged(Breakpoint breakpoint) {
            }

            @Override
            public void attached() {
            }

            @Override
            public void detached() {
            }
        });

        breakpointHandler = new TeaVMLineBreakpointHandler(session.getProject(), innerDebugger);
    }

    private Debugger initDebugger() {
        debugServer = new ChromeRDPServer();
        debugServer.setPort(port);
        ChromeRDPDebugger chromeDebugger = new ChromeRDPDebugger();
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
    public void startStepOver() {
        innerDebugger.stepOver();
    }

    @Override
    public void startStepInto() {
        innerDebugger.stepInto();
    }

    @Override
    public void startStepOut() {
        innerDebugger.stepOut();
    }

    @Override
    public void resume() {
        innerDebugger.resume();
    }

    @Override
    public void stop() {
        debugServer.stop();
    }

    @Override
    public void runToPosition(@NotNull XSourcePosition position) {
        innerDebugger.continueToLocation(position.getFile().getPath(), position.getLine());
    }

    private void handlePaused() {
        getSession().positionReached(new TeaVMSuspendContext(innerDebugger, getSession().getProject()));
    }

    @NotNull
    @Override
    public XBreakpointHandler<?>[] getBreakpointHandlers() {
        return new XBreakpointHandler[] { breakpointHandler };
    }
}
