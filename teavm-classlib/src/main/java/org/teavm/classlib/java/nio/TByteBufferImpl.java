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
}
