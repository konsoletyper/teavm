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
import org.teavm.jso.typedarrays.DataView;
import org.teavm.jso.typedarrays.Int8Array;

class TFloatBufferOverDataView extends TFloatBufferImpl implements TArrayBufferViewProvider {
    private boolean readOnly;
    private DataView data;
    private boolean littleEndian;

    TFloatBufferOverDataView(int position, int limit, boolean readOnly, DataView data, boolean littleEndian) {
        super(position, limit);
        this.readOnly = readOnly;
        this.data = data;
        this.littleEndian = littleEndian;
    }

    @Override
    TFloatBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        var slice = new DataView(data.getBuffer(), data.getByteOffset() + start * 4, capacity * 4);
        return new TFloatBufferOverDataView(position, limit, readOnly, slice, littleEndian);
    }

    @Override
    boolean readOnly() {
        return readOnly;
    }

    @Override
    float getElement(int index) {
        return data.getFloat32(index * 4, littleEndian);
    }

    @Override
    void putElement(int index, float value) {
        data.setFloat32(index * 4, value, littleEndian);
    }

    @Override
    void getImpl(int index, float[] dst, int offset, int length) {
        index *= 4;
        while (length-- > 0) {
            dst[offset++] = data.getFloat32(index, littleEndian);
            index += 4;
        }
    }

    @Override
    void putImpl(int index, TFloatBuffer src, int offset, int length) {
        if (src instanceof TFloatBufferOverDataView) {
            length *= 4;
            var srcImpl = (TFloatBufferOverDataView) src;
            var srcArray = new Int8Array(srcImpl.data.getBuffer(), srcImpl.data.getByteOffset() + offset * 4, length);
            var destArray = new Int8Array(data.getBuffer(), data.getByteOffset() + index * 4, length);
            destArray.set(srcArray, 0);
        } else {
            index *= 4;
            while (length-- > 0) {
                data.setFloat32(index, src.get(offset++), littleEndian);
                index += 4;
            }
        }
    }

    @Override
    void putImpl(int index, float[] src, int offset, int length) {
        index *= 4;
        while (length-- > 0) {
            data.setFloat32(index, src[offset++], littleEndian);
            index += 4;
        }
    }

    @Override
    boolean isArrayPresent() {
        return false;
    }

    @Override
    float[] getArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getArrayOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TByteOrder order() {
        return littleEndian ? TByteOrder.LITTLE_ENDIAN : TByteOrder.BIG_ENDIAN;
    }

    @Override
    int capacityImpl() {
        return data.getByteLength() / 4;
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
