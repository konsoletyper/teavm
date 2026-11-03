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
import org.teavm.extension.bridge.BridgeParameterizedType;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectParameterizedType;
import org.teavm.extension.introspect.IntrospectType;
import org.teavm.extension.introspect.IntrospectTypeArgument;

class GuestParameterizedType extends GuestElement implements IntrospectParameterizedType {
    private final BridgeParameterizedType remoteType;
    private List<? extends IntrospectTypeArgument> typeArguments;

    GuestParameterizedType(GuestEnvironment env, BridgeParameterizedType remote) {
        super(env, remote);
        remoteType = remote;
    }

    @Override
    public IntrospectClass<?> rawType() {
        return (IntrospectClass<?>) env.element(remoteType.rawType());
    }

    @Override
    public IntrospectType ownerType() {
        return (IntrospectType) env.element(remoteType.ownerType());
    }

    @Override
    public List<? extends IntrospectTypeArgument> typeArguments() {
        if (typeArguments == null) {
            typeArguments = env.elements(remoteType.typeArguments());
        }
        return typeArguments;
    }
}
