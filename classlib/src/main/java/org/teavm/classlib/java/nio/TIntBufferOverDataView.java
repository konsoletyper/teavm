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

import org.teavm.jso.typedarrays.DataView;
import org.teavm.jso.typedarrays.Int8Array;

class TIntBufferOverDataView extends TIntBufferImpl {
    private boolean readOnly;
    private DataView data;
    private boolean littleEndian;

    TIntBufferOverDataView(int position, int limit, boolean readOnly, DataView data, boolean littleEndian) {
        super(position, limit);
        this.readOnly = readOnly;
        this.data = data;
        this.littleEndian = littleEndian;
    }

    @Override
    TIntBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        var slice = new DataView(data.getBuffer(), data.getByteOffset() + start * 4, capacity);
        return new TIntBufferOverDataView(position, limit, readOnly, slice, littleEndian);
    }

    @Override
    boolean readOnly() {
        return readOnly;
    }

    @Override
    int getElement(int index) {
        return data.getInt32(index * 4, littleEndian);
    }

    @Override
    void putElement(int index, int value) {
        data.setInt32(index * 4, value, littleEndian);
    }

    @Override
    void getImpl(int index, int[] dst, int offset, int length) {
        index *= 4;
        while (length-- > 0) {
            dst[offset++] = data.getInt32(index, littleEndian);
            index += 4;
        }
    }

    @Override
    void putImpl(int index, TIntBuffer src, int offset, int length) {
        if (src instanceof TIntBufferOverDataView) {
            length *= 4;
            var srcImpl = (TIntBufferOverDataView) src;
            var srcArray = new Int8Array(srcImpl.data.getBuffer(), srcImpl.data.getByteOffset() + offset * 4, length);
            var destArray = new Int8Array(data.getBuffer(), data.getByteOffset() + index * 4, length);
            destArray.set(srcArray, 0);
        } else {
            index *= 4;
            while (length-- > 0) {
                data.setInt32(index, src.get(offset++), littleEndian);
                index += 4;
            }
        }
    }

    @Override
    void putImpl(int index, int[] src, int offset, int length) {
        while (length-- > 0) {
            data.setInt32(index, src[offset++], littleEndian);
            index += 4;
        }
    }

    @Override
    boolean isArrayPresent() {
        return false;
    }

    @Override
    int[] getArray() {
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
        return data.getLength() / 4;
    }
}