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

import org.teavm.extension.bridge.BridgeGenericArray;
import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.bridge.BridgeType;
import org.teavm.extension.introspect.IntrospectGenericArray;

class HostGenericArray extends HostType implements BridgeGenericArray {
    final IntrospectGenericArray array;

    HostGenericArray(HostEnvironment env, IntrospectGenericArray array) {
        super(env, array, BridgeKinds.GENERIC_ARRAY);
        this.array = array;
    }

    @Override
    public BridgeType componentType() {
        return env.type(array.componentType());
    }
}
