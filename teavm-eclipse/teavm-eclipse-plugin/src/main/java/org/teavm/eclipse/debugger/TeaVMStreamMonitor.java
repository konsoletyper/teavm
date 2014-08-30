package org.teavm.eclipse.debugger;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMStreamMonitor implements IStreamMonitor {
    @Override
    public void addListener(IStreamListener listener) {
    }

    @Override
    public String getContents() {
        return "";
    }

    @Override
    public void removeListener(IStreamListener arg0) {
    }
}
