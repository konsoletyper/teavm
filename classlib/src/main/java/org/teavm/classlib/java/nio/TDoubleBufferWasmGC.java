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

class TDoubleBufferWasmGC extends TDoubleBufferNative {
    TDoubleBufferWasmGC(double[] array, int position, int limit, boolean readOnly, Object base, Address address,
            int capacity, boolean swap) {
        super(array, position, limit, readOnly, base, address, capacity, swap);
    }

    @Override
    void copy(double[] from, int fromOffset, Address to, int count) {
        for (var i = 0; i < count; ++i) {
            to.putDouble(from[fromOffset++]);
            to = to.add(8);
        }
    }

    @Override
    void copy(Address from, double[] to, int toOffset, int count) {
        for (var i = 0; i < count; ++i) {
            to[toOffset++] = from.getDouble();
            from = from.add(8);
        }
    }

    @Override
    TDoubleBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TDoubleBufferWasmGC(array, position, limit, readOnly, base, address.add(start * 8), capacity, swap);
    }

    @Override
    int getArrayOffset() {
        throw new UnsupportedOperationException();
    }
}
