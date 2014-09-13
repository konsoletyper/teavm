package org.teavm.eclipse.debugger;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public abstract class TeaVMValue extends TeaVMDebugElement implements IValue {
    private TeaVMVariablesHolder variablesHolder;

    public TeaVMValue(TeaVMDebugTarget debugTarget, TeaVMVariablesHolder variablesHolder) {
        super(debugTarget);
        this.variablesHolder = variablesHolder;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        return variablesHolder.getVariables();
    }

    @Override
    public boolean isAllocated() throws DebugException {
        return true;
    }

    public abstract String getDescription();
}
