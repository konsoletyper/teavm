package org.teavm.eclipse.debugger;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.teavm.debugging.Value;

/**
 *
 * @author Alexey Andreev
 */
public class TeaVMValue implements IValue {
    TeaVMDebugTarget debugTarget;
    private Value teavmValue;
    private TeaVMVariablesHolder variablesHolder;

    public TeaVMValue(TeaVMDebugTarget debugTarget, Value teavmValue) {
        this.debugTarget = debugTarget;
        this.teavmValue = teavmValue;
        this.variablesHolder = new TeaVMVariablesHolder(debugTarget, teavmValue.getProperties().values());
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
        return TeaVMDebugConstants.VALUE_ID;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class arg0) {
        return null;
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
        return teavmValue.getType();
    }

    @Override
    public String getValueString() throws DebugException {
        return teavmValue.getRepresentation();
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        return variablesHolder.getVariables();
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return true;
    }

    @Override
    public boolean isAllocated() throws DebugException {
        return true;
    }
}
