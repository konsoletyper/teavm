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
import org.teavm.jso.typedarrays.BigInt64Array;

class TLongBufferOverTypedArray extends TLongBufferImpl implements TArrayBufferViewProvider {
    private boolean readOnly;
    private BigInt64Array data;
    private long[] array;

    TLongBufferOverTypedArray(int position, int limit, boolean readOnly, BigInt64Array data, long[] array) {
        super(position, limit);
        this.readOnly = readOnly;
        this.data = data;
        this.array = array;
    }

    @Override
    TLongBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        var slice = new BigInt64Array(data.getBuffer(), data.getByteOffset() + start * 8, capacity);
        return new TLongBufferOverTypedArray(position, limit, readOnly, slice, array);
    }

    @Override
    boolean readOnly() {
        return readOnly;
    }

    @Override
    long getElement(int index) {
        return data.get(index);
    }

    @Override
    void putElement(int index, long value) {
        data.set(index, value);
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
        return data.getByteOffset() / 8;
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
    void getImpl(int index, long[] dst, int offset, int length) {
        var slice = new BigInt64Array(data.getBuffer(), data.getByteOffset() + index * 8, length);
        BigInt64Array.fromJavaArray(dst).set(slice, offset);
    }

    @Override
    void putImpl(int index, long[] src, int offset, int length) {
        var slice = new BigInt64Array(BigInt64Array.fromJavaArray(src).getBuffer(), offset * 8, length);
        data.set(slice, index);
    }

    @Override
    void putImpl(int index, TLongBuffer src, int offset, int length) {
        if (src instanceof TLongBufferOverTypedArray) {
            var srcImpl = (TLongBufferOverTypedArray) src;
            var slice = new BigInt64Array(srcImpl.data.getBuffer(), srcImpl.data.getByteOffset() + index * 8, length);
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
        return 8;
    }
}
