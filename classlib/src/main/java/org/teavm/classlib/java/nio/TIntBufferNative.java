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

class TIntBufferNative extends TIntBufferImpl {
    private int[] array;
    private boolean readOnly;
    @TNativeBufferObjectMarker
    private final Object base;
    private Address address;
    private int capacity;
    private boolean swap;

    TIntBufferNative(int[] array, int position, int limit, boolean readOnly,
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
        return new TIntBufferNative(array, position, limit, readOnly, base, address.add(start * 4), capacity, swap);
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
            TByteBufferNative.copy(addr, Address.ofData(dst).add(offset * 4), length * 4);
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
            TByteBufferNative.copy(Address.ofData(src).add(offset * 4), addr, length * 4);
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
}
