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
import org.teavm.jso.typedarrays.Float64Array;

public abstract class TDoubleBuffer extends TBuffer implements Comparable<TDoubleBuffer> {
    TDoubleBuffer(int position, int limit) {
        this.position = position;
        this.limit = limit;
    }

    public static TDoubleBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity is negative: " + capacity);
        }
        if (PlatformDetector.isJavaScript()) {
            var array = new double[capacity];
            return new TDoubleBufferOverTypedArray(0, capacity, false, Float64Array.fromJavaArray(array), array);
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var array = new double[capacity];
            return new TDoubleBufferNative(null, array, 0, capacity, false, array, Address.ofData(array),
                    capacity, false);
        }
        return new TDoubleBufferOverArray(capacity);
    }

    public static TDoubleBuffer wrap(double[] array, int offset, int length) {
        if (PlatformDetector.isJavaScript()) {
            var result = new TDoubleBufferOverTypedArray(0, array.length, false, Float64Array.fromJavaArray(array),
                    array);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var result = new TDoubleBufferNative(null, array, 0, array.length, false, array, Address.ofData(array),
                    array.length, false);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
        return new TDoubleBufferOverArray(0, array.length, array, offset, offset + length, false);
    }

    public static TDoubleBuffer wrap(double[] array) {
        return wrap(array, 0, array.length);
    }

    public abstract TDoubleBuffer slice();

    public abstract TDoubleBuffer duplicate();

    public abstract TDoubleBuffer asReadOnlyBuffer();

    public abstract double get();

    public abstract TDoubleBuffer put(double b);

    public abstract double get(int index);

    public abstract TDoubleBuffer put(int index, double b);

    abstract double getElement(int index);

    abstract void putElement(int index, double value);

    public TDoubleBuffer get(double[] dst, int offset, int length) {
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

    public TDoubleBuffer get(int index, double[] dst, int offset, int length) {
        if (length < 0 || offset < 0 || offset + length > dst.length || index < 0 || index + length > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        getImpl(index, dst, offset, length);
        return this;
    }

    public TDoubleBuffer get(int index, double[] dst) {
        return get(index, dst, 0, dst.length);
    }

    abstract void getImpl(int index, double[] dst, int offset, int length);

    public TDoubleBuffer get(double[] dst) {
        return get(dst, 0, dst.length);
    }

    public TDoubleBuffer put(int index, TDoubleBuffer src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index > limit() || offset < 0 || offset + length > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(index, src, offset, length);
        return this;
    }

    abstract void putImpl(int index, TDoubleBuffer src, int offset, int length);

    public TDoubleBuffer put(TDoubleBuffer src) {
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

    public TDoubleBuffer put(double[] src, int offset, int length) {
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

    public TDoubleBuffer put(int index, double[] src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (length < 0 || offset < 0 || offset + length > src.length || index < 0 || index + length > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(index, src, offset, length);
        return this;
    }

    public final TDoubleBuffer put(double[] src) {
        return put(src, 0, src.length);
    }

    public TDoubleBuffer put(int index, double[] src) {
        return put(index, src, 0, src.length);
    }

    abstract void putImpl(int index, double[] src, int offset, int length);

    @Override
    public final boolean hasArray() {
        return isArrayPresent();
    }

    @Override
    public final double[] array() {
        return getArray();
    }

    @Override
    public final int arrayOffset() {
        return getArrayOffset();
    }

    abstract boolean isArrayPresent();

    abstract double[] getArray();

    abstract int getArrayOffset();

    public abstract TDoubleBuffer compact();

    @Override
    public abstract boolean isDirect();

    @Override
    public String toString() {
        return "[DoubleBuffer position=" + position + ", limit=" + limit + ", capacity=" + capacity() + ", mark "
                + (mark >= 0 ? " at " + mark : " is not set") + "]";
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        int pos = position;
        for (int i = position; i < limit; ++i) {
            long elem = Double.doubleToLongBits(getElement(pos++));
            hashCode = 31 * hashCode + (int) elem + (int) (elem >>> 32);
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TDoubleBuffer)) {
            return false;
        }
        TDoubleBuffer other = (TDoubleBuffer) obj;
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
    public int compareTo(TDoubleBuffer other) {
        if (this == other) {
            return 0;
        }
        int sz = Math.min(remaining(), other.remaining());
        int a = position;
        int b = other.position;
        for (int i = 0; i < sz; ++i) {
            int r = Double.compare(getElement(a++), other.getElement(b++));
            if (r != 0) {
                return r;
            }
        }
        return Integer.compare(remaining(), other.remaining());
    }

    public abstract TByteOrder order();

    @Override
    public final TDoubleBuffer mark() {
        super.mark();
        return this;
    }

    @Override
    public final TDoubleBuffer reset() {
        super.reset();
        return this;
    }

    @Override
    public final TDoubleBuffer clear() {
        super.clear();
        return this;
    }

    @Override
    public final TDoubleBuffer flip() {
        super.flip();
        return this;
    }

    @Override
    public final TDoubleBuffer rewind() {
        super.rewind();
        return this;
    }

    @Override
    public TDoubleBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }

    @Override
    public TDoubleBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }
}
