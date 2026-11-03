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

import org.teavm.extension.bridge.BridgeGenericDeclaration;
import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.bridge.BridgeType;
import org.teavm.extension.bridge.BridgeTypeVariable;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.extension.introspect.IntrospectTypeVariable;

class HostTypeVariable extends HostType implements BridgeTypeVariable {
    final IntrospectTypeVariable variable;

    HostTypeVariable(HostEnvironment env, IntrospectTypeVariable variable) {
        super(env, variable, BridgeKinds.TYPE_VARIABLE);
        this.variable = variable;
    }

    @Override
    public String name() {
        return variable.name();
    }

    @Override
    public BridgeType[] superTypes() {
        return env.types(variable.superTypes());
    }

    @Override
    public BridgeGenericDeclaration genericDeclaration() {
        var declaration = variable.genericDeclaration();
        if (declaration instanceof IntrospectMethod) {
            return env.method((IntrospectMethod) declaration);
        } else if (declaration instanceof IntrospectClass<?>) {
            return env.cls((IntrospectClass<?>) declaration);
        } else {
            return null;
        }
    }
}
