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

import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.bridge.BridgeTypeArgument;
import org.teavm.extension.introspect.IntrospectProjection;
import org.teavm.extension.introspect.IntrospectType;
import org.teavm.extension.introspect.IntrospectTypeArgument;

class GuestTypeArgument extends GuestElement implements IntrospectTypeArgument {
    private final BridgeTypeArgument remoteArgument;

    GuestTypeArgument(GuestEnvironment env, BridgeTypeArgument remote) {
        super(env, remote);
        remoteArgument = remote;
    }

    @Override
    public IntrospectProjection projection() {
        switch (remoteArgument.projection()) {
            case BridgeKinds.PROJECTION_EXTENDS:
                return IntrospectProjection.EXTENDS;
            case BridgeKinds.PROJECTION_SUPER:
                return IntrospectProjection.SUPER;
            default:
                return IntrospectProjection.EXACT;
        }
    }

    @Override
    public IntrospectType type() {
        return (IntrospectType) env.element(remoteArgument.type());
    }
}
