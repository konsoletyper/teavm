/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.expr;

import java.util.ArrayList;
import java.util.List;
import org.teavm.newir.type.IrType;

public final class IrProgram {
    private IrParameter[] parameters;
    private IrExpr body = IrExpr.VOID;
    private List<IrVariable> variables = new ArrayList<>();

    public IrProgram(IrType... parameterTypes) {
        parameters = new IrParameter[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; ++i) {
            parameters[i] = new IrParameter(this, i, parameterTypes[i]);
        }
    }

    public int getParameterCount() {
        return parameters.length;
    }

    public IrParameter getParameter(int index) {
        return parameters[index];
    }

    public IrExpr getBody() {
        return body;
    }

    public void setBody(IrExpr body) {
        this.body = body;
    }

    public IrVariable addVariable(IrType type) {
        IrVariable variable = new IrVariable(this, variables.size(), type);
        variables.add(variable);
        return variable;
    }

    public void removeVariable(IrVariable variable) {
        if (variable.program != this) {
            throw new IllegalArgumentException("Variable does not belong to function");
        }
        variables.remove(variable.index);
        for (int i = variable.index; i < variables.size(); ++i) {
            variables.get(i).index = i;
        }
        variable.index = -1;
        variable.program = null;
    }

    public Iterable<IrVariable> getVariables() {
        return variables;
    }

    public int getVariableCount() {
        return variables.size();
    }

    public IrVariable getVariable(int index) {
        return variables.get(index);
    }
}
