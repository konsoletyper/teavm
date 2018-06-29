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
package org.teavm.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodReference;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.emit.ValueEmitter;

public class DynamicCallSite {
    private MethodReference caller;
    private MethodDescriptor calledMethod;
    private ValueEmitter instance;
    private List<ValueEmitter> arguments;
    private MethodHandle bootstrapMethod;
    private List<RuntimeConstant> bootstrapArguments;
    private DependencyAgent agent;

    DynamicCallSite(MethodReference caller, MethodDescriptor calledMethod, ValueEmitter instance,
            List<ValueEmitter> arguments, MethodHandle bootstrapMethod, List<RuntimeConstant> bootstrapArguments,
            DependencyAgent agent) {
        this.caller = caller;
        this.calledMethod = calledMethod;
        this.instance = instance;
        this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
        this.bootstrapMethod = bootstrapMethod;
        this.bootstrapArguments = Collections.unmodifiableList(new ArrayList<>(bootstrapArguments));
        this.agent = agent;
    }

    public MethodReference getCaller() {
        return caller;
    }

    public MethodDescriptor getCalledMethod() {
        return calledMethod;
    }

    public MethodHandle getBootstrapMethod() {
        return bootstrapMethod;
    }

    public List<ValueEmitter> getArguments() {
        return arguments;
    }

    public List<RuntimeConstant> getBootstrapArguments() {
        return bootstrapArguments;
    }

    public DependencyAgent getAgent() {
        return agent;
    }

    public ValueEmitter getInstance() {
        return instance;
    }
}
