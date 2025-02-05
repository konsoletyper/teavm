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
import org.teavm.jso.typedarrays.Int32Array;

class TIntBufferOverTypedArray extends TIntBufferImpl implements TArrayBufferViewProvider {
    private boolean readOnly;
    private Int32Array data;
    private int[] array;

    TIntBufferOverTypedArray(int position, int limit, boolean readOnly, Int32Array data, int[] array) {
        super(position, limit);
        this.readOnly = readOnly;
        this.data = data;
        this.array = array;
    }

    @Override
    TIntBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        var slice = new Int32Array(data.getBuffer(), data.getByteOffset() + start * 4, capacity);
        return new TIntBufferOverTypedArray(position, limit, readOnly, slice, array);
    }

    @Override
    boolean readOnly() {
        return readOnly;
    }

    @Override
    int getElement(int index) {
        return data.get(index);
    }

    @Override
    void putElement(int index, int value) {
        data.set(index, value);
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
        return data.getByteOffset() / 4;
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
    void getImpl(int index, int[] dst, int offset, int length) {
        var slice = new Int32Array(data.getBuffer(), data.getByteOffset() + index * 4, length);
        Int32Array.fromJavaArray(dst).set(slice, offset);
    }

    @Override
    void putImpl(int index, int[] src, int offset, int length) {
        var slice = new Int32Array(Int32Array.fromJavaArray(src).getBuffer(), offset * 4, length);
        data.set(slice, index);
    }

    @Override
    void putImpl(int index, TIntBuffer src, int offset, int length) {
        if (src instanceof TIntBufferOverTypedArray) {
            var srcImpl = (TIntBufferOverTypedArray) src;
            var slice = new Int32Array(srcImpl.data.getBuffer(), srcImpl.data.getByteOffset() + index * 4, length);
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
        return 4;
    }
}
