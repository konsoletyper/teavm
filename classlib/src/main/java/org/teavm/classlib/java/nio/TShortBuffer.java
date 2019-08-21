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

public abstract class TShortBuffer extends TBuffer implements Comparable<TShortBuffer> {
    TShortBuffer(int capacity, int position, int limit) {
        super(capacity);
        this.position = position;
        this.limit = limit;
    }

    public static TShortBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity is negative: " + capacity);
        }
        return new TShortBufferOverArray(capacity);
    }

    public static TShortBuffer wrap(short[] array, int offset, int length) {
        return new TShortBufferOverArray(0, array.length, array, offset, offset + length, false);
    }

    public static TShortBuffer wrap(short[] array) {
        return wrap(array, 0, array.length);
    }

    public abstract TShortBuffer slice();

    public abstract TShortBuffer duplicate();

    public abstract TShortBuffer asReadOnlyBuffer();

    public abstract short get();

    public abstract TShortBuffer put(short b);

    public abstract short get(int index);

    public abstract TShortBuffer put(int index, short b);

    abstract short getElement(int index);

    abstract void putElement(int index, short value);

    public TShortBuffer get(short[] dst, int offset, int length) {
        if (offset < 0 || offset >= dst.length) {
            throw new IndexOutOfBoundsException("Offset " + offset + " is outside of range [0;" + dst.length + ")");
        }
        if (offset + length > dst.length) {
            throw new IndexOutOfBoundsException("The last short in dst " + (offset + length) + " is outside "
                    + "of array of size " + dst.length);
        }
        if (remaining() < length) {
            throw new TBufferUnderflowException();
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length " + length + " must be non-negative");
        }
        int pos = position;
        for (int i = 0; i < length; ++i) {
            dst[offset++] = getElement(pos++);
        }
        position += length;
        return this;
    }

    public TShortBuffer get(short[] dst) {
        return get(dst, 0, dst.length);
    }

    public TShortBuffer put(TShortBuffer src) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (remaining() < src.remaining()) {
            throw new TBufferOverflowException();
        }
        int length = src.remaining();
        int pos = position;
        int offset = src.position;
        for (int i = 0; i < length; ++i) {
            putElement(pos++, src.getElement(offset++));
        }
        position += length;
        return this;
    }

    public TShortBuffer put(short[] src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (remaining() < length) {
            throw new TBufferOverflowException();
        }
        if (offset < 0 || offset >= src.length) {
            throw new IndexOutOfBoundsException("Offset " + offset + " is outside of range [0;" + src.length + ")");
        }
        if (offset + length > src.length) {
            throw new IndexOutOfBoundsException("The last short in src " + (offset + length) + " is outside "
                    + "of array of size " + src.length);
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length " + length + " must be non-negative");
        }
        int pos = position;
        for (int i = 0; i < length; ++i) {
            putElement(pos++, src[offset++]);
        }
        position += length;
        return this;
    }

    public final TShortBuffer put(short[] src) {
        return put(src, 0, src.length);
    }

    @Override
    public final boolean hasArray() {
        return isArrayPresent();
    }

    @Override
    public final short[] array() {
        return getArray();
    }

    @Override
    public final int arrayOffset() {
        return getArrayOffset();
    }

    abstract boolean isArrayPresent();

    abstract short[] getArray();

    abstract int getArrayOffset();

    public abstract TShortBuffer compact();

    @Override
    public abstract boolean isDirect();

    @Override
    public String toString() {
        return "[ShortBuffer position=" + position + ", limit=" + limit + ", capacity=" + capacity + ", mark "
                + (mark >= 0 ? " at " + mark : " is not set") + "]";
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        int pos = position;
        for (int i = position; i < limit; ++i) {
            hashCode = 31 * hashCode + getElement(pos++);
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TShortBuffer)) {
            return false;
        }
        TShortBuffer other = (TShortBuffer) obj;
        int sz = remaining();
        if (sz != other.remaining()) {
            return false;
        }
        int a = position;
        int b = other.position;
        for (int i = 0; i < sz; ++i) {
            if (getElement(a++) != other.getElement(b++)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int compareTo(TShortBuffer other) {
        if (this == other) {
            return 0;
        }
        int sz = Math.min(remaining(), other.remaining());
        int a = position;
        int b = other.position;
        for (int i = 0; i < sz; ++i) {
            int r = Short.compare(getElement(a++), other.getElement(b++));
            if (r != 0) {
                return r;
            }
        }
        return Integer.compare(remaining(), other.remaining());
    }

    public abstract TByteOrder order();

    @Override
    public final TShortBuffer mark() {
        super.mark();
        return this;
    }

    @Override
    public final TShortBuffer reset() {
        super.reset();
        return this;
    }

    @Override
    public final TShortBuffer clear() {
        super.clear();
        return this;
    }

    @Override
    public final TShortBuffer flip() {
        super.flip();
        return this;
    }

    @Override
    public final TShortBuffer rewind() {
        super.rewind();
        return this;
    }

    @Override
    public TShortBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }

    @Override
    public TShortBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }
}
