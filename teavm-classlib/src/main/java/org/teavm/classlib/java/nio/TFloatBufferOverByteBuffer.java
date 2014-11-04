/*
 *  Copyright 2014 Alexey Andreev.
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

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
class TFloatBufferOverByteBuffer extends TFloatBufferImpl {
    private TByteBufferImpl byteByffer;
    TByteOrder byteOrder = TByteOrder.BIG_ENDIAN;
    boolean readOnly;
    private int start;

    public TFloatBufferOverByteBuffer(int start, int capacity, TByteBufferImpl byteBuffer, int position, int limit,
            boolean readOnly) {
        super(capacity, position, limit);
        this.start = start;
        this.byteByffer = byteBuffer;
        this.readOnly = readOnly;
    }

    @Override
    TFloatBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        TFloatBufferOverByteBuffer result = new TFloatBufferOverByteBuffer(this.start + start * 2, capacity,
                byteByffer, position, limit, readOnly);
        result.byteOrder = byteOrder;
        return result;
    }

    @Override
    float getElement(int index) {
        int value;
        if (byteOrder == TByteOrder.BIG_ENDIAN) {
            value = (byteByffer.array[start + index * 4] << 24) |
                    (byteByffer.array[start + index * 4 + 1] << 16) |
                    (byteByffer.array[start + index * 4 + 2] << 8) |
                    (byteByffer.array[start + index * 4 + 3]);
        } else {
            value = (byteByffer.array[start + index * 4]) |
                    (byteByffer.array[start + index * 4 + 1] << 8) |
                    (byteByffer.array[start + index * 4 + 2] << 16) |
                    (byteByffer.array[start + index * 4 + 3] << 24);
        }
        return Float.intBitsToFloat(value);
    }

    @Override
    void putElement(int index, float f) {
        int value = Float.floatToIntBits(f);
        if (byteOrder == TByteOrder.BIG_ENDIAN) {
            byteByffer.array[start + index * 4] = (byte)(value >> 24);
            byteByffer.array[start + index * 4 + 1] = (byte)(value >> 16);
            byteByffer.array[start + index * 4 + 2] = (byte)(value >> 8);
            byteByffer.array[start + index * 4 + 3] = (byte)value;
        } else {
            byteByffer.array[start + index * 4] = (byte)value;
            byteByffer.array[start + index * 4 + 1] = (byte)(value >> 8);
            byteByffer.array[start + index * 4 + 2] = (byte)(value >> 16);
            byteByffer.array[start + index * 4 + 3] = (byte)(value >> 24);
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
    boolean readOnly() {
        return readOnly;
    }

    @Override
    public TByteOrder order() {
        return byteOrder;
    }
}
