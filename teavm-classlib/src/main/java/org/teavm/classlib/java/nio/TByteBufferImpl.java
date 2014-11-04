package org.teavm.classlib.java.nio;

/**
 *
 * @author Alexey Andreev
 */
class TByteBufferImpl extends TByteBuffer {
    private boolean direct;
    private boolean readOnly;

    public TByteBufferImpl(int capacity, boolean direct) {
        this(0, capacity, new byte[capacity], 0, capacity, direct, false);
    }

    public TByteBufferImpl(int start, int capacity, byte[] array, int position, int limit,
            boolean direct, boolean readOnly) {
        super(start, capacity, array, position, limit);
        this.direct = direct;
        this.readOnly = readOnly;
    }

    @Override
    public TByteBuffer slice() {
        return new TByteBufferImpl(position, limit - position,  array, 0, limit - position, direct, readOnly);
    }

    @Override
    public TByteBuffer duplicate() {
        return new TByteBufferImpl(start, capacity, array, position, limit, direct, readOnly);
    }

    @Override
    public TByteBuffer asReadOnlyBuffer() {
        return new TByteBufferImpl(start, capacity, array, position, limit, direct, true);
    }

    @Override
    public byte get() {
        if (position >= limit) {
            throw new TBufferUnderflowException();
        }
        return array[start + position++];
    }

    @Override
    public TByteBuffer put(byte b) {
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
    public byte get(int index) {
        if (index < 0 || index >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + limit + ")");
        }
        return array[start + index];
    }

    @Override
    public TByteBuffer put(int index, byte b) {
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
    public TByteBuffer compact() {
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
        return direct;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public char getChar() {
        if (position + 1 >= limit) {
            throw new TBufferUnderflowException();
        }
        int a = array[start + position] & 0xFF;
        int b = array[start + position + 1] & 0xFF;
        position += 2;
        if (order == TByteOrder.BIG_ENDIAN) {
            return (char)(a << 8 | b);
        } else {
            return (char)(b << 8 | a);
        }
    }

    @Override
    public TByteBuffer putChar(char value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (position + 1 >= limit) {
            throw new TBufferOverflowException();
        }
        if (order == TByteOrder.BIG_ENDIAN) {
            array[start + position++] = (byte)(value >> 8);
            array[start + position++] = (byte)value;
        } else {
            array[start + position++] = (byte)value;
            array[start + position++] = (byte)(value >> 8);
        }
        return this;
    }

    @Override
    public char getChar(int index) {
        if (index < 0 || index + 1 >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + (limit - 1) + ")");
        }
        int a = array[start + index] & 0xFF;
        int b = array[start + index + 1] & 0xFF;
        if (order == TByteOrder.BIG_ENDIAN) {
            return (char)(a << 8 | b);
        } else {
            return (char)(b << 8 | a);
        }
    }

    @Override
    public TByteBuffer putChar(int index, char value) {
        if (readOnly) {
            throw new TReadOnlyBufferException();
        }
        if (index < 0 || index + 1 >= limit) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of range [0;" + (limit - 1) + ")");
        }
        if (order == TByteOrder.BIG_ENDIAN) {
            array[start + index] = (byte)(value >> 8);
            array[start + index + 1] = (byte)value;
        } else {
            array[start + index] = (byte)value;
            array[start + index + 1] = (byte)(value >> 8);
        }
        return this;
    }

    @Override
    public TCharBuffer asCharBuffer() {
        int sz = remaining() / 2;
        if (order == TByteOrder.BIG_ENDIAN) {
            return new TCharBufferOverByteBufferBigEndian(start + position, sz, this, 0, sz, isReadOnly());
        } else {
            return new TCharBufferOverByteBufferLittleEndian(start + position, sz, this, 0, sz, isReadOnly());
        }
    }

    @Override
    public TShortBuffer asShortBuffer() {
        int sz = remaining() / 2;
        if (order == TByteOrder.BIG_ENDIAN) {
            return new TShortBufferOverByteBufferBigEndian(start + position, sz, this, 0, sz, isReadOnly());
        } else {
            return new TShortBufferOverByteBufferLittleEndian(start + position, sz, this, 0, sz, isReadOnly());
        }
    }

    @Override
    public TIntBuffer asIntBuffer() {
        int sz = remaining() / 4;
        if (order == TByteOrder.BIG_ENDIAN) {
            return new TIntBufferOverByteBufferBigEndian(start + position, sz, this, 0, sz, isReadOnly());
        } else {
            return new TIntBufferOverByteBufferLittleEndian(start + position, sz, this, 0, sz, isReadOnly());
        }
    }

    @Override
    public TLongBuffer asLongBuffer() {
        int sz = remaining() / 8;
        if (order == TByteOrder.BIG_ENDIAN) {
            return new TLongBufferOverByteBufferBigEndian(start + position, sz, this, 0, sz, isReadOnly());
        } else {
            return new TLongBufferOverByteBufferLittleEndian(start + position, sz, this, 0, sz, isReadOnly());
        }
    }

    @Override
    public TFloatBuffer asFloatBuffer() {
        int sz = remaining() / 4;
        if (order == TByteOrder.LITTLE_ENDIAN) {
            return new TFloatBufferOverByteBufferBigEndian(start + position, sz, this, 0, sz, isReadOnly());
        } else {
            return new TFloatBufferOverByteBufferLittleEndian(start + position, sz, this, 0, sz, isReadOnly());
        }
    }

    @Override
    public TDoubleBuffer asDoubleBuffer() {
        int sz = remaining() / 8;
        TDoubleBufferOverByteBuffer result = new TDoubleBufferOverByteBuffer(start + position, sz, this, 0, sz,
                isReadOnly());
        result.byteOrder = order;
        return result;
    }
}
