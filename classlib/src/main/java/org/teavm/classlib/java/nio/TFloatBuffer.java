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
import org.teavm.jso.typedarrays.Float32Array;

public abstract class TFloatBuffer extends TBuffer implements Comparable<TFloatBuffer> {
    TFloatBuffer(int position, int limit) {
        this.position = position;
        this.limit = limit;
    }

    public static TFloatBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity is negative: " + capacity);
        }
        if (PlatformDetector.isJavaScript()) {
            var array = new float[capacity];
            return new TFloatBufferOverTypedArray(0, capacity, false, Float32Array.fromJavaArray(array), array);
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var array = new float[capacity];
            return new TFloatBufferNative(null, array, 0, capacity, false, array, Address.ofData(array),
                    capacity, false);
        }
        return new TFloatBufferOverArray(capacity);
    }

    public static TFloatBuffer wrap(float[] array, int offset, int length) {
        if (PlatformDetector.isJavaScript()) {
            var result = new TFloatBufferOverTypedArray(0, array.length, false, Float32Array.fromJavaArray(array),
                    array);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var result = new TFloatBufferNative(null, array, 0, array.length, false, array, Address.ofData(array),
                    array.length, false);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
        return new TFloatBufferOverArray(0, array.length, array, offset, offset + length, false);
    }

    public static TFloatBuffer wrap(float[] array) {
        return wrap(array, 0, array.length);
    }

    public abstract TFloatBuffer slice();

    public abstract TFloatBuffer duplicate();

    public abstract TFloatBuffer asReadOnlyBuffer();

    public abstract float get();

    public abstract TFloatBuffer put(float b);

    public abstract float get(int index);

    public abstract TFloatBuffer put(int index, float b);

    abstract float getElement(int index);

    abstract void putElement(int index, float value);

    public TFloatBuffer get(float[] dst, int offset, int length) {
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

    public TFloatBuffer get(int index, float[] dst, int offset, int length) {
        if (length < 0 || offset < 0 || offset + length > dst.length || index < 0 || index + length > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        getImpl(index, dst, offset, length);
        return this;
    }

    public TFloatBuffer get(int index, float[] dst) {
        return get(index, dst, 0, dst.length);
    }

    abstract void getImpl(int index, float[] dst, int offset, int length);

    public TFloatBuffer get(float[] dst) {
        return get(dst, 0, dst.length);
    }

    public TFloatBuffer put(int index, TFloatBuffer src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index > limit() || offset < 0 || offset + length > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(index, src, offset, length);
        return this;
    }

    abstract void putImpl(int index, TFloatBuffer src, int offset, int length);

    public TFloatBuffer put(TFloatBuffer src) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        var elementsToTransfer = src.remaining();
        if (remaining() < elementsToTransfer) {
            throw new TBufferOverflowException();
        }
        putImpl(position, src, src.position, elementsToTransfer);
        position += elementsToTransfer;
        src.position += elementsToTransfer;
        return this;
    }

    public TFloatBuffer put(float[] src, int offset, int length) {
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

    public TFloatBuffer put(int index, float[] src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (length < 0 || offset < 0 || offset + length > src.length || index < 0 || index + length > capacity()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(index, src, offset, length);
        return this;
    }

    public final TFloatBuffer put(float[] src) {
        return put(src, 0, src.length);
    }

    public TFloatBuffer put(int index, float[] src) {
        return put(index, src, 0, src.length);
    }

    abstract void putImpl(int index, float[] src, int offset, int length);

    @Override
    public final boolean hasArray() {
        return isArrayPresent();
    }

    @Override
    public final float[] array() {
        return getArray();
    }

    @Override
    public final int arrayOffset() {
        return getArrayOffset();
    }

    abstract boolean isArrayPresent();

    abstract float[] getArray();

    abstract int getArrayOffset();

    public abstract TFloatBuffer compact();

    @Override
    public abstract boolean isDirect();

    @Override
    public String toString() {
        return "[FloatBuffer position=" + position + ", limit=" + limit + ", capacity=" + capacity() + ", mark "
                + (mark >= 0 ? " at " + mark : " is not set") + "]";
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        int pos = position;
        for (int i = position; i < limit; ++i) {
            hashCode = 31 * hashCode + Float.floatToIntBits(getElement(pos++));
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TFloatBuffer)) {
            return false;
        }
        TFloatBuffer other = (TFloatBuffer) obj;
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
    public int compareTo(TFloatBuffer other) {
        if (this == other) {
            return 0;
        }
        int sz = Math.min(remaining(), other.remaining());
        int a = position;
        int b = other.position;
        for (int i = 0; i < sz; ++i) {
            int r = Float.compare(getElement(a++), other.getElement(b++));
            if (r != 0) {
                return r;
            }
        }
        return Integer.compare(remaining(), other.remaining());
    }

    public abstract TByteOrder order();

    @Override
    public final TFloatBuffer mark() {
        super.mark();
        return this;
    }

    @Override
    public final TFloatBuffer reset() {
        super.reset();
        return this;
    }

    @Override
    public final TFloatBuffer clear() {
        super.clear();
        return this;
    }

    @Override
    public final TFloatBuffer flip() {
        super.flip();
        return this;
    }

    @Override
    public final TFloatBuffer rewind() {
        super.rewind();
        return this;
    }

    @Override
    public TFloatBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }

    @Override
    public TFloatBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }
}
