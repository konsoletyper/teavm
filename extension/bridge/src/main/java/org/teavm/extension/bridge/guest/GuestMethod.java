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
package org.teavm.extension.bridge.guest;

import java.util.List;
import org.teavm.extension.bridge.BridgeMethod;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.extension.introspect.IntrospectParameter;
import org.teavm.extension.introspect.IntrospectTypeVariable;

class GuestMethod extends GuestMember implements IntrospectMethod {
    final BridgeMethod remoteMethod;
    private IntrospectClass<?> returnType;
    private List<? extends IntrospectParameter> parameters;
    private List<? extends IntrospectClass<? extends Throwable>> exceptionTypes;
    private List<? extends IntrospectTypeVariable> typeParameters;

    GuestMethod(GuestEnvironment env, BridgeMethod remote) {
        super(env, remote);
        remoteMethod = remote;
    }

    @Override
    public boolean isConstructor() {
        return name().equals("<init>");
    }

    @Override
    public IntrospectClass<?> returnType() {
        if (returnType == null) {
            returnType = (IntrospectClass<?>) env.element(remoteMethod.returnType());
        }
        return returnType;
    }

    @Override
    public List<? extends IntrospectParameter> parameters() {
        if (parameters == null) {
            parameters = env.elements(remoteMethod.parameters());
        }
        return parameters;
    }

    @Override
    public List<? extends IntrospectClass<? extends Throwable>> exceptionTypes() {
        if (exceptionTypes == null) {
            exceptionTypes = env.elements(remoteMethod.exceptionTypes());
        }
        return exceptionTypes;
    }

    @Override
    public List<? extends IntrospectTypeVariable> typeParameters() {
        if (typeParameters == null) {
            typeParameters = env.elements(remoteMethod.typeParameters());
        }
        return typeParameters;
    }
}
