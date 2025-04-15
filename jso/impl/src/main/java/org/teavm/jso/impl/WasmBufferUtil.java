/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.jso.impl;

import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.runtime.heap.Heap;

final class WasmBufferUtil {
    static final int BUFFER_SIZE = 4096;
    static final Address buffer;

    private WasmBufferUtil() {
    }

    static {
        buffer = Heap.alloc(BUFFER_SIZE);
        if (buffer.toInt() == 0) {
            throw new RuntimeException("Could not initialize buffer");
        }
    }


    @Import(module = "teavm", name = "linearMemory")
    static native ArrayBuffer getLinearMemory();
}
