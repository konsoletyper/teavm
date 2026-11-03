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

import org.teavm.extension.bridge.BridgeKinds;
import org.teavm.extension.bridge.BridgeType;
import org.teavm.extension.bridge.BridgeTypeArgument;
import org.teavm.extension.introspect.IntrospectTypeArgument;

class HostTypeArgument extends HostElement implements BridgeTypeArgument {
    final IntrospectTypeArgument argument;

    HostTypeArgument(HostEnvironment env, IntrospectTypeArgument argument) {
        super(env, argument, BridgeKinds.TYPE_ARGUMENT);
        this.argument = argument;
    }

    @Override
    public int projection() {
        switch (argument.projection()) {
            case EXTENDS:
                return BridgeKinds.PROJECTION_EXTENDS;
            case SUPER:
                return BridgeKinds.PROJECTION_SUPER;
            default:
                return BridgeKinds.PROJECTION_EXACT;
        }
    }

    @Override
    public BridgeType type() {
        return env.type(argument.type());
    }
}
