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

import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Int16Array;

class TShortBufferOverTypedArray extends TShortBufferImpl implements TArrayBufferViewProvider {
    private boolean readOnly;
    private Int16Array data;
    private short[] array;

    TShortBufferOverTypedArray(int position, int limit, boolean readOnly, Int16Array data, short[] array) {
        super(position, limit);
        this.readOnly = readOnly;
        this.data = data;
        this.array = array;
    }

    @Override
    TShortBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        var slice = new Int16Array(data.getBuffer(), data.getByteOffset() + start * 2, capacity);
        return new TShortBufferOverTypedArray(position, limit, readOnly, slice, array);
    }

    @Override
    boolean readOnly() {
        return readOnly;
    }

    @Override
    short getElement(int index) {
        return data.get(index);
    }

    @Override
    void putElement(int index, short value) {
        data.set(index, value);
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
    void getImpl(int index, short[] dst, int offset, int length) {
        var slice = new Int16Array(data.getBuffer(), data.getByteOffset() + index * 2, length);
        Int16Array.fromJavaArray(dst).set(slice, offset);
    }

    @Override
    void putImpl(int index, short[] src, int offset, int length) {
        var slice = new Int16Array(Int16Array.fromJavaArray(src).getBuffer(), offset * 2, length);
        data.set(slice, index);
    }

    @Override
    void putImpl(int index, TShortBuffer src, int offset, int length) {
        if (src instanceof TShortBufferOverTypedArray) {
            var srcImpl = (TShortBufferOverTypedArray) src;
            var slice = new Int16Array(srcImpl.data.getBuffer(), srcImpl.data.getByteOffset() + index * 2, length);
            data.set(slice, index);
        } else {
            while (length-- > 0) {
                data.set(index++, src.get(offset++));
            }
        }
    }

    @Override
    public ArrayBufferView getArrayBufferView() {
        return data;
    }

    @Override
    public int elementSize() {
        return 2;
    }
}
