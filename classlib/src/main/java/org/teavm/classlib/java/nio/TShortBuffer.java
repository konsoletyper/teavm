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
import org.teavm.jso.typedarrays.Int16Array;

public abstract class TShortBuffer extends TBuffer implements Comparable<TShortBuffer> {
    TShortBuffer(int position, int limit) {
        this.position = position;
        this.limit = limit;
    }

    public static TShortBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity is negative: " + capacity);
        }
        if (PlatformDetector.isJavaScript()) {
            var array = new short[capacity];
            return new TShortBufferOverTypedArray(0, capacity, false, Int16Array.fromJavaArray(array), array);
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var array = new short[capacity];
            return new TShortBufferNative(array, 0, capacity, false, array, Address.ofData(array), capacity, false);
        }
        return new TShortBufferOverArray(capacity);
    }

    public static TShortBuffer wrap(short[] array, int offset, int length) {
        if (PlatformDetector.isJavaScript()) {
            var result = new TShortBufferOverTypedArray(0, array.length, false, Int16Array.fromJavaArray(array), array);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var result = new TShortBufferNative(array, 0, array.length, false, array, Address.ofData(array),
                    array.length, false);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
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

    public TShortBuffer get(int index, short[] dst, int offset, int length) {
        if (length < 0 || offset < 0 || offset + length > dst.length || index < 0 || index + length > limit()) {
            throw new IndexOutOfBoundsException();
        }
        getImpl(index, dst, offset, length);
        return this;
    }

    public TShortBuffer get(int index, short[] dst) {
        return get(index, dst, 0, dst.length);
    }

    abstract void getImpl(int index, short[] dst, int offset, int length);

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
        putImpl(position, src, src.position, src.remaining());
        position += src.remaining();
        return this;
    }

    public TShortBuffer put(int index, TShortBuffer src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index > limit() || offset < 0 || offset + length > limit()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(index, src, offset, length);
        return this;
    }

    abstract void putImpl(int index, TShortBuffer src, int offset, int length);

    public TShortBuffer put(short[] src, int offset, int length) {
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

    public TShortBuffer put(int index, short[] src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (length < 0 || offset < 0 || offset + length > src.length || index < 0 || index + length > limit()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(index, src, offset, length);
        return this;
    }

    public TShortBuffer put(int index, short[] src) {
        return put(index, src, 0, src.length);
    }

    public final TShortBuffer put(short[] src) {
        return put(src, 0, src.length);
    }

    abstract void putImpl(int index, short[] src, int offset, int length);

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
        return "[ShortBuffer position=" + position + ", limit=" + limit + ", capacity=" + capacity() + ", mark "
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
