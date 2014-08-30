package org.teavm.eclipse.debugger;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
@SuppressWarnings("rawtypes")
public class TeaVMDebugProcess implements IProcess {
    private Map<String, String> attributes = new HashMap<>();
    private ILaunch launch;
    private TeaVMStreamsProxy streamsProxy = new TeaVMStreamsProxy();
    private TeaVMDebugTarget debugTarget;

    public TeaVMDebugProcess(ILaunch launch, TeaVMDebugTarget debugTarget) {
        this.launch = launch;
        this.debugTarget = debugTarget;
    }

    @Override
    public Object getAdapter(Class arg0) {
        return null;
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
    public String getAttribute(String attr) {
        return attributes.get(attr);
    }

    @Override
    public int getExitValue() throws DebugException {
        return 0;
    }

    @Override
    public String getLabel() {
        return "TeaVM debug process";
    }

    @Override
    public ILaunch getLaunch() {
        return launch;
    }

    @Override
    public IStreamsProxy getStreamsProxy() {
        return streamsProxy;
    }

    @Override
    public void setAttribute(String attr, String value) {
        attributes.put(attr, value);
    }
}
