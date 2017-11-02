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

abstract class TCharBufferImpl extends TCharBuffer {
    public TCharBufferImpl(int capacity, int position, int limit) {
        super(capacity, position, limit);
    }

    @Override
    public TCharBuffer slice() {
        return duplicate(position, limit - position, 0, limit - position, isReadOnly());
    }

    @Override
    public TCharBuffer duplicate() {
        return duplicate(0, capacity, position, limit, isReadOnly());
    }

    @Override
    public TCharBuffer asReadOnlyBuffer() {
        return duplicate(0, capacity, position, limit, true);
    }

    abstract TCharBuffer duplicate(int start, int capacity, int position, int limit, boolean readOnly);

    @Override
    public char get() {
        if (position >= limit) {
            throw new TBufferUnderflowException();
        }
        return getChar(position++);
    }

    @Override
    public TCharBuffer put(char c) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (position >= limit) {
            throw new TBufferOverflowException();
        }
        putChar(position++, c);
        return this;
    }

    @Override
    public char get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + limit + ")");
        }
        return getChar(index);
    }

    @Override
    public TCharBuffer put(int index, char b) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + limit + ")");
        }
        putChar(index, b);
        return this;
    }

    @Override
    public TCharBuffer compact() {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        int sz = remaining();
        if (position > 0) {
            int src = position;
            for (int i = 0; i < sz; ++i) {
                putChar(i, getChar(src++));
            }
        }
        position = sz;
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
        return readOnly();
    }

    abstract boolean readOnly();

    @Override
    public TCharBuffer subSequence(int start, int end) {
        return duplicate(0, capacity, position + start, position + end, isReadOnly());
    }
}
