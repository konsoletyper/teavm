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

import org.teavm.jso.typedarrays.Float32Array;

class TFloatBufferOverTypedArray extends TFloatBufferImpl {
    private boolean readOnly;
    private Float32Array data;
    private float[] array;

    TFloatBufferOverTypedArray(int position, int limit, boolean readOnly, Float32Array data, float[] array) {
        super(position, limit);
        this.readOnly = readOnly;
        this.data = data;
        this.array = array;
    }

    @Override
    TFloatBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        var slice = new Float32Array(data.getBuffer(), data.getByteOffset() + start * 4, capacity);
        return new TFloatBufferOverTypedArray(position, limit, readOnly, slice, array);
    }

    @Override
    boolean readOnly() {
        return readOnly;
    }

    @Override
    float getElement(int index) {
        return data.get(index);
    }

    @Override
    void putElement(int index, float value) {
        data.set(index, value);
    }

    @Override
    boolean isArrayPresent() {
        return array != null;
    }

    @Override
    float[] getArray() {
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
    void getImpl(int index, float[] dst, int offset, int length) {
        var slice = new Float32Array(data.getBuffer(), data.getByteOffset() + index * 4, length);
        Float32Array.fromJavaArray(dst).set(slice, offset);
    }

    @Override
    void putImpl(int index, float[] src, int offset, int length) {
        var slice = new Float32Array(Float32Array.fromJavaArray(src).getBuffer(), offset * 4, length);
        data.set(slice, index);
    }

    @Override
    void putImpl(int index, TFloatBuffer src, int offset, int length) {
        if (src instanceof TFloatBufferOverTypedArray) {
            var srcImpl = (TFloatBufferOverTypedArray) src;
            var slice = new Float32Array(srcImpl.data.getBuffer(), srcImpl.data.getByteOffset() + index * 4, length);
            data.set(slice, index);
        } else {
            while (length-- > 0) {
                data.set(index++, src.get(offset++));
            }
        }
    }
}
