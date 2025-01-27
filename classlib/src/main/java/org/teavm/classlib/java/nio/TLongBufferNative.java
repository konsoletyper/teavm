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

class TLongBufferNative extends TLongBufferImpl {
    private long[] array;
    private boolean readOnly;
    @TNativeBufferObjectMarker
    protected final Object base;
    private final Address address;
    private int capacity;
    private boolean swap;

    TLongBufferNative(long[] array, int position, int limit, boolean readOnly,
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
        return new TLongBufferNative(array, position, limit, readOnly, base, address.add(start * 8), capacity, swap);
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
            TByteBufferNative.copy(addr, Address.ofData(dst).add(offset * 8), length * 8);
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
            TByteBufferNative.copy(Address.ofData(src).add(offset * 8), addr, length * 8);
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
}
