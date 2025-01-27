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

class TDoubleBufferNative extends TDoubleBufferImpl {
    private double[] array;
    private boolean readOnly;
    @TNativeBufferObjectMarker
    protected final Object base;
    private final Address address;
    private int capacity;
    private boolean swap;

    TDoubleBufferNative(double[] array, int position, int limit, boolean readOnly,
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
    double[] getArray() {
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
    TDoubleBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TDoubleBufferNative(array, position, limit, readOnly, base, address.add(start * 8), capacity, swap);
    }

    @Override
    double getElement(int index) {
        var addr = address.add(index * 8);
        return swap ? Double.longBitsToDouble(Long.reverseBytes(addr.getLong())) : addr.getDouble();
    }

    @Override
    void putElement(int index, double value) {
        var addr = address.add(index * 8);
        if (swap) {
            addr.putLong(Long.reverseBytes(Double.doubleToRawLongBits(value)));
        } else {
            addr.putDouble(value);
        }
    }

    @Override
    void getImpl(int index, double[] dst, int offset, int length) {
        var addr = address.add(index * 8);
        if (swap) {
            while (length-- > 0) {
                dst[offset++] = Double.longBitsToDouble(Long.reverseBytes(addr.getLong()));
                addr = addr.add(8);
            }
        } else {
            TByteBufferNative.copy(addr, Address.ofData(dst).add(offset * 8), length * 8);
        }
    }

    @Override
    void putImpl(int index, double[] src, int offset, int length) {
        var addr = address.add(index * 8);
        if (swap) {
            while (length-- > 0) {
                addr.putLong(Long.reverseBytes(Double.doubleToRawLongBits(src[offset++])));
                addr = addr.add(8);
            }
        } else {
            TByteBufferNative.copy(Address.ofData(src).add(offset * 8), addr, length * 8);
        }
    }

    @Override
    void putImpl(int index, TDoubleBuffer src, int offset, int length) {
        if (src instanceof TDoubleBufferNative) {
            var srcImpl = (TDoubleBufferNative) src;
            var addr = address.add(index * 8);
            if (srcImpl.swap == swap) {
                TByteBufferNative.copy(srcImpl.address.add(offset * 8), addr, length * 8);
            } else {
                var srcAddr = srcImpl.address.add(offset * 8);
                while (length-- > 0) {
                    addr.putLong(Long.reverseBytes(Double.doubleToRawLongBits(srcAddr.getDouble())));
                    addr = addr.add(8);
                    srcAddr = srcAddr.add(8);
                }
            }
        } else {
            var addr = address.add(index * 8);
            if (swap) {
                while (length-- > 0) {
                    addr.putLong(Long.reverseBytes(Double.doubleToRawLongBits(src.get(offset++))));
                    addr = addr.add(8);
                }
            } else {
                while (length-- > 0) {
                    addr.putDouble(src.get(offset++));
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
