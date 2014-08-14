package org.teavm.eclipse.debugger;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.teavm.debugging.Variable;
import org.teavm.eclipse.TeaVMEclipsePlugin;

/**
 *
 * @author Alexey Andreev
 */
public class TeaVMVariable implements IVariable {
    private TeaVMDebugTarget debugTarget;
    private Variable var;
    private TeaVMValue value;

    public TeaVMVariable(TeaVMDebugTarget debugTarget, Variable var) {
        this.debugTarget = debugTarget;
        this.var = var;
        this.value = new TeaVMValue(debugTarget, var.getValue());
    }

    @Override
    public void setValue(IValue arg0) throws DebugException {
        throw new DebugException(new Status(Status.ERROR, TeaVMEclipsePlugin.ID, "Can't set value"));
    }

    @Override
    public void setValue(String arg0) throws DebugException {
        throw new DebugException(new Status(Status.ERROR, TeaVMEclipsePlugin.ID, "Can't set value"));
    }

    @Override
    public boolean supportsValueModification() {
        return false;
    }

    @Override
    public boolean verifyValue(IValue arg0) throws DebugException {
        return false;
    }

    @Override
    public boolean verifyValue(String arg0) throws DebugException {
        return false;
    }

    @Override
    public IDebugTarget getDebugTarget() {
        return debugTarget;
    }

    @Override
    public ILaunch getLaunch() {
        return debugTarget.getLaunch();
    }

    @Override
    public String getModelIdentifier() {
        return TeaVMDebugConstants.VARIABLE_ID;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class arg0) {
        return null;
    }

    @Override
    public String getName() throws DebugException {
        return var.getName();
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
        return var.getValue().getType();
    }

    @Override
    public IValue getValue() throws DebugException {
        return value;
    }

    @Override
    public boolean hasValueChanged() throws DebugException {
        return false;
    }
}
