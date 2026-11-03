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
import org.teavm.extension.bridge.BridgeField;
import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.bridge.BridgeType;
import org.teavm.extension.introspect.IntrospectField;

class HostField extends HostMember implements BridgeField {
    final IntrospectField field;

    HostField(HostEnvironment env, IntrospectField field) {
        super(env, field, BridgeKinds.FIELD);
        this.field = field;
    }

    @Override
    public boolean isEnumConstant() {
        return field.isEnumConstant();
    }

    @Override
    public BridgeClass type() {
        return env.cls(field.type());
    }

    @Override
    public BridgeType genericType() {
        return env.type(field.genericType());
    }
}
