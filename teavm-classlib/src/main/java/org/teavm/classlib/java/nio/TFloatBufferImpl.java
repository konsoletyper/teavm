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
class TFloatBufferImpl extends TFloatBuffer {
    private boolean readOnly;

    public TFloatBufferImpl(int capacity) {
        this(0, capacity, new float[capacity], 0, capacity, false);
    }

    public TFloatBufferImpl(int start, int capacity, float[] array, int position, int limit, boolean readOnly) {
        super(start, capacity, array, position, limit);
        this.readOnly = readOnly;
    }

    @Override
    public TFloatBuffer slice() {
        return new TFloatBufferImpl(position, limit - position,  array, 0, limit - position, readOnly);
    }

    @Override
    public TFloatBuffer duplicate() {
        return new TFloatBufferImpl(start, capacity, array, position, limit, readOnly);
    }

    @Override
    public TFloatBuffer asReadOnlyBuffer() {
        return new TFloatBufferImpl(start, capacity, array, position, limit, true);
    }

    @Override
    public float get() {
        if (position >= limit) {
            throw new TBufferUnderflowException();
        }
        return array[start + position++];
    }

    @Override
    public TFloatBuffer put(float b) {
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
    public float get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + limit + ")");
        }
        return array[start + index];
    }

    @Override
    public TFloatBuffer put(int index, float b) {
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
    public TFloatBuffer compact() {
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
