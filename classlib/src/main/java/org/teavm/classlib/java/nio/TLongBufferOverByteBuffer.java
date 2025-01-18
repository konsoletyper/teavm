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

abstract class TLongBufferOverByteBuffer extends TLongBufferImpl {
    TByteBufferImpl byteBuffer;
    private boolean readOnly;
    int start;
    private int capacity;

    TLongBufferOverByteBuffer(int start, int capacity, TByteBufferImpl byteBuffer, int position, int limit,
            boolean readOnly) {
        super(position, limit);
        this.start = start;
        this.capacity = capacity;
        this.byteBuffer = byteBuffer;
        this.readOnly = readOnly;
    }

    @Override
    boolean isArrayPresent() {
        return false;
    }

    @Override
    long[] getArray() {
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
    void getImpl(int index, long[] dst, int offset, int length) {
        while (length-- > 0) {
            dst[offset++] = getElement(index++);
        }
    }

    @Override
    void putImpl(int index, long[] src, int offset, int length) {
        while (length-- > 0) {
            putElement(index++, src[offset++]);
        }
    }

    @Override
    void putImpl(int index, TLongBuffer src, int offset, int length) {
        if (src instanceof TLongBufferOverByteBuffer && src.order() == order()) {
            var srcImpl = (TLongBufferOverByteBuffer) src;
            System.arraycopy(srcImpl.byteBuffer.array, srcImpl.start + index * 8,
                    byteBuffer.array, start + offset * 8, length * 8);
        } else {
            while (length-- > 0) {
                putElement(index++, src.getElement(offset++));
            }
        }
    }

    @Override
    int capacityImpl() {
        return capacity;
    }
}
