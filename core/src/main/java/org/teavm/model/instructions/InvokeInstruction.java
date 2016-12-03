/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.model.instructions;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Variable;

public class InvokeInstruction extends Instruction {
    private InvocationType type;
    private MethodReference method;
    private Variable instance;
    private List<Variable> arguments = new ArrayList<>();
    private Variable receiver;

    public InvocationType getType() {
        return type;
    }

    public void setType(InvocationType type) {
        this.type = type;
    }

    public Variable getInstance() {
        return instance;
    }

    public void setInstance(Variable instance) {
        this.instance = instance;
    }

    public List<Variable> getArguments() {
        return arguments;
    }

    public MethodReference getMethod() {
        return method;
    }

    public void setMethod(MethodReference method) {
        this.method = method;
    }

    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
