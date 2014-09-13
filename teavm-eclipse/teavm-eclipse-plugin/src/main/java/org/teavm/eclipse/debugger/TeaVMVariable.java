package org.teavm.eclipse.debugger;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.teavm.eclipse.TeaVMEclipsePlugin;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public abstract class TeaVMVariable extends TeaVMDebugElement implements IVariable {
    private TeaVMValue value;

    public TeaVMVariable(TeaVMDebugTarget debugTarget, TeaVMValue value) {
        super(debugTarget);
        this.value = value;
    }

    @Override
    public void setValue(IValue value) throws DebugException {
        throw new DebugException(new Status(Status.ERROR, TeaVMEclipsePlugin.ID, "Can't set value"));
    }

    @Override
    public void setValue(String value) throws DebugException {
        throw new DebugException(new Status(Status.ERROR, TeaVMEclipsePlugin.ID, "Can't set value"));
    }

    @Override
    public boolean supportsValueModification() {
        return false;
    }

    @Override
    public boolean verifyValue(IValue value) throws DebugException {
        return false;
    }

    @Override
    public boolean verifyValue(String value) throws DebugException {
        return false;
    }

    @Override
    public TeaVMValue getValue(){
        return value;
    }

    @Override
    public boolean hasValueChanged() throws DebugException {
        return false;
    }
}
