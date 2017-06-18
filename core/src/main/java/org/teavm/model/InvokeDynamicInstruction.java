/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.model;

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.instructions.InstructionVisitor;

public class InvokeDynamicInstruction extends Instruction {
    private MethodDescriptor method;
    private MethodHandle bootstrapMethod;
    private List<RuntimeConstant> bootstrapArguments = new ArrayList<>();
    private Variable instance;
    private List<Variable> arguments = new ArrayList<>();
    private Variable receiver;

    public MethodDescriptor getMethod() {
        return method;
    }

    public void setMethod(MethodDescriptor method) {
        this.method = method;
    }

    public MethodHandle getBootstrapMethod() {
        return bootstrapMethod;
    }

    public void setBootstrapMethod(MethodHandle bootstrapMethod) {
        this.bootstrapMethod = bootstrapMethod;
    }

    public Variable getReceiver() {
        return receiver;
    }

    public void setReceiver(Variable receiver) {
        this.receiver = receiver;
    }

    public List<RuntimeConstant> getBootstrapArguments() {
        return bootstrapArguments;
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

    @Override
    public void acceptVisitor(InstructionVisitor visitor) {
        visitor.visit(this);
    }
}
