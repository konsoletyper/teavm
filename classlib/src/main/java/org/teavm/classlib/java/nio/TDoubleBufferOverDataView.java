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

class TDoubleBufferOverDataView extends TDoubleBufferImpl implements TArrayBufferViewProvider {
    private boolean readOnly;
    private DataView data;
    private boolean littleEndian;

    TDoubleBufferOverDataView(int position, int limit, boolean readOnly, DataView data, boolean littleEndian) {
        super(position, limit);
        this.readOnly = readOnly;
        this.data = data;
        this.littleEndian = littleEndian;
    }

    @Override
    TDoubleBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        var slice = new DataView(data.getBuffer(), data.getByteOffset() + start * 8, capacity * 8);
        return new TDoubleBufferOverDataView(position, limit, readOnly, slice, littleEndian);
    }

    @Override
    boolean readOnly() {
        return readOnly;
    }

    @Override
    double getElement(int index) {
        return data.getFloat64(index * 8, littleEndian);
    }

    @Override
    void putElement(int index, double value) {
        data.setFloat64(index * 8, value, littleEndian);
    }

    @Override
    void getImpl(int index, double[] dst, int offset, int length) {
        index *= 8;
        while (length-- > 0) {
            dst[offset++] = data.getFloat64(index, littleEndian);
            index += 8;
        }
    }

    @Override
    void putImpl(int index, TDoubleBuffer src, int offset, int length) {
        if (src instanceof TDoubleBufferOverDataView) {
            length *= 8;
            var srcImpl = (TDoubleBufferOverDataView) src;
            var srcArray = new Int8Array(srcImpl.data.getBuffer(), srcImpl.data.getByteOffset() + offset * 8, length);
            var destArray = new Int8Array(data.getBuffer(), data.getByteOffset() + index * 8, length);
            destArray.set(srcArray, 0);
        } else {
            index *= 8;
            while (length-- > 0) {
                data.setFloat64(index, src.get(offset++), littleEndian);
                index += 8;
            }
        }
    }

    @Override
    void putImpl(int index, double[] src, int offset, int length) {
        index *= 8;
        while (length-- > 0) {
            data.setFloat64(index, src[offset++], littleEndian);
            index += 8;
        }
    }

    @Override
    boolean isArrayPresent() {
        return false;
    }

    @Override
    double[] getArray() {
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
        return data.getByteLength() / 8;
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
