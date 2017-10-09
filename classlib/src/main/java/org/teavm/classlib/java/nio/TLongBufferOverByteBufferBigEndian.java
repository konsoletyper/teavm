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

class TLongBufferOverByteBufferBigEndian extends TLongBufferOverByteBuffer {
    public TLongBufferOverByteBufferBigEndian(int start, int capacity, TByteBufferImpl byteBuffer, int position,
            int limit, boolean readOnly) {
        super(start, capacity, byteBuffer, position, limit, readOnly);
    }

    @Override
    TLongBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TLongBufferOverByteBufferBigEndian(this.start + start * 8, capacity,
                byteByffer, position, limit, readOnly);
    }

    @Override
    long getElement(int index) {
        return (((long) byteByffer.array[start + index * 8] & 0xFF) << 56)
                | (((long) byteByffer.array[start + index * 8 + 1] & 0xFF) << 48)
                | (((long) byteByffer.array[start + index * 8 + 2] & 0xFF) << 40)
                | (((long) byteByffer.array[start + index * 8 + 3] & 0xFF) << 32)
                | (((long) byteByffer.array[start + index * 8 + 4] & 0xFF) << 24)
                | (((long) byteByffer.array[start + index * 8 + 5] & 0xFF) << 16)
                | (((long) byteByffer.array[start + index * 8 + 6] & 0xFF) << 8)
                | (byteByffer.array[start + index * 8 + 7] & 0xFF);
    }

    @Override
    void putElement(int index, long value) {
        byteByffer.array[start + index * 8] = (byte) (value >> 56);
        byteByffer.array[start + index * 8 + 1] = (byte) (value >> 48);
        byteByffer.array[start + index * 8 + 2] = (byte) (value >> 40);
        byteByffer.array[start + index * 8 + 3] = (byte) (value >> 32);
        byteByffer.array[start + index * 8 + 4] = (byte) (value >> 24);
        byteByffer.array[start + index * 8 + 5] = (byte) (value >> 16);
        byteByffer.array[start + index * 8 + 6] = (byte) (value >> 8);
        byteByffer.array[start + index * 8 + 7] = (byte) value;
    }

    @Override
    public TByteOrder order() {
        return TByteOrder.BIG_ENDIAN;
    }
}
