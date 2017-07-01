/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.eclipse.debugger;

import java.util.*;
import org.teavm.debugging.Variable;

public class TeaVMJavaVariablesHolder extends TeaVMVariablesHolder {
    private String idPrefix;
    private TeaVMDebugTarget debugTarget;
    private Collection<Variable> teavmVariables;

    public TeaVMJavaVariablesHolder(String idPrefix, TeaVMDebugTarget debugTarget,
            Collection<Variable> teavmVariables) {
        this.idPrefix = idPrefix;
        this.debugTarget = debugTarget;
        this.teavmVariables = teavmVariables;
    }

    @Override
    protected TeaVMVariable[] createVariables() {
        TeaVMJavaVariable[] newVariables = new TeaVMJavaVariable[teavmVariables.size()];
        List<Variable> teavmVarList = new ArrayList<>(teavmVariables);
        Collections.sort(teavmVarList, new PropertyNameComparator<Variable>() {
            @Override String getName(Variable value) {
                return value.getName();
            }
        });
        for (int i = 0; i < teavmVarList.size(); ++i) {
            Variable var = teavmVarList.get(i);
            newVariables[i] = new TeaVMJavaVariable(idPrefix + "." + var.getName(), debugTarget, var);
        }
        return newVariables;
    }
}
