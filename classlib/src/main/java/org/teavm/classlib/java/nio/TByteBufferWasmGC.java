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
import org.teavm.jso.JSObject;

class TByteBufferWasmGC extends TByteBufferNative {
    JSObject regKey;
    JSObject regToken;

    TByteBufferWasmGC(Object gcRef, byte[] array, int arrayOffset, Object base, Address address, int capacity,
            boolean readOnly) {
        super(gcRef, array, arrayOffset, base, address, capacity, readOnly);
    }

    @Override
    public void release() {
        if (regKey != null) {
            TJSBufferHelper.WasmGC.unregister(regToken);
            regKey = null;
            regToken = null;
        }
        super.release();
    }

    @Override
    void copy(byte[] from, int fromOffset, Address to, int count) {
        for (var i = 0; i < count; ++i) {
            to.putByte(from[fromOffset++]);
            to = to.add(1);
        }
    }

    @Override
    void copy(Address from, byte[] to, int toOffset, int count) {
        for (var i = 0; i < count; ++i) {
            to[toOffset++] = from.getByte();
            from = from.add(1);
        }
    }

    @Override
    public TByteBuffer slice() {
        var newData = address.add(position);
        var result = new TByteBufferWasmGC(gcRef, array, arrayOffset + position, base, newData, remaining(), readOnly);
        result.position = 0;
        result.limit = result.capacity();
        result.order = TByteOrder.BIG_ENDIAN;
        return result;
    }

    @Override
    public TByteBuffer duplicate() {
        var result = new TByteBufferWasmGC(gcRef, array, arrayOffset + position, base, address, capacity, readOnly);
        result.position = position;
        result.limit = limit;
        result.mark = mark;
        result.order = TByteOrder.BIG_ENDIAN;
        return result;
    }

    @Override
    public TByteBuffer asReadOnlyBuffer() {
        var result = new TByteBufferWasmGC(gcRef, array, arrayOffset + position, base, address, capacity, true);
        result.position = position;
        result.limit = limit;
        result.mark = mark;
        result.order = TByteOrder.BIG_ENDIAN;
        return result;
    }

    @Override
    public TCharBuffer asCharBuffer() {
        int sz = remaining() / 2;
        return new TCharBufferWasmGC(gcRef, null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }

    @Override
    public TShortBuffer asShortBuffer() {
        int sz = remaining() / 2;
        return new TShortBufferWasmGC(gcRef, null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }

    @Override
    public TIntBuffer asIntBuffer() {
        int sz = remaining() / 4;
        return new TIntBufferWasmGC(gcRef, null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }

    @Override
    public TLongBuffer asLongBuffer() {
        int sz = remaining() / 8;
        return new TLongBufferWasmGC(gcRef, null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }

    @Override
    public TFloatBuffer asFloatBuffer() {
        int sz = remaining() / 4;
        return new TFloatBufferWasmGC(gcRef, null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }

    @Override
    public TDoubleBuffer asDoubleBuffer() {
        int sz = remaining() / 8;
        return new TDoubleBufferWasmGC(gcRef, null, 0, sz, readOnly, base, address.add(position), sz, swap);
    }
}
