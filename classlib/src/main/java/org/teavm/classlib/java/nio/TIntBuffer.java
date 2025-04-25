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

import org.teavm.classlib.PlatformDetector;
import org.teavm.interop.Address;
import org.teavm.jso.typedarrays.Int32Array;

public abstract class TIntBuffer extends TBuffer implements Comparable<TIntBuffer> {
    TIntBuffer(int position, int limit) {
        this.position = position;
        this.limit = limit;
    }

    public static TIntBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity is negative: " + capacity);
        }
        if (PlatformDetector.isJavaScript()) {
            var array = new int[capacity];
            return new TIntBufferOverTypedArray(0, capacity, false, Int32Array.fromJavaArray(array), array);
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var array = new int[capacity];
            return new TIntBufferNative(null, array, 0, capacity, false, array, Address.ofData(array),
                    capacity, false);
        }
        return new TIntBufferOverArray(capacity);
    }

    public static TIntBuffer wrap(int[] array, int offset, int length) {
        if (PlatformDetector.isJavaScript()) {
            var result = new TIntBufferOverTypedArray(0, array.length, false, Int32Array.fromJavaArray(array), array);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var result = new TIntBufferNative(null, array, 0, array.length, false, array, Address.ofData(array),
                    array.length, false);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
        return new TIntBufferOverArray(0, array.length, array, offset, offset + length, false);
    }

    public static TIntBuffer wrap(int[] array) {
        return wrap(array, 0, array.length);
    }

    public abstract TIntBuffer slice();

    public abstract TIntBuffer duplicate();

    public abstract TIntBuffer asReadOnlyBuffer();

    public abstract int get();

    public abstract TIntBuffer put(int b);

    public abstract int get(int index);

    public abstract TIntBuffer put(int index, int b);

    abstract int getElement(int index);

    abstract void putElement(int index, int value);

    public TIntBuffer get(int[] dst, int offset, int length) {
        if (length < 0 || offset < 0 || offset + length > dst.length) {
            throw new IndexOutOfBoundsException();
        }
        if (length > remaining()) {
            throw new TBufferUnderflowException();
        }
        getImpl(position, dst, offset, length);
        position += length;
        return this;
    }

    public TIntBuffer get(int index, int[] dst, int offset, int length) {
        if (length < 0 || offset < 0 || offset + length > dst.length || index < 0 || index + length > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        getImpl(index, dst, offset, length);
        return this;
    }

    public TIntBuffer get(int index, int[] dst) {
        return get(index, dst, 0, dst.length);
    }

    abstract void getImpl(int index, int[] dst, int offset, int length);

    public TIntBuffer get(int[] dst) {
        return get(dst, 0, dst.length);
    }

    public TIntBuffer put(int index, TIntBuffer src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index > limit() || offset < 0 || offset + length > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(index, src, offset, length);
        return this;
    }

    public TIntBuffer put(int index, int[] src) {
        return put(index, src, 0, src.length);
    }

    abstract void putImpl(int index, TIntBuffer src, int offset, int length);

    public TIntBuffer put(TIntBuffer src) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (remaining() < src.remaining()) {
            throw new TBufferOverflowException();
        }
        putImpl(position, src, src.position, src.remaining());
        position += src.remaining();
        return this;
    }

    public TIntBuffer put(int[] src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (remaining() < length) {
            throw new TBufferOverflowException();
        }
        if (length < 0 || offset < 0 || offset + length > src.length) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(position, src, offset, length);
        position += length;
        return this;
    }

    public TIntBuffer put(int index, int[] src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (length < 0 || offset < 0 || offset + length > src.length || index < 0 || index + length > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(index, src, offset, length);
        return this;
    }

    public final TIntBuffer put(int[] src) {
        return put(src, 0, src.length);
    }

    abstract void putImpl(int index, int[] src, int offset, int length);

    @Override
    public final boolean hasArray() {
        return isArrayPresent();
    }

    @Override
    public final int[] array() {
        return getArray();
    }

    @Override
    public final int arrayOffset() {
        return getArrayOffset();
    }

    abstract boolean isArrayPresent();

    abstract int[] getArray();

    abstract int getArrayOffset();

    public abstract TIntBuffer compact();

    @Override
    public abstract boolean isDirect();

    @Override
    public String toString() {
        return "[IntBuffer position=" + position + ", limit=" + limit + ", capacity=" + capacity() + ", mark "
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
        if (!(obj instanceof TIntBuffer)) {
            return false;
        }
        TIntBuffer other = (TIntBuffer) obj;
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
    public int compareTo(TIntBuffer other) {
        if (this == other) {
            return 0;
        }
        int sz = Math.min(remaining(), other.remaining());
        int a = position;
        int b = other.position;
        for (int i = 0; i < sz; ++i) {
            int r = Integer.compare(getElement(a++), other.getElement(b++));
            if (r != 0) {
                return r;
            }
        }
        return Integer.compare(remaining(), other.remaining());
    }

    public abstract TByteOrder order();

    @Override
    public final TIntBuffer mark() {
        super.mark();
        return this;
    }

    @Override
    public final TIntBuffer reset() {
        super.reset();
        return this;
    }

    @Override
    public final TIntBuffer clear() {
        super.clear();
        return this;
    }

    @Override
    public final TIntBuffer flip() {
        super.flip();
        return this;
    }

    @Override
    public final TIntBuffer rewind() {
        super.rewind();
        return this;
    }

    @Override
    public TIntBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }

    @Override
    public TIntBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }
}
