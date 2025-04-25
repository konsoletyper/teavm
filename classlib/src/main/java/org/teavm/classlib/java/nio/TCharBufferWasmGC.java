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

class TCharBufferWasmGC extends TCharBufferNative {
    TCharBufferWasmGC(Object gcRef, char[] array, int position, int limit, boolean readOnly, Object base,
            Address address, int capacity, boolean swap) {
        super(gcRef, array, position, limit, readOnly, base, address, capacity, swap);
    }

    @Override
    void copy(char[] from, int fromOffset, Address to, int count) {
        for (var i = 0; i < count; ++i) {
            to.putChar(from[fromOffset++]);
            to = to.add(2);
        }
    }

    @Override
    void copy(Address from, char[] to, int toOffset, int count) {
        for (var i = 0; i < count; ++i) {
            to[toOffset++] = from.getChar();
            from = from.add(2);
        }
    }

    @Override
    TCharBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TCharBufferWasmGC(gcRef, array, position, limit, readOnly, base, address.add(start * 2),
                capacity, swap);
    }

    void copy(double[] from, int fromOffset, Address to, int count) {
        TByteBufferNative.copy(Address.ofData(from).add(fromOffset * 8), to, count * 8);
    }

    void copy(Address from, double[] to, int toOffset, int count) {
        TByteBufferNative.copy(from, Address.ofData(to).add(toOffset * 8), count * 8);
    }

    @Override
    int getArrayOffset() {
        throw new UnsupportedOperationException();
    }
}
