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
package org.teavm.classlib.java.nio;

import org.teavm.interop.Address;
import org.teavm.jso.core.JSFinalizationRegistry;
import org.teavm.jso.core.JSNumber;
import org.teavm.runtime.heap.Heap;

final class TBuffersCleaner {
    private static final JSFinalizationRegistry registry = new JSFinalizationRegistry(address -> {
        var addr = Address.fromInt(((JSNumber) address).intValue());
        Heap.release(addr);
    });

    private TBuffersCleaner() {
    }

    static void register(Object object, Address address) {
        registry.register(object, JSNumber.valueOf(address.toInt()));
    }
}
