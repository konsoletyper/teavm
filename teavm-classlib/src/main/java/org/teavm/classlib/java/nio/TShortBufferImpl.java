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
class TShortBufferImpl extends TShortBuffer {
    private boolean readOnly;

    public TShortBufferImpl(int capacity) {
        this(0, capacity, new short[capacity], 0, capacity, false);
    }

    public TShortBufferImpl(int start, int capacity, short[] array, int position, int limit, boolean readOnly) {
        super(start, capacity, array, position, limit);
        this.readOnly = readOnly;
    }

    @Override
    public TShortBuffer slice() {
        return new TShortBufferImpl(position, limit - position,  array, 0, limit - position, readOnly);
    }

    @Override
    public TShortBuffer duplicate() {
        return new TShortBufferImpl(start, capacity, array, position, limit, readOnly);
    }

    @Override
    public TShortBuffer asReadOnlyBuffer() {
        return new TShortBufferImpl(start, capacity, array, position, limit, true);
    }

    @Override
    public short get() {
        if (position >= limit) {
            throw new TBufferUnderflowException();
        }
        return array[start + position++];
    }

    @Override
    public TShortBuffer put(short b) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position >= limit) {
            throw new TBufferOverflowException();
        }
        array[start + position++] = b;
        return this;
    }

    @Override
    public short get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + limit + ")");
        }
        return array[start + index];
    }

    @Override
    public TShortBuffer put(int index, short b) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + limit + ")");
        }
        array[start + index] = b;
        return this;
    }

    @Override
    public TShortBuffer compact() {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position > 0) {
            int sz = remaining();
            int dst = start;
            int src = start + position;
            for (int i = 0; i < sz; ++i) {
                array[dst++] = array[src++];
            }
            position = sz;
        }
        limit = capacity;
        mark = -1;
        return this;
    }

    @Override
    public boolean isDirect() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }
}
