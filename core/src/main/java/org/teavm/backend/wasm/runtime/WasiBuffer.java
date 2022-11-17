/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.runtime;

import org.teavm.backend.wasm.WasmHeap;
import org.teavm.interop.Address;

public final class WasiBuffer {
    private WasiBuffer() {
    }

    public static int getBufferSize() {
        return WasmHeap.bufferSize;
    }

    public static Address getBuffer() {
        return WasmHeap.buffer;
    }
}
