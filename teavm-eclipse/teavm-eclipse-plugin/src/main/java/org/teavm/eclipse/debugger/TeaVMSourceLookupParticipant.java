package org.teavm.eclipse.debugger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;
import org.teavm.debugging.information.SourceLocation;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMSourceLookupParticipant extends AbstractSourceLookupParticipant {
    @Override
    public String getSourceName(Object object) throws CoreException {
        if (!(object instanceof TeaVMStackFrame)) {
            return null;
        }
        TeaVMStackFrame stackFrame = (TeaVMStackFrame)object;
        SourceLocation location = stackFrame.callFrame.getLocation();
        return location != null ? location.getFileName() : null;
    }
}
