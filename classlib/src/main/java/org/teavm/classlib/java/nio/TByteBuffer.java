/*
 *  Copyright 2015 Alexey Andreev.
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

import java.nio.BufferUnderflowException;
import java.util.Objects;
import org.teavm.backend.c.runtime.Memory;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.lang.TOutOfMemoryError;
import org.teavm.interop.Address;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.runtime.GC;
import org.teavm.runtime.heap.Heap;

public abstract class TByteBuffer extends TBuffer implements TComparable<TByteBuffer> {
    TByteOrder order = TByteOrder.BIG_ENDIAN;

    TByteBuffer() {
    }

    public static TByteBuffer allocateDirect(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity is negative: " + capacity);
        }
        if (PlatformDetector.isJavaScript()) {
            var result = new TByteBufferJsImpl(null, 0, new Int8Array(capacity), true, false);
            result.limit = capacity;
            return result;
        }
        if (PlatformDetector.isC()) {
            var memory = Memory.malloc(capacity);
            var result = new TByteBufferNative(null, 0, null, memory, capacity, false);
            GC.registerDirectBuffer(Address.ofObject(result).toStructure());
            result.limit = capacity;
            return result;
        }
        if (PlatformDetector.isWebAssembly()) {
            var array = new byte[capacity];
            var result = new TByteBufferNative(array, 0, array, Address.ofData(array), array.length, false);
            result.limit = capacity;
            return result;
        }
        if (PlatformDetector.isWebAssemblyGC()) {
            var addr = Heap.alloc(capacity);
            if (addr == null) {
                throw new TOutOfMemoryError();
            }
            var result = new TByteBufferWasmGC(null, 0, null, addr, capacity, false);
            result.limit = capacity;
            TJSBufferHelper.WasmGC.register(result, addr);
            return result;
        }
        return new TByteBufferImpl(capacity, true);
    }

    public static TByteBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity is negative: " + capacity);
        }
        if (PlatformDetector.isJavaScript()) {
            var array = new byte[capacity];
            var result = new TByteBufferJsImpl(array, 0, Int8Array.fromJavaArray(array), false, false);
            result.limit = capacity;
            return result;
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var array = new byte[capacity];
            var result = new TByteBufferNative(array, 0, array, Address.ofData(array), array.length, false);
            result.limit = capacity;
            return result;
        }
        return new TByteBufferImpl(capacity, false);
    }

    public static TByteBuffer wrap(byte[] array, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, array.length);
        if (PlatformDetector.isJavaScript()) {
            var data = Int8Array.fromJavaArray(array);
            var result = new TByteBufferJsImpl(array, 0, data, false, false);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var result = new TByteBufferNative(array, 0, array, Address.ofData(array), array.length, false);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
        return new TByteBufferImpl(0, array.length, array, offset, offset + length, false, false);
    }

    public static TByteBuffer wrap(byte[] array) {
        return wrap(array, 0, array.length);
    }

    public abstract TByteBuffer slice();

    public abstract TByteBuffer duplicate();

    public abstract TByteBuffer asReadOnlyBuffer();

    public abstract byte get();

    public abstract TByteBuffer put(byte b);

    public abstract byte get(int index);

    public abstract TByteBuffer put(int index, byte b);

    public TByteBuffer get(byte[] dst, int offset, int length) {
        if (length < 0 || offset < 0 || offset + length > dst.length) {
            throw new IndexOutOfBoundsException();
        }
        if (length > remaining()) {
            throw new BufferUnderflowException();
        }
        getImpl(position, dst, offset, length);
        position += length;
        return this;
    }

    public TByteBuffer get(int index, byte[] dst) {
        return get(index, dst, 0, dst.length);
    }

    public TByteBuffer get(int index, byte[] dst, int offset, int length) {
        if (length < 0 || offset < 0 || offset + length > dst.length || index < 0 || index + length > limit()) {
            throw new IndexOutOfBoundsException();
        }
        getImpl(index, dst, offset, length);
        return this;
    }

    abstract void getImpl(int index, byte[] dst, int offset, int length);

    public TByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    public TByteBuffer put(TByteBuffer src) {
        return put(position(), src, src.position(), src.remaining());
    }

    public TByteBuffer put(int index, TByteBuffer src, int offset, int length) {
        if (src.remaining() == 0) {
            return this;
        }
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (length < 0 || offset < 0 || offset + length > src.limit() || index < 0 || index + length > limit()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(index, src, offset, length);
        return this;
    }

    abstract void putImpl(int index, TByteBuffer src, int offset, int length);

    public TByteBuffer put(byte[] src, int offset, int length) {
        if (length == 0) {
            return this;
        }
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (length < 0 || offset < 0 || offset + length > src.length) {
            throw new IndexOutOfBoundsException();
        }
        if (length > remaining()) {
            throw new TBufferOverflowException();
        }
        putImpl(src, offset, position, length);
        position += length;
        return this;
    }

    public final TByteBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    public TByteBuffer put(int index, byte[] src) {
        return put(index, src, 0, src.length);
    }

    public TByteBuffer put(int index, byte[] src, int offset, int length) {
        if (length == 0) {
            return this;
        }
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (length < 0 || offset < 0 || offset + length > src.length || index < 0 || index + length > limit()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(src, offset, index, length);
        return this;
    }

    abstract void putImpl(byte[] src, int srcOffset, int destOffset, int length);

    @Override
    public final boolean hasArray() {
        return hasArrayImpl();
    }

    @Override
    public final byte[] array() {
        return arrayImpl();
    }

    @Override
    public final int arrayOffset() {
        return arrayOffsetImpl();
    }

    abstract boolean hasArrayImpl();

    abstract byte[] arrayImpl();

    abstract int arrayOffsetImpl();

    public abstract TByteBuffer compact();

    @Override
    public abstract boolean isDirect();

    @Override
    public String toString() {
        return "[ByteBuffer position=" + position + ", limit=" + limit + ", capacity=" + capacity() + ", mark "
                + (mark >= 0 ? " at " + mark : " is not set") + "]";
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        int pos = position();
        for (int i = position(); i < limit; ++i) {
            hashCode = 31 * hashCode + get(pos);
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TByteBuffer)) {
            return false;
        }
        TByteBuffer other = (TByteBuffer) obj;
        int sz = remaining();
        if (sz != other.remaining()) {
            return false;
        }
        int a = position();
        int b = other.position();
        for (int i = 0; i < sz; ++i) {
            if (get(a++) != other.get(b++)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int compareTo(TByteBuffer other) {
        if (this == other) {
            return 0;
        }
        int sz = Math.min(remaining(), other.remaining());
        int a = position();
        int b = other.position();
        for (int i = 0; i < sz; ++i) {
            int r = Byte.compare(get(a++), other.get(b++));
            if (r != 0) {
                return r;
            }
        }
        return Integer.compare(remaining(), other.remaining());
    }

    public final TByteOrder order() {
        return order;
    }

    public final TByteBuffer order(TByteOrder bo) {
        order = bo;
        onOrderChanged();
        return this;
    }

    void onOrderChanged() {
    }

    public abstract char getChar();

    public abstract TByteBuffer putChar(char value);

    public abstract char getChar(int index);

    public abstract TByteBuffer putChar(int index, char value);

    public abstract TCharBuffer asCharBuffer();

    public abstract short getShort();

    public abstract TByteBuffer putShort(short value);

    public abstract short getShort(int index);

    public abstract TByteBuffer putShort(int index, short value);

    public abstract TShortBuffer asShortBuffer();

    public abstract int getInt();

    public abstract TByteBuffer putInt(int value);

    public abstract int getInt(int index);

    public abstract TByteBuffer putInt(int index, int value);

    public abstract TIntBuffer asIntBuffer();

    public abstract long getLong();

    public abstract TByteBuffer putLong(long value);

    public abstract long getLong(int index);

    public abstract TByteBuffer putLong(int index, long value);

    public abstract TLongBuffer asLongBuffer();

    public abstract float getFloat();

    public abstract TByteBuffer putFloat(float value);

    public abstract float getFloat(int index);

    public abstract TByteBuffer putFloat(int index, float value);

    public abstract TFloatBuffer asFloatBuffer();

    public abstract double getDouble();

    public abstract TByteBuffer putDouble(double value);

    public abstract double getDouble(int index);

    public abstract TByteBuffer putDouble(int index, double value);

    public abstract TDoubleBuffer asDoubleBuffer();

    @Override
    public final TByteBuffer mark() {
        super.mark();
        return this;
    }

    @Override
    public final TByteBuffer reset() {
        super.reset();
        return this;
    }

    @Override
    public final TByteBuffer clear() {
        super.clear();
        return this;
    }

    @Override
    public final TByteBuffer flip() {
        super.flip();
        return this;
    }

    @Override
    public final TByteBuffer rewind() {
        super.rewind();
        return this;
    }

    @Override
    public TByteBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }

    @Override
    public TByteBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }
}
