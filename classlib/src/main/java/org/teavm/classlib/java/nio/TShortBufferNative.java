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
import org.teavm.jso.typedarrays.Int16Array;

class TShortBufferNative extends TShortBufferImpl implements TArrayBufferViewProvider {
    short[] array;
    boolean readOnly;
    @TNativeBufferObjectMarker
    protected final Object base;
    Address address;
    int capacity;
    boolean swap;

    TShortBufferNative(short[] array, int position, int limit, boolean readOnly,
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
    short[] getArray() {
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
        return (int) (address.diff(Address.ofData(array)) / 2);
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
    TShortBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TShortBufferNative(array, position, limit, readOnly, base, address.add(start * 2), capacity, swap);
    }

    @Override
    short getElement(int index) {
        var result = address.add(index * 2).getShort();
        return swap ? Short.reverseBytes(result) : result;
    }

    @Override
    void putElement(int index, short value) {
        address.add(index * 2).putShort(swap ? Short.reverseBytes(value) : value);
    }

    @Override
    void getImpl(int index, short[] dst, int offset, int length) {
        var addr = address.add(index * 2);
        if (swap) {
            while (length-- > 0) {
                dst[offset++] = Short.reverseBytes(addr.getShort());
                addr = addr.add(2);
            }
        } else {
            copy(addr, dst, offset, length);
        }
    }

    @Override
    void putImpl(int index, short[] src, int offset, int length) {
        var addr = address.add(index * 2);
        if (swap) {
            while (length-- > 0) {
                addr.putShort(Short.reverseBytes(src[offset++]));
                addr = addr.add(2);
            }
        } else {
            copy(src, offset, addr, length);
        }
    }

    @Override
    void putImpl(int index, TShortBuffer src, int offset, int length) {
        if (src instanceof TShortBufferNative) {
            var srcImpl = (TShortBufferNative) src;
            var addr = address.add(index * 2);
            if (srcImpl.swap == swap) {
                TByteBufferNative.copy(srcImpl.address.add(offset * 2), addr, length * 2);
            } else {
                var srcAddr = srcImpl.address.add(offset * 2);
                while (length-- > 0) {
                    addr.putShort(Short.reverseBytes(srcAddr.getShort()));
                    addr = addr.add(2);
                    srcAddr = srcAddr.add(2);
                }
            }
        } else {
            var addr = address.add(index * 2);
            if (swap) {
                while (length-- > 0) {
                    addr.putShort(Short.reverseBytes(src.get(offset++)));
                    addr = addr.add(2);
                }
            } else {
                while (length-- > 0) {
                    addr.putShort(src.get(offset++));
                    addr = addr.add(2);
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
        return new Int16Array(TJSBufferHelper.WasmGC.getLinearMemory(), address.toInt(), capacity);
    }

    @Override
    public int elementSize() {
        return 2;
    }

    void copy(short[] from, int fromOffset, Address to, int count) {
        TByteBufferNative.copy(Address.ofData(from).add(fromOffset * 2), to, count * 2);
    }

    void copy(Address from, short[] to, int toOffset, int count) {
        TByteBufferNative.copy(from, Address.ofData(to).add(toOffset * 2), count * 2);
    }
}
