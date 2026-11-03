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

import org.teavm.extension.bridge.BridgeMember;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectMember;

abstract class GuestMember extends GuestAnnotatedElement implements IntrospectMember {
    final BridgeMember remoteMember;
    private String name;
    private int modifiers = -1;
    private IntrospectClass<?> declaringClass;

    GuestMember(GuestEnvironment env, BridgeMember remote) {
        super(env, remote);
        remoteMember = remote;
    }

    @Override
    public String name() {
        if (name == null) {
            name = remoteMember.name();
        }
        return name;
    }

    @Override
    public int modifiers() {
        if (modifiers < 0) {
            modifiers = remoteMember.modifiers();
        }
        return modifiers;
    }

    @Override
    public IntrospectClass<?> declaringClass() {
        if (declaringClass == null) {
            declaringClass = (IntrospectClass<?>) env.element(remoteMember.declaringClass());
        }
        return declaringClass;
    }
}
