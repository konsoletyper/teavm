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
public class TIntBufferOverByteBuffer extends TIntBufferImpl {
    private TByteBufferImpl byteByffer;
    TByteOrder byteOrder = TByteOrder.BIG_ENDIAN;
    boolean readOnly;
    private int start;

    public TIntBufferOverByteBuffer(int start, int capacity, TByteBufferImpl byteBuffer, int position, int limit,
            boolean readOnly) {
        super(capacity, position, limit);
        this.start = start;
        this.byteByffer = byteBuffer;
        this.readOnly = readOnly;
    }

    @Override
    TIntBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        TIntBufferOverByteBuffer result = new TIntBufferOverByteBuffer(this.start + start * 2, capacity,
                byteByffer, position, limit, readOnly);
        result.byteOrder = byteOrder;
        return result;
    }

    @Override
    int getElement(int index) {
        int value;
        if (byteOrder == TByteOrder.BIG_ENDIAN) {
            value = ((byteByffer.array[start + index * 4] & 0xFF) << 24) |
                    ((byteByffer.array[start + index * 4 + 1] & 0xFF) << 16) |
                    ((byteByffer.array[start + index * 4 + 2] & 0xFF) << 8) |
                    (byteByffer.array[start + index * 4 + 3] & 0xFF);
        } else {
            value = (byteByffer.array[start + index * 4] & 0xFF) |
                    ((byteByffer.array[start + index * 4 + 1] & 0xFF) << 8) |
                    ((byteByffer.array[start + index * 4 + 2] & 0xFF) << 16) |
                    ((byteByffer.array[start + index * 4 + 3] & 0xFF) << 24);
        }
        return value;
    }

    @Override
    void putElement(int index, int value) {
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
    int[] getArray() {
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
