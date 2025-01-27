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

import java.io.IOException;
import java.util.Objects;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.lang.TReadable;
import org.teavm.interop.Address;
import org.teavm.jso.typedarrays.Uint16Array;

public abstract class TCharBuffer extends TBuffer implements Comparable<TCharBuffer>, Appendable,
        CharSequence, TReadable {
    TCharBuffer(int position, int limit) {
        this.position = position;
        this.limit = limit;
    }

    abstract char getChar(int index);

    abstract void putChar(int index, char value);

    public static TCharBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity is negative: " + capacity);
        }
        if (PlatformDetector.isJavaScript()) {
            var array = new char[capacity];
            return new TCharBufferOverTypedArray(0, capacity, false, Uint16Array.fromJavaArray(array), array);
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var array = new char[capacity];
            return new TCharBufferNative(array, 0, capacity, false, array, Address.ofData(array), capacity, false);
        }
        return new TCharBufferOverArray(capacity);
    }

    public static TCharBuffer wrap(char[] array, int offset, int length) {
        if (length < 0 || offset < 0 || length + offset > array.length) {
            throw new IndexOutOfBoundsException();
        }
        if (PlatformDetector.isJavaScript()) {
            var result = new TCharBufferOverTypedArray(0, array.length, false, Uint16Array.fromJavaArray(array), array);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
        if (PlatformDetector.isC() || PlatformDetector.isWebAssembly()) {
            var result = new TCharBufferNative(array, 0, array.length, false, array, Address.ofData(array),
                    array.length, false);
            result.position = offset;
            result.limit = offset + length;
            return result;
        }
        return new TCharBufferOverArray(0, array.length, array, offset, offset + length, false);
    }

    public static TCharBuffer wrap(char[] array) {
        return wrap(array, 0, array.length);
    }

    @Override
    public int read(TCharBuffer target) throws IOException {
        Objects.requireNonNull(target);
        if (!hasRemaining()) {
            return -1;
        }
        int sz = Math.min(remaining(), target.remaining());
        int srcPos = position;
        int dstPos = target.position;
        for (int i = 0; i < sz; ++i) {
            target.putChar(dstPos++, getChar(srcPos++));
        }
        target.position += sz;
        return sz;
    }

    public static TCharBuffer wrap(CharSequence csq, int start, int end) {
        return wrap(csq.toString().toCharArray(), start, end - start);
    }

    public static TCharBuffer wrap(CharSequence csq) {
        return wrap(csq.toString().toCharArray());
    }

    public abstract TCharBuffer slice();

    public abstract TCharBuffer duplicate();

    public abstract TCharBuffer asReadOnlyBuffer();

    public abstract char get();

    public abstract TCharBuffer put(char c);

    public abstract char get(int index);

    public abstract TCharBuffer put(int index, char c);

    public TCharBuffer get(char[] dst, int offset, int length) {
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

    public TCharBuffer get(int index, char[] dst, int offset, int length) {
        if (length < 0 || offset < 0 || offset + length > dst.length || index < 0 || index + length > limit()) {
            throw new IndexOutOfBoundsException();
        }
        getImpl(index, dst, offset, length);
        return this;
    }

    public TCharBuffer get(int index, char[] dst) {
        return get(index, dst, 0, dst.length);
    }

    abstract void getImpl(int index, char[] dst, int offset, int length);

    public TCharBuffer get(char[] dst) {
        return get(dst, 0, dst.length);
    }

    public TCharBuffer put(TCharBuffer src) {
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

    public TCharBuffer put(int index, TCharBuffer src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index > limit() || offset < 0 || offset + length > limit()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(index, src, offset, length);
        return this;
    }

    abstract void putImpl(int index, TCharBuffer src, int offset, int length);

    public TCharBuffer put(char[] src, int offset, int length) {
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

    public TCharBuffer put(int index, char[] src, int offset, int length) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (length < 0 || offset < 0 || offset + length > src.length || index < 0 || index + length > limit()) {
            throw new IndexOutOfBoundsException();
        }
        putImpl(index, src, offset, length);
        return this;
    }

    public TCharBuffer put(int index, char[] src) {
        return put(index, src, 0, src.length);
    }

    public final TCharBuffer put(char[] src) {
        return put(src, 0, src.length);
    }

    abstract void putImpl(int index, char[] src, int offset, int length);

    public TCharBuffer put(String src, int start, int end) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (end < start || start < 0 || end > src.length()) {
            throw new IndexOutOfBoundsException();
        }
        int sz = end - start;
        if (remaining() < sz) {
            throw new TBufferOverflowException();
        }
        int pos = position;
        putImpl(pos, src, start, end);
        position += sz;
        return this;
    }

    public final TCharBuffer put(String src) {
        return put(src, 0, src.length());
    }

    abstract void putImpl(int index, String src, int offset, int length);

    @Override
    public final boolean hasArray() {
        return isArrayPresent();
    }

    @Override
    public final char[] array() {
        return getArray();
    }

    @Override
    public final int arrayOffset() {
        return getArrayOffset();
    }

    abstract boolean isArrayPresent();

    abstract char[] getArray();

    abstract int getArrayOffset();

    public abstract TCharBuffer compact();

    @Override
    public abstract boolean isDirect();

    @Override
    public int hashCode() {
        int hashCode = 0;
        int pos = position;
        for (int i = position; i < limit; ++i) {
            hashCode = 31 * hashCode + getChar(pos++);
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TCharBuffer)) {
            return false;
        }
        TCharBuffer other = (TCharBuffer) obj;
        int sz = remaining();
        if (sz != other.remaining()) {
            return false;
        }
        int a = position;
        int b = other.position;
        for (int i = 0; i < sz; ++i) {
            if (getChar(a++) != other.getChar(b++)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int compareTo(TCharBuffer other) {
        if (this == other) {
            return 0;
        }
        int sz = Math.min(remaining(), other.remaining());
        int a = position;
        int b = other.position;
        for (int i = 0; i < sz; ++i) {
            int r = Character.compare(getChar(a++), other.getChar(b++));
            if (r != 0) {
                return r;
            }
        }
        return Integer.compare(remaining(), other.remaining());
    }

    @Override
    public String toString() {
        char[] chars = new char[limit - position];
        int pos = position;
        for (int i = 0; i < chars.length; ++i) {
            chars[i] = getChar(pos++);
        }
        return new String(chars);
    }

    @Override
    public final int length() {
        return remaining();
    }

    @Override
    public final char charAt(int index) {
        return get(index);
    }

    @Override
    public abstract TCharBuffer subSequence(int start, int end);

    @Override
    public TCharBuffer append(CharSequence csq) {
        return put(csq.toString());
    }

    @Override
    public TCharBuffer append(CharSequence csq, int start, int end) {
        return put(csq.subSequence(start, end).toString());
    }

    @Override
    public TCharBuffer append(char c) {
        return put(c);
    }

    public abstract TByteOrder order();

    @Override
    public final TCharBuffer mark() {
        super.mark();
        return this;
    }

    @Override
    public final TCharBuffer reset() {
        super.reset();
        return this;
    }

    @Override
    public final TCharBuffer clear() {
        super.clear();
        return this;
    }

    @Override
    public final TCharBuffer flip() {
        super.flip();
        return this;
    }

    @Override
    public final TCharBuffer rewind() {
        super.rewind();
        return this;
    }

    @Override
    public TCharBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }

    @Override
    public TCharBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }
}
