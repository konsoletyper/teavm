package org.teavm.eclipse.debugger;

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMSourceLookupDirector extends AbstractSourceLookupDirector {
    @Override
    public void initializeParticipants() {
        addParticipants(new ISourceLookupParticipant[] { new TeaVMSourceLookupParticipant() });
    }
}
