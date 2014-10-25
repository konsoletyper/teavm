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

import org.teavm.classlib.java.io.TIOException;
import org.teavm.classlib.java.lang.TReadable;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public abstract class TCharBuffer extends TBuffer implements Comparable<TCharBuffer>, Appendable,
        CharSequence, TReadable {
    int start;
    char[] array;

    TCharBuffer(int start, int capacity, char[] array, int position, int limit) {
        super(capacity);
        this.start = start;
        this.array = array;
        this.position = position;
        this.limit = limit;
    }

    public static TCharBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity is negative: " + capacity);
        }
        return new TCharBufferImpl(capacity);
    }

    public static TCharBuffer wrap(char[] array, int offset, int length) {
        return new TCharBufferImpl(0, array.length, array, offset, offset + length, false);
    }

    public static TCharBuffer wrap(char[] array) {
        return wrap(array, 0, array.length);
    }

    @Override
    public int read(TCharBuffer target) throws TIOException {
        if (target == null) {
            throw new NullPointerException("Target is null");
        }
        if (target.isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        if (!hasRemaining()) {
            return -1;
        }
        int sz = Math.min(remaining(), target.remaining());
        target.put(array, start + position, sz);
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
        if (offset < 0 || offset >= dst.length) {
            throw new IndexOutOfBoundsException("Offset " + offset + " is outside of range [0;" + dst.length + ")");
        }
        if (offset + length > dst.length) {
            throw new IndexOutOfBoundsException("The last char in dst " + (offset + length) + " is outside " +
                    "of array of size " + dst.length);
        }
        if (remaining() < length) {
            throw new TBufferUnderflowException();
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length " + length + " must be non-negative");
        }
        int pos = position + start;
        for (int i = 0; i < length; ++i) {
            dst[offset++] = array[pos++];
        }
        position += length;
        return this;
    }

    public TCharBuffer get(char[] dst) {
        return get(dst, 0, dst.length);
    }

    public TCharBuffer put(TCharBuffer src) {
        return put(src.array, src.start + src.position, src.remaining());
    }

    public TCharBuffer put(char[] src, int offset, int length) {
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
            throw new IndexOutOfBoundsException("The last char in src " + (offset + length) + " is outside " +
                    "of array of size " + src.length);
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Length " + length + " must be non-negative");
        }
        int pos = position + start;
        for (int i = 0; i < length; ++i) {
            array[pos++] = src[offset++];
        }
        position += length;
        return this;
    }

    public final TCharBuffer put(char[] src) {
        return put(src, 0, src.length);
    }

    public TCharBuffer put(String src, int start, int end) {
        if (isReadOnly()) {
            throw new TReadOnlyBufferException();
        }
        int sz = end - start;
        if (remaining() < sz) {
            throw new TBufferOverflowException();
        }
        if (start < 0 || start >= src.length()) {
            throw new IndexOutOfBoundsException("Start " + start + " is outside of range [0;" + src.length() + ")");
        }
        if (end > src.length()) {
            throw new IndexOutOfBoundsException("The last char in src " + end + " is outside " +
                    "of string of size " + src.length());
        }
        if (start > end) {
            throw new IndexOutOfBoundsException("Start " + start + " must be before end " + end);
        }
        int pos = position + this.start;
        while (start < end) {
            array[pos++] = src.charAt(start++);
        }
        position += sz;
        return this;
    }

    public final TCharBuffer put(String src) {
        return put(src, 0, src.length());
    }

    @Override
    public final boolean hasArray() {
        return true;
    }

    @Override
    public final char[] array() {
        return array;
    }

    @Override
    public final int arrayOffset() {
        return start;
    }

    public abstract TCharBuffer compact();

    @Override
    public abstract boolean isDirect();

    @Override
    public int hashCode() {
        int hashCode = 0;
        int pos = position + start;
        for (int i = position; i < limit; ++i) {
            hashCode = 31 * hashCode + array[pos++];
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
        TCharBuffer other = (TCharBuffer)obj;
        int sz = remaining();
        if (sz != other.remaining()) {
            return false;
        }
        int a = position + start;
        int b = other.position + other.start;
        for (int i = 0; i < sz; ++i) {
            if (array[a++] != other.array[b++]) {
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
        int a = position + start;
        int b = other.position + other.start;
        for (int i = 0; i < sz; ++i) {
            int r = Character.compare(array[a++], other.array[b++]);
            if (r != 0) {
                return r;
            }
        }
        return Integer.compare(remaining(), other.remaining());
    }

    @Override
    public String toString() {
        return new String(array, start + position, limit - position);
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
}
