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

import org.teavm.backend.wasm.runtime.WasiBuffer;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;

public class AddrInfoHints {
    private final int type;
    private final int family;
    private final int hintsEnabled;

    public static class AddrInfoHintsStruct extends Structure {
        public int type;
        public int family;
        public int hintsEnabled;
    }

    public AddrInfoHints(int type, int family, int hintsEnabled) {
        this.type = type;
        this.family = family;
        this.hintsEnabled = hintsEnabled;
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
        Address argsAddress = WasiBuffer.getBuffer();
        AddrInfoHintsStruct s = argsAddress.toStructure();
        s.type = type;
        s.family = family;
        s.hintsEnabled = hintsEnabled;
        return argsAddress;
    }

    @Override
    public String toString() {
        return "AddrInfoHints{type=" + type + ", family=" + family + ", hintsEnabled=" + hintsEnabled + "}";
    }
}
