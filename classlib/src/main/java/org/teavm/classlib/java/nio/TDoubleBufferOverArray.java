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

class TDoubleBufferOverArray extends TDoubleBufferImpl {
    private boolean readOnly;
    private int start;
    private int capacity;
    private double[] array;

    TDoubleBufferOverArray(int capacity) {
        this(0, capacity, new double[capacity], 0, capacity, false);
    }

    TDoubleBufferOverArray(int start, int capacity, double[] array, int position, int limit, boolean readOnly) {
        super(position, limit);
        this.start = start;
        this.capacity = capacity;
        this.readOnly = readOnly;
        this.array = array;
    }

    @Override
    TDoubleBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TDoubleBufferOverArray(this.start + start, capacity, array, position, limit, readOnly);
    }

    @Override
    int capacityImpl() {
        return capacity;
    }

    @Override
    double getElement(int index) {
        return array[index + start];
    }

    @Override
    void putElement(int index, double value) {
        array[index + start] = value;
    }

    @Override
    boolean isArrayPresent() {
        return true;
    }

    @Override
    double[] getArray() {
        return array;
    }

    @Override
    int getArrayOffset() {
        return start;
    }

    @Override
    boolean readOnly() {
        return readOnly;
    }

    @Override
    public TByteOrder order() {
        return TByteOrder.nativeOrder();
    }

    @Override
    void getImpl(int index, double[] dst, int offset, int length) {
        System.arraycopy(array, start + index, dst, offset, length);
    }

    @Override
    void putImpl(int index, double[] src, int offset, int length) {
        System.arraycopy(src, offset, array, start + index, length);
    }

    @Override
    void putImpl(int index, TDoubleBuffer src, int offset, int length) {
        if (src.hasArray()) {
            System.arraycopy(src.array(), src.arrayOffset() + offset, array, start + index, length);
        } else {
            while (length-- > 0) {
                src.putElement(offset++, getElement(index++));
            }
        }
    }
}
