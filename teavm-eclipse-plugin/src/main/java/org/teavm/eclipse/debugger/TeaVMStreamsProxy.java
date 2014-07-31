package org.teavm.eclipse.debugger;

import java.io.IOException;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
// TODO: implement interaction with browser console
public class TeaVMStreamsProxy implements IStreamsProxy {
    @Override
    public IStreamMonitor getErrorStreamMonitor() {
        return new TeaVMStreamMonitor();
    }

    @Override
    public IStreamMonitor getOutputStreamMonitor() {
        return new TeaVMStreamMonitor();
    }

    @Override
    public void write(String text) throws IOException {
    }
}
