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
import org.teavm.jso.typedarrays.Float64Array;

class TDoubleBufferOverTypedArray extends TDoubleBufferImpl implements TArrayBufferViewProvider {
    private boolean readOnly;
    private Float64Array data;
    private double[] array;

    TDoubleBufferOverTypedArray(int position, int limit, boolean readOnly, Float64Array data, double[] array) {
        super(position, limit);
        this.readOnly = readOnly;
        this.data = data;
        this.array = array;
    }

    @Override
    TDoubleBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        var slice = new Float64Array(data.getBuffer(), data.getByteOffset() + start * 8, capacity);
        return new TDoubleBufferOverTypedArray(position, limit, readOnly, slice, array);
    }

    @Override
    boolean readOnly() {
        return readOnly;
    }

    @Override
    double getElement(int index) {
        return data.get(index);
    }

    @Override
    void putElement(int index, double value) {
        data.set(index, value);
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
    void getImpl(int index, double[] dst, int offset, int length) {
        var slice = new Float64Array(data.getBuffer(), data.getByteOffset() + index * 8, length);
        Float64Array.fromJavaArray(dst).set(slice, offset);
    }

    @Override
    void putImpl(int index, double[] src, int offset, int length) {
        var slice = new Float64Array(Float64Array.fromJavaArray(src).getBuffer(), offset * 8, length);
        data.set(slice, index);
    }

    @Override
    void putImpl(int index, TDoubleBuffer src, int offset, int length) {
        if (src instanceof TDoubleBufferOverTypedArray) {
            var srcImpl = (TDoubleBufferOverTypedArray) src;
            var slice = new Float64Array(srcImpl.data.getBuffer(), srcImpl.data.getByteOffset() + index * 8, length);
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
