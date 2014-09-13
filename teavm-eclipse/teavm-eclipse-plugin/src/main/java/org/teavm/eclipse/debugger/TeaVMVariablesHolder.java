package org.teavm.eclipse.debugger;

import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public abstract class TeaVMVariablesHolder {
    private AtomicReference<TeaVMVariable[]> variables = new AtomicReference<>();

    public TeaVMVariable[] getVariables() {
        if (variables.get() == null) {
            variables.compareAndSet(null, createVariables());
        }
        return variables.get();
    }

    protected abstract TeaVMVariable[] createVariables();
}
