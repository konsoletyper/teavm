/*
 *  Copyright 2025 Maksim Tiushev.
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
package org.teavm.backend.wasm.runtime.net.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.teavm.interop.Address;

public class AddrInfoHints {
    private static final int BUFFER_SIZE = 12;
    private final int type;
    private final int family;
    private final int hintsEnabled;
    private final ByteBuffer buffer;

    public AddrInfoHints(int type, int family, int hintsEnabled) {
        this.type = type;
        this.family = family;
        this.hintsEnabled = hintsEnabled;
        this.buffer = createBuffer(type, family, hintsEnabled);
    }

    private static ByteBuffer createBuffer(int type, int family, int hintsEnabled) {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.order(ByteOrder.nativeOrder());
        buffer.putInt(type);
        buffer.putInt(family);
        buffer.putInt(hintsEnabled);
        return buffer;
    }

    public int getType() {
        return type;
    }

    public int getFamily() {
        return family;
    }

    public int getHintsEnabled() {
        return hintsEnabled;
    }

    public Address getAddress() {
        return Address.ofData(buffer.array());
    }

    @Override
    public String toString() {
        return String.format(
                "AddrInfoHints{type=%d, family=%d, hintsEnabled=%d}", type, family, hintsEnabled);
    }
}
