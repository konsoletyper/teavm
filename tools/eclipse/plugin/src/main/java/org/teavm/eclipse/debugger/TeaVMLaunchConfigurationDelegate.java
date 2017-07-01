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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.teavm.chromerdp.ChromeRDPDebugger;
import org.teavm.chromerdp.ChromeRDPServer;
import org.teavm.debugging.Debugger;
import org.teavm.debugging.information.URLDebugInformationProvider;

public class TeaVMLaunchConfigurationDelegate extends LaunchConfigurationDelegate {
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {
        if (!mode.equals(ILaunchManager.DEBUG_MODE)) {
            throw new IllegalArgumentException("Only debug mode supported");
        }
        int port = configuration.getAttribute("teavm-debugger-port", 2357);
        final ChromeRDPServer server = new ChromeRDPServer();
        server.setPort(port);
        ChromeRDPDebugger jsDebugger = new ChromeRDPDebugger();
        server.setExchangeConsumer(jsDebugger);
        Debugger debugger = new Debugger(jsDebugger, new URLDebugInformationProvider(""));
        new Thread() {
            @Override public void run() {
                server.start();
            }
        }.start();
        TeaVMDebugTarget target = new TeaVMDebugTarget(launch, debugger, jsDebugger, server);
        launch.addDebugTarget(target);
        launch.addProcess(target.getProcess());
    }
}
