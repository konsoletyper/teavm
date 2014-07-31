package org.teavm.eclipse.debugger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.ui.PlatformUI;
import org.teavm.chromerdp.*;
import org.teavm.debugging.Debugger;
import org.teavm.debugging.URLDebugInformationProvider;

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
        server.setExchangeConsumer(new SynchronousMessageExchange(jsDebugger));
        Debugger debugger = new Debugger(jsDebugger, new URLDebugInformationProvider(""));
        new Thread() {
            @Override public void run() {
                server.start();
            }
        }.start();
        launch.addDebugTarget(new TeaVMDebugTarget(launch, debugger, server));
    }

    private static class SynchronousMessageExchange implements ChromeRDPExchangeConsumer,
            ChromeRDPExchange {
        private List<ChromeRDPExchangeListener> listeners = new ArrayList<>();
        private ChromeRDPDebugger debugger;
        private ChromeRDPExchange exchange;

        public SynchronousMessageExchange(ChromeRDPDebugger debugger) {
            this.debugger = debugger;
        }

        @Override
        public void setExchange(ChromeRDPExchange exchange) {
            this.exchange = exchange;
            debugger.setExchange(this);
            exchange.addListener(new ChromeRDPExchangeListener() {
                @Override public void received(final String message) throws IOException {
                    postToUIThread(message);
                }
            });
        }

        private void postToUIThread(final String message) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                @Override public void run() {
                    try {
                        for (ChromeRDPExchangeListener listener : listeners) {
                            listener.received(message);
                        }
                    } catch (IOException e) {
                        // TODO: add logging
                    }
                }
            });
        }

        @Override
        public void send(String message) {
            exchange.send(message);
        }

        @Override
        public void addListener(ChromeRDPExchangeListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeListener(ChromeRDPExchangeListener listener) {
            listeners.remove(listener);
        }
    }
}
