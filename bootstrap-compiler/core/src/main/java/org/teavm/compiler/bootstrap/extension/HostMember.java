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
import org.teavm.extension.bridge.BridgeMember;
import org.teavm.extension.introspect.IntrospectMember;

class HostMember extends HostAnnotated implements BridgeMember {
    HostMember(HostEnvironment env, IntrospectMember member, int kind) {
        super(env, member, kind);
    }

    final IntrospectMember member() {
        return (IntrospectMember) underlying();
    }

    @Override
    public String name() {
        return member().name();
    }

    @Override
    public int modifiers() {
        return member().modifiers();
    }

    @Override
    public BridgeClass declaringClass() {
        return env.cls(member().declaringClass());
    }
}
