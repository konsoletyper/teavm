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
import org.teavm.debugging.javascript.JavaScriptValue;
import org.teavm.debugging.javascript.JavaScriptVariable;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMJSVariablesHolder extends TeaVMVariablesHolder {
    private TeaVMDebugTarget debugTarget;
    private Collection<JavaScriptVariable> teavmVariables;
    private JavaScriptValue thisScope;
    private JavaScriptValue closureScope;

    public TeaVMJSVariablesHolder(TeaVMDebugTarget debugTarget, Collection<JavaScriptVariable> teavmVariables,
            JavaScriptValue thisScope, JavaScriptValue closureScope) {
        this.debugTarget = debugTarget;
        this.teavmVariables = teavmVariables;
        this.thisScope = thisScope;
        this.closureScope = closureScope;
    }

    @Override
    protected TeaVMVariable[] createVariables() {
        List<TeaVMVariable> variables = new ArrayList<>();
        if (thisScope != null) {
            variables.add(new TeaVMJSScope(debugTarget, "this", thisScope));
        }
        if (closureScope != null) {
            variables.add(new TeaVMJSScope(debugTarget, "<closure>", closureScope));
        }
        List<JavaScriptVariable> teavmVarList = new ArrayList<>(teavmVariables);
        Collections.sort(teavmVarList, new Comparator<JavaScriptVariable>() {
            @Override public int compare(JavaScriptVariable o1, JavaScriptVariable o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (int i = 0; i < teavmVarList.size(); ++i) {
            variables.add(new TeaVMJSVariable(debugTarget, teavmVarList.get(i)));
        }
        return variables.toArray(new TeaVMVariable[0]);
    }
}
