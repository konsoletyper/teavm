package org.teavm.classlib.java.nio;

import org.teavm.classlib.java.lang.TComparable;

/**
 *
 * @author Alexey Andreev
 */
public abstract class TByteBuffer extends TBuffer implements TComparable<TByteBuffer> {
    int start;
    byte[] array;
    TByteOrder order = TByteOrder.BIG_ENDIAN;

    TByteBuffer(int start, int capacity, byte[] array, int position, int limit) {
        super(capacity);
        this.start = start;
        this.array = array;
        this.position = position;
        this.limit = limit;
    }

    public static TByteBuffer allocateDirect(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity is negative: " + capacity);
        }
        return new TByteBufferImpl(capacity, true);
    }

    public static TByteBuffer allocate(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity is negative: " + capacity);
        }
        return new TByteBufferImpl(capacity, false);
    }

    public static TByteBuffer wrap(byte[] array, int offset, int length) {
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
        if (offset < 0 || offset >= dst.length) {
            throw new IndexOutOfBoundsException("Offset " + offset + " is outside of range [0;" + dst.length + ")");
        }
        if (offset + length > dst.length) {
            throw new IndexOutOfBoundsException("The last byte in dst " + (offset + length) + " is outside " +
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

    public TByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    public TByteBuffer put(TByteBuffer src) {
        return put(src.array, src.start + src.position, src.remaining());
    }

    public TByteBuffer put(byte[] src, int offset, int length) {
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
            throw new IndexOutOfBoundsException("The last byte in src " + (offset + length) + " is outside " +
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

    public final TByteBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    @Override
    public boolean hasArray() {
        return true;
    }

    @Override
    public final byte[] array() {
        return array;
    }

    @Override
    public int arrayOffset() {
        return start;
    }

    public abstract TByteBuffer compact();

    @Override
    public abstract boolean isDirect();

    @Override
    public String toString() {
        return "[ByteBuffer position=" + position + ", limit=" + limit + ", capacity=" + capacity + ", mark " +
                (mark >= 0 ? " at " + mark : " is not set") + "]";
    }

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
        if (!(obj instanceof TByteBuffer)) {
            return false;
        }
        TByteBuffer other = (TByteBuffer)obj;
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
    public int compareTo(TByteBuffer other) {
        if (this == other) {
            return 0;
        }
        int sz = Math.min(remaining(), other.remaining());
        int a = position + start;
        int b = other.position + other.start;
        for (int i = 0; i < sz; ++i) {
            int r = Byte.compare(array[a++], other.array[b++]);
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
        return this;
    }
}
