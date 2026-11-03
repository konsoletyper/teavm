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

import org.teavm.extension.bridge.BridgeField;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectField;
import org.teavm.extension.introspect.IntrospectType;

class GuestField extends GuestMember implements IntrospectField {
    final BridgeField remoteField;
    private IntrospectClass<?> type;

    GuestField(GuestEnvironment env, BridgeField remote) {
        super(env, remote);
        remoteField = remote;
    }

    @Override
    public boolean isEnumConstant() {
        return remoteField.isEnumConstant();
    }

    @Override
    public IntrospectClass<?> type() {
        if (type == null) {
            type = (IntrospectClass<?>) env.element(remoteField.type());
        }
        return type;
    }

    @Override
    public IntrospectType genericType() {
        return (IntrospectType) env.element(remoteField.genericType());
    }
}
