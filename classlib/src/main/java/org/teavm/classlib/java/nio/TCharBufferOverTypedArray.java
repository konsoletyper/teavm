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

import org.teavm.jso.typedarrays.Uint16Array;

class TCharBufferOverTypedArray extends TCharBufferImpl {
    private boolean readOnly;
    private Uint16Array data;
    private char[] array;

    TCharBufferOverTypedArray(int position, int limit, boolean readOnly, Uint16Array data, char[] array) {
        super(position, limit);
        this.readOnly = readOnly;
        this.data = data;
        this.array = array;
    }

    @Override
    TCharBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        var slice = new Uint16Array(data.getBuffer(), data.getByteOffset() + start * 2, capacity);
        return new TCharBufferOverTypedArray(position, limit, readOnly, slice, array);
    }

    @Override
    boolean readOnly() {
        return readOnly;
    }

    @Override
    char getChar(int index) {
        return (char) data.get(index);
    }

    @Override
    void putChar(int index, char value) {
        data.set(index, value);
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
        return data.getByteOffset() / 2;
    }

    @Override
    public TByteOrder order() {
        return TByteOrder.nativeOrder();
    }

    @Override
    int capacityImpl() {
        return data.getLength();
    }

    @Override
    void getImpl(int index, char[] dst, int offset, int length) {
        var slice = new Uint16Array(data.getBuffer(), data.getByteOffset() + index * 2, length);
        Uint16Array.fromJavaArray(dst).set(slice, offset);
    }

    @Override
    void putImpl(int index, char[] src, int offset, int length) {
        var slice = new Uint16Array(Uint16Array.fromJavaArray(src).getBuffer(), offset * 2, length);
        data.set(slice, index);
    }

    @Override
    void putImpl(int index, String src, int offset, int length) {
        while (length-- > 0) {
            data.set(index++, src.charAt(offset++));
        }
    }

    @Override
    void putImpl(int index, TCharBuffer src, int offset, int length) {
        if (src instanceof TCharBufferOverTypedArray) {
            var srcImpl = (TCharBufferOverTypedArray) src;
            var slice = new Uint16Array(srcImpl.data.getBuffer(), srcImpl.data.getByteOffset() + index * 2, length);
            data.set(slice, index);
        } else {
            while (length-- > 0) {
                data.set(index++, src.get(offset++));
            }
        }
    }
}
