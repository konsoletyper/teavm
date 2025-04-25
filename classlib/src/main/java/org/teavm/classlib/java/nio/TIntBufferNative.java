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
import org.teavm.jso.typedarrays.Int32Array;

class TIntBufferNative extends TIntBufferImpl implements TArrayBufferViewProvider {
    Object gcRef;
    int[] array;
    boolean readOnly;
    @TNativeBufferObjectMarker
    final Object base;
    Address address;
    int capacity;
    boolean swap;

    TIntBufferNative(Object gcRef, int[] array, int position, int limit, boolean readOnly,
            Object base, Address address, int capacity, boolean swap) {
        super(position, limit);
        this.gcRef = gcRef;
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
    int[] getArray() {
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
    TIntBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TIntBufferNative(gcRef, array, position, limit, readOnly, base, address.add(start * 4),
                capacity, swap);
    }

    @Override
    int getElement(int index) {
        var result = address.add(index * 4).getInt();
        return swap ? Integer.reverseBytes(result) : result;
    }

    @Override
    void putElement(int index, int value) {
        address.add(index * 4).putInt(swap ? Integer.reverseBytes(value) : value);
    }

    @Override
    void getImpl(int index, int[] dst, int offset, int length) {
        var addr = address.add(index * 4);
        if (swap) {
            while (length-- > 0) {
                dst[offset++] = Integer.reverseBytes(addr.getInt());
                addr = addr.add(4);
            }
        } else {
            copy(addr, dst, offset, length);
        }
    }

    @Override
    void putImpl(int index, int[] src, int offset, int length) {
        var addr = address.add(index * 4);
        if (swap) {
            while (length-- > 0) {
                addr.putInt(Integer.reverseBytes(src[offset++]));
                addr = addr.add(4);
            }
        } else {
            copy(src, offset, addr, length);
        }
    }

    @Override
    void putImpl(int index, TIntBuffer src, int offset, int length) {
        if (src instanceof TIntBufferNative) {
            var srcImpl = (TIntBufferNative) src;
            var addr = address.add(index * 4);
            if (srcImpl.swap == swap) {
                TByteBufferNative.copy(srcImpl.address.add(offset * 4), addr, length * 4);
            } else {
                var srcAddr = srcImpl.address.add(offset * 4);
                while (length-- > 0) {
                    addr.putInt(Integer.reverseBytes(srcAddr.getInt()));
                    addr = addr.add(4);
                    srcAddr = srcAddr.add(4);
                }
            }
        } else {
            if (swap) {
                var addr = address.add(index * 4);
                while (length-- > 0) {
                    addr.putInt(Integer.reverseBytes(src.get(offset++)));
                    addr = addr.add(4);
                }
            } else {
                var addr = address.add(index * 4);
                while (length-- > 0) {
                    addr.putInt(src.get(offset++));
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
        return new Int32Array(TJSBufferHelper.WasmGC.getLinearMemory(), address.toInt(), capacity);
    }

    @Override
    public int elementSize() {
        return 4;
    }

    void copy(int[] from, int fromOffset, Address to, int count) {
        TByteBufferNative.copy(Address.ofData(from).add(fromOffset * 4), to, count * 4);
    }

    void copy(Address from, int[] to, int toOffset, int count) {
        TByteBufferNative.copy(from, Address.ofData(to).add(toOffset * 4), count * 4);
    }
}
