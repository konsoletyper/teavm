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
import org.teavm.jso.typedarrays.Uint16Array;

class TCharBufferNative extends TCharBufferImpl implements TArrayBufferViewProvider {
    private char[] array;
    private boolean readOnly;
    @TNativeBufferObjectMarker
    private final Object base;
    private Address address;
    private int capacity;
    private boolean swap;

    TCharBufferNative(char[] array, int position, int limit, boolean readOnly,
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
    char[] getArray() {
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
    TCharBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TCharBufferNative(array, position, limit, readOnly, base, address.add(start * 2), capacity, swap);
    }

    @Override
    char getChar(int index) {
        var result = address.add(index * 2).getChar();
        return swap ? Character.reverseBytes(result) : result;
    }

    @Override
    void putChar(int index, char value) {
        address.add(index * 2).putChar(swap ? Character.reverseBytes(value) : value);
    }

    @Override
    void getImpl(int index, char[] dst, int offset, int length) {
        var addr = address.add(index * 2);
        if (swap) {
            while (length-- > 0) {
                dst[offset++] = Character.reverseBytes(addr.getChar());
                addr = addr.add(2);
            }
        } else {
            TByteBufferNative.copy(addr, Address.ofData(dst).add(offset * 2), length * 2);
        }
    }

    @Override
    void putImpl(int index, char[] src, int offset, int length) {
        var addr = address.add(index * 2);
        if (swap) {
            while (length-- > 0) {
                addr.putChar(Character.reverseBytes(src[offset++]));
                addr = addr.add(2);
            }
        } else {
            TByteBufferNative.copy(Address.ofData(src).add(offset * 2), addr, length * 2);
        }
    }

    @Override
    void putImpl(int index, TCharBuffer src, int offset, int length) {
        if (src instanceof TCharBufferNative) {
            var srcImpl = (TCharBufferNative) src;
            var addr = address.add(index * 2);
            if (srcImpl.swap == swap) {
                TByteBufferNative.copy(srcImpl.address.add(offset * 2), addr, length * 2);
            } else {
                var srcAddr = srcImpl.address.add(offset * 2);
                while (length-- > 0) {
                    addr.putChar(Character.reverseBytes(srcAddr.getChar()));
                    addr = addr.add(2);
                    srcAddr = srcAddr.add(2);
                }
            }
        } else {
            var addr = address.add(index * 2);
            if (swap) {
                while (length-- > 0) {
                    addr.putChar(Character.reverseBytes(src.get(offset++)));
                    addr = addr.add(2);
                }
            } else {
                while (length-- > 0) {
                    addr.putChar(src.get(offset++));
                    addr = addr.add(2);
                }
            }
        }
    }

    @Override
    void putImpl(int index, String src, int offset, int length) {
        var addr = address.add(index * 2);
        if (swap) {
            while (length-- > 0) {
                addr.putChar(Character.reverseBytes(src.charAt(offset++)));
                addr = addr.add(2);
            }
        } else {
            while (length-- > 0) {
                addr.putChar(src.charAt(offset++));
                addr = addr.add(2);
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
        return new Uint16Array(TJSBufferHelper.WasmGC.getLinearMemory(), address.toInt(), capacity);
    }

    @Override
    public int elementSize() {
        return 2;
    }
}
