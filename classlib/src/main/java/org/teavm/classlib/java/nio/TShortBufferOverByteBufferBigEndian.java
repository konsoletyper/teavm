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

class TShortBufferOverByteBufferBigEndian extends TShortBufferOverByteBuffer {
    TShortBufferOverByteBufferBigEndian(int start, int capacity, TByteBufferImpl byteBuffer, int position,
            int limit, boolean readOnly) {
        super(start, capacity, byteBuffer, position, limit, readOnly);
    }

    @Override
    TShortBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TShortBufferOverByteBufferBigEndian(this.start + start * 2, capacity,
                byteBuffer, position, limit, readOnly);
    }

    @Override
    short getElement(int index) {
        int value = ((byteBuffer.array[start + index * 2] & 0xFF) << 8)
                | (byteBuffer.array[start + index * 2 + 1] & 0xFF);
        return (short) value;
    }

    @Override
    void putElement(int index, short value) {
        byteBuffer.array[start + index * 2] = (byte) (value >> 8);
        byteBuffer.array[start + index * 2 + 1] = (byte) value;
    }

    @Override
    public TByteOrder order() {
        return TByteOrder.BIG_ENDIAN;
    }
}
