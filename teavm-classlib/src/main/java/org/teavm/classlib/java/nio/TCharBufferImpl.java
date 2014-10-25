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
class TCharBufferImpl extends TCharBuffer {
    private boolean readOnly;

    public TCharBufferImpl(int capacity) {
        this(0, capacity, new char[capacity], 0, capacity, false);
    }

    public TCharBufferImpl(int start, int capacity, char[] array, int position, int limit,
            boolean readOnly) {
        super(start, capacity, array, position, limit);
        this.readOnly = readOnly;
    }

    @Override
    public TCharBuffer slice() {
        return new TCharBufferImpl(position, limit - position,  array, 0, limit - position, readOnly);
    }

    @Override
    public TCharBuffer duplicate() {
        return new TCharBufferImpl(start, capacity, array, position, limit, readOnly);
    }

    @Override
    public TCharBuffer asReadOnlyBuffer() {
        return new TCharBufferImpl(start, capacity, array, position, limit, true);
    }

    @Override
    public char get() {
        if (position >= limit) {
            throw new TBufferUnderflowException();
        }
        return array[start + position++];
    }

    @Override
    public TCharBuffer put(char c) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position >= limit) {
            throw new TBufferOverflowException();
        }
        array[start + position++] = c;
        return this;
    }

    @Override
    public char get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + limit + ")");
        }
        return array[start + index];
    }

    @Override
    public TCharBuffer put(int index, char b) {
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
    public TCharBuffer compact() {
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

    @Override
    public TCharBuffer subSequence(int start, int end) {
        return wrap(array, arrayOffset() + start, end - start);
    }
}
