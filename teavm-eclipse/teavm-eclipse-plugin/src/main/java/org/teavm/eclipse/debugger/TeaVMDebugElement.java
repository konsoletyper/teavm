package org.teavm.eclipse.debugger;

import org.eclipse.debug.core.model.DebugElement;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public abstract class TeaVMDebugElement extends DebugElement {
    public TeaVMDebugElement(TeaVMDebugTarget target) {
        super(target);
    }

    @Override
    public String getModelIdentifier() {
        return getDebugTarget().getModelIdentifier();
    }

    @Override
    public TeaVMDebugTarget getDebugTarget() {
        return (TeaVMDebugTarget)super.getDebugTarget();
    }
}
