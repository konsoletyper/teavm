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

class TShortBufferWasmGC extends TShortBufferNative {
    TShortBufferWasmGC(Object gcRef, short[] array, int position, int limit, boolean readOnly, Object base,
            Address address, int capacity, boolean swap) {
        super(gcRef, array, position, limit, readOnly, base, address, capacity, swap);
    }

    @Override
    void copy(short[] from, int fromOffset, Address to, int count) {
        for (var i = 0; i < count; ++i) {
            to.putShort(from[fromOffset++]);
            to = to.add(2);
        }
    }

    @Override
    void copy(Address from, short[] to, int toOffset, int count) {
        for (var i = 0; i < count; ++i) {
            to[toOffset++] = from.getShort();
            from = from.add(2);
        }
    }

    @Override
    TShortBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TShortBufferWasmGC(gcRef, array, position, limit, readOnly, base, address.add(start * 2),
                capacity, swap);
    }

    @Override
    int getArrayOffset() {
        throw new UnsupportedOperationException();
    }
}
