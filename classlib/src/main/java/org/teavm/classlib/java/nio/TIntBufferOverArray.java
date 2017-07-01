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

class TIntBufferOverArray extends TIntBufferImpl {
    boolean readOnly;
    int start;
    int[] array;

    public TIntBufferOverArray(int capacity) {
        this(0, capacity, new int[capacity], 0, capacity, false);
    }

    public TIntBufferOverArray(int start, int capacity, int[] array, int position, int limit, boolean readOnly) {
        super(capacity, position, limit);
        this.start = start;
        this.readOnly = readOnly;
        this.array = array;
    }

    @Override
    TIntBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly) {
        return new TIntBufferOverArray(this.start + start, capacity, array, position, limit, readOnly);
    }

    @Override
    int getElement(int index) {
        return array[index + start];
    }

    @Override
    void putElement(int index, int value) {
        array[index + start] = value;
    }

    @Override
    boolean isArrayPresent() {
        return true;
    }

    @Override
    int[] getArray() {
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
        return TByteOrder.BIG_ENDIAN;
    }
}
