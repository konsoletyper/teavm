package org.teavm.eclipse.debugger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.teavm.chromerdp.ChromeRDPDebugger;
import org.teavm.chromerdp.ChromeRDPServer;
import org.teavm.debugging.Debugger;
import org.teavm.debugging.information.URLDebugInformationProvider;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {
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
        TeaVMDebugTarget target = new TeaVMDebugTarget(launch, debugger, server);
        launch.addDebugTarget(target);
        launch.addProcess(target.getProcess());
    }
}
