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
import org.teavm.extension.bridge.BridgeTypeVariable;
import org.teavm.extension.introspect.IntrospectGenericDeclaration;
import org.teavm.extension.introspect.IntrospectType;
import org.teavm.extension.introspect.IntrospectTypeVariable;

class GuestTypeVariable extends GuestElement implements IntrospectTypeVariable {
    private final BridgeTypeVariable remoteVariable;
    private String name;
    private List<? extends IntrospectType> superTypes;

    GuestTypeVariable(GuestEnvironment env, BridgeTypeVariable remote) {
        super(env, remote);
        remoteVariable = remote;
    }

    @Override
    public String name() {
        if (name == null) {
            name = remoteVariable.name();
        }
        return name;
    }

    @Override
    public List<? extends IntrospectType> superTypes() {
        if (superTypes == null) {
            superTypes = env.elements(remoteVariable.superTypes());
        }
        return superTypes;
    }

    @Override
    public IntrospectGenericDeclaration genericDeclaration() {
        return (IntrospectGenericDeclaration) env.element(remoteVariable.genericDeclaration());
    }
}
