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
import org.teavm.extension.bridge.BridgeParameterizedType;
import org.teavm.extension.bridge.BridgeType;
import org.teavm.extension.bridge.BridgeTypeArgument;
import org.teavm.extension.introspect.IntrospectParameterizedType;

class HostParameterizedType extends HostType implements BridgeParameterizedType {
    final IntrospectParameterizedType type;

    HostParameterizedType(HostEnvironment env, IntrospectParameterizedType type) {
        super(env, type, BridgeKinds.PARAMETERIZED_TYPE);
        this.type = type;
    }

    @Override
    public BridgeClass rawType() {
        return env.cls(type.rawType());
    }

    @Override
    public BridgeType ownerType() {
        return env.type(type.ownerType());
    }

    @Override
    public BridgeTypeArgument[] typeArguments() {
        var arguments = type.typeArguments();
        var result = new HostTypeArgument[arguments.size()];
        for (var i = 0; i < result.length; ++i) {
            result[i] = env.typeArgument(arguments.get(i));
        }
        return result;
    }
}
