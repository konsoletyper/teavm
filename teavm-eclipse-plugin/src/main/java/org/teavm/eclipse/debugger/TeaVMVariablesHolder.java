package org.teavm.eclipse.debugger;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.teavm.debugging.Variable;

/**
 *
 * @author Alexey Andreev
 */
public class TeaVMVariablesHolder {
    private TeaVMDebugTarget debugTarget;
    private Collection<Variable> teavmVariables;
    private AtomicReference<TeaVMVariable[]> variables = new AtomicReference<>();

    public TeaVMVariablesHolder(TeaVMDebugTarget debugTarget, Collection<Variable> teavmVariables) {
        this.debugTarget = debugTarget;
        this.teavmVariables = teavmVariables;
    }

    public TeaVMVariable[] getVariables() {
        if (variables.get() == null) {
            TeaVMVariable[] newVariables = new TeaVMVariable[teavmVariables.size()];
            List<Variable> teavmVarList = new ArrayList<>(teavmVariables);
            Collections.sort(teavmVarList, new Comparator<Variable>() {
                @Override public int compare(Variable o1, Variable o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            for (int i = 0; i < teavmVarList.size(); ++i) {
                newVariables[i] = new TeaVMVariable(debugTarget, teavmVarList.get(i));
            }
            variables.compareAndSet(null, newVariables);
        }
        return variables.get();
    }
}
