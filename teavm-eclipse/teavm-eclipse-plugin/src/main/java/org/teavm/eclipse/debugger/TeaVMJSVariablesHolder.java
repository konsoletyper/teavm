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
import java.util.concurrent.atomic.AtomicReference;
import org.teavm.debugging.javascript.JavaScriptVariable;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMJSVariablesHolder {
    private TeaVMDebugTarget debugTarget;
    private Collection<JavaScriptVariable> teavmVariables;
    private AtomicReference<TeaVMJSVariable[]> variables = new AtomicReference<>();

    public TeaVMJSVariablesHolder(TeaVMDebugTarget debugTarget, Collection<JavaScriptVariable> teavmVariables) {
        this.debugTarget = debugTarget;
        this.teavmVariables = teavmVariables;
    }

    public TeaVMJSVariable[] getVariables() {
        if (variables.get() == null) {
            TeaVMJSVariable[] newVariables = new TeaVMJSVariable[teavmVariables.size()];
            List<JavaScriptVariable> teavmVarList = new ArrayList<>(teavmVariables);
            Collections.sort(teavmVarList, new Comparator<JavaScriptVariable>() {
                @Override public int compare(JavaScriptVariable o1, JavaScriptVariable o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            for (int i = 0; i < teavmVarList.size(); ++i) {
                newVariables[i] = new TeaVMJSVariable(debugTarget, teavmVarList.get(i));
            }
            variables.compareAndSet(null, newVariables);
        }
        return variables.get();
    }
}
