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

import org.teavm.classlib.java.nio.file.TAddressBasedBuffer;
import org.teavm.interop.Address;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.BigInt64Array;

class TLongBufferNative extends TLongBufferImpl implements TArrayBufferViewProvider, TAddressBasedBuffer {
    Object gcRef;
    long[] array;
    boolean readOnly;
    @TNativeBufferObjectMarker
    protected final Object base;
    Address address;
    int capacity;
    boolean swap;

    TLongBufferNative(Object gcRef, long[] array, int position, int limit, boolean readOnly,
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
    public Address getDataAddress() {
        return address;
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
    long[] getArray() {
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
        return (int) (address.diff(Address.ofData(array)) / 8);
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
    TLongBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TLongBufferNative(gcRef, array, position, limit, readOnly, base, address.add(start * 8),
                capacity, swap);
    }

    @Override
    long getElement(int index) {
        var result = address.add(index * 8).getLong();
        return swap ? Long.reverseBytes(result) : result;
    }

    @Override
    void putElement(int index, long value) {
        address.add(index * 8).putLong(swap ? Long.reverseBytes(value) : value);
    }

    @Override
    void getImpl(int index, long[] dst, int offset, int length) {
        var addr = address.add(index * 8);
        if (swap) {
            while (length-- > 0) {
                dst[offset++] = Long.reverseBytes(addr.getLong());
                addr = addr.add(8);
            }
        } else {
            copy(addr, dst, offset, length);
        }
    }

    @Override
    void putImpl(int index, long[] src, int offset, int length) {
        var addr = address.add(index * 8);
        if (swap) {
            while (length-- > 0) {
                addr.putLong(Long.reverseBytes(src[offset++]));
                addr = addr.add(8);
            }
        } else {
            copy(src, offset, addr, length);
        }
    }

    @Override
    void putImpl(int index, TLongBuffer src, int offset, int length) {
        if (src instanceof TLongBufferNative) {
            var srcImpl = (TLongBufferNative) src;
            var addr = address.add(index * 8);
            if (srcImpl.swap == swap) {
                TByteBufferNative.copy(srcImpl.address.add(offset * 8), addr, length * 8);
            } else {
                var srcAddr = srcImpl.address.add(offset * 8);
                while (length-- > 0) {
                    addr.putLong(Long.reverseBytes(srcAddr.getLong()));
                    addr = addr.add(8);
                    srcAddr = srcAddr.add(8);
                }
            }
        } else {
            var addr = address.add(index * 8);
            if (swap) {
                while (length-- > 0) {
                    addr.putLong(Long.reverseBytes(src.get(offset++)));
                    addr = addr.add(8);
                }
            } else {
                while (length-- > 0) {
                    addr.putLong(src.get(offset++));
                    addr = addr.add(8);
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
        return new BigInt64Array(TJSBufferHelper.WasmGC.getLinearMemory(), address.toInt(), capacity);
    }

    @Override
    public int elementSize() {
        return 8;
    }

    void copy(long[] from, int fromOffset, Address to, int count) {
        TByteBufferNative.copy(Address.ofData(from).add(fromOffset * 8), to, count * 8);
    }

    void copy(Address from, long[] to, int toOffset, int count) {
        TByteBufferNative.copy(from, Address.ofData(to).add(toOffset * 8), count * 8);
    }
}
