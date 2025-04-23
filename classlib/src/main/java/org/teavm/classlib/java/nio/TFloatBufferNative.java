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
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Float32Array;

class TFloatBufferNative extends TFloatBufferImpl implements TArrayBufferViewProvider {
    float[] array;
    boolean readOnly;
    @TNativeBufferObjectMarker
    protected final Object base;
    Address address;
    int capacity;
    boolean swap;

    TFloatBufferNative(float[] array, int position, int limit, boolean readOnly,
            Object base, Address address, int capacity, boolean swap) {
        super(position, limit);
        this.array = array;
        this.readOnly = readOnly;
        this.base = base;
        this.address = address;
        this.capacity = capacity;
        this.swap = swap;
    }

    @Override
    int capacityImpl() {
        return capacity;
    }

    @Override
    boolean isArrayPresent() {
        return array != null;
    }

    @Override
    float[] getArray() {
        if (array == null) {
            throw new UnsupportedOperationException();
        }
        return array;
    }

    @Override
    int getArrayOffset() {
        if (array == null) {
            throw new UnsupportedOperationException();
        }
        return (int) (address.diff(Address.ofData(array)) / 4);
    }

    @Override
    boolean readOnly() {
        return readOnly;
    }

    @Override
    public boolean isDirect() {
        return base == null;
    }

    @Override
    TFloatBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TFloatBufferNative(array, position, limit, readOnly, base, address.add(start * 4), capacity, swap);
    }

    @Override
    float getElement(int index) {
        var addr = address.add(index * 4);
        return swap ? Float.intBitsToFloat(Integer.reverseBytes(addr.getInt())) : addr.getFloat();
    }

    @Override
    void putElement(int index, float value) {
        var addr = address.add(index * 4);
        if (swap) {
            addr.putInt(Integer.reverseBytes(Float.floatToRawIntBits(value)));
        } else {
            addr.putFloat(value);
        }
    }

    @Override
    void getImpl(int index, float[] dst, int offset, int length) {
        var addr = address.add(index * 4);
        if (swap) {
            while (length-- > 0) {
                dst[offset++] = Float.intBitsToFloat(Integer.reverseBytes(addr.getInt()));
                addr = addr.add(4);
            }
        } else {
            copy(addr, dst, offset, length);
        }
    }

    @Override
    void putImpl(int index, float[] src, int offset, int length) {
        var addr = address.add(index * 4);
        if (swap) {
            while (length-- > 0) {
                addr.putInt(Integer.reverseBytes(Float.floatToRawIntBits(src[offset++])));
                addr = addr.add(4);
            }
        } else {
            copy(src, offset, addr, length);
        }
    }

    @Override
    void putImpl(int index, TFloatBuffer src, int offset, int length) {
        if (src instanceof TFloatBufferNative) {
            var srcImpl = (TFloatBufferNative) src;
            var addr = address.add(index * 4);
            if (srcImpl.swap == swap) {
                TByteBufferNative.copy(srcImpl.address.add(offset * 4), addr, length * 4);
            } else {
                var srcAddr = srcImpl.address.add(offset * 4);
                while (length-- > 0) {
                    addr.putInt(Integer.reverseBytes(Float.floatToRawIntBits(srcAddr.getFloat())));
                    addr = addr.add(4);
                    srcAddr = srcAddr.add(4);
                }
            }
        } else {
            var addr = address.add(index * 4);
            if (swap) {
                while (length-- > 0) {
                    addr.putInt(Integer.reverseBytes(Float.floatToIntBits(src.get(offset++))));
                    addr = addr.add(4);
                }
            } else {
                while (length-- > 0) {
                    addr.putFloat(src.get(offset++));
                    addr = addr.add(4);
                }
            }
        }
    }

    @Override
    public TByteOrder order() {
        return swap
                ? TByteBufferNative.oppositeOrder(TByteOrder.nativeOrder())
                : TByteOrder.nativeOrder();
    }

    @Override
    public ArrayBufferView getArrayBufferView() {
        return new Float32Array(TJSBufferHelper.WasmGC.getLinearMemory(), address.toInt(), capacity);
    }

    @Override
    public int elementSize() {
        return 4;
    }

    void copy(float[] from, int fromOffset, Address to, int count) {
        TByteBufferNative.copy(Address.ofData(from).add(fromOffset * 4), to, count * 4);
    }

    void copy(Address from, float[] to, int toOffset, int count) {
        TByteBufferNative.copy(from, Address.ofData(to).add(toOffset * 4), count * 4);
    }
}
