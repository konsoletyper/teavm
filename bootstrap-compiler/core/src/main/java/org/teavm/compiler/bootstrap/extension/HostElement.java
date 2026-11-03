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

import org.teavm.extension.bridge.BridgeElement;

/**
 * Base class for host elements. Note that it's important that this class and its subclasses are not abstract,
 * otherwise JSO would consider them JavaScript classes.
 */
class HostElement implements BridgeElement {
    final HostEnvironment env;
    private final Object underlying;
    private final int kind;

    HostElement(HostEnvironment env, Object underlying, int kind) {
        this.env = env;
        this.underlying = underlying;
        this.kind = kind;
    }

    final Object underlying() {
        return underlying;
    }

    @Override
    public final int kind() {
        return kind;
    }

    @Override
    public String describe() {
        return String.valueOf(underlying);
    }
}
