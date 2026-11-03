/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.compiler.bootstrap.extension;

import org.teavm.extension.bridge.BridgeClass;
import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.bridge.BridgeMethod;
import org.teavm.extension.bridge.BridgeParameter;
import org.teavm.extension.bridge.BridgeTypeVariable;
import org.teavm.extension.introspect.IntrospectMethod;

class HostMethod extends HostMember implements BridgeMethod {
    final IntrospectMethod method;

    HostMethod(HostEnvironment env, IntrospectMethod method) {
        super(env, method, BridgeKinds.METHOD);
        this.method = method;
    }

    @Override
    public BridgeClass returnType() {
        return env.cls(method.returnType());
    }

    @Override
    public BridgeParameter[] parameters() {
        var parameters = method.parameters();
        var result = new HostParameter[parameters.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = env.parameter(parameters.get(i));
        }
        return result;
    }

    @Override
    public BridgeClass[] exceptionTypes() {
        return env.classes(method.exceptionTypes());
    }

    @Override
    public BridgeTypeVariable[] typeParameters() {
        return env.typeVariables(method.typeParameters());
    }
}
