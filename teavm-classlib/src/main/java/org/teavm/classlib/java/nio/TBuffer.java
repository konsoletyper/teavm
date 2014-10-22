package org.teavm.classlib.java.nio;

/**
 *
 * @author Alexey Andreev
 */
public abstract class TBuffer {
    int capacity;
    int position;
    int limit;
    int mark = -1;

    TBuffer(int capacity) {
        this.capacity = capacity;
        limit = capacity;
    }

    public final int capacity() {
        return capacity;
    }

    public final int position() {
        return position;
    }

    public final TBuffer position(int newPosition) {
        if (newPosition < 0 || newPosition > limit) {
            throw new IllegalArgumentException("New position " + newPosition + " is outside of range [0;" +
                    limit + "]");
        }
        position = newPosition;
        if (newPosition < mark) {
            mark = 0;
        }
        return this;
    }

    public final int limit() {
        return limit;
    }

    public final TBuffer limit(int newLimit) {
        if (newLimit < 0 || newLimit > capacity) {
            throw new IllegalArgumentException("New limit " + newLimit + " is outside of range [0;" +
                    capacity + "]");
        }
        if (mark > newLimit) {
            mark = -1;
        }
        limit = newLimit;
        if (position > limit) {
            position = limit;
        }
        return this;
    }

    public final TBuffer mark() {
        mark = position;
        return this;
    }

    public final TBuffer reset() {
        if (mark < 0) {
            throw new TInvalidMarkException();
        }
        position = mark;
        return this;
    }

    public final TBuffer clear() {
        position = 0;
        limit = capacity;
        mark = -1;
        return this;
    }

    public final TBuffer flip() {
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }

    public final TBuffer rewind() {
        mark = -1;
        position = 0;
        return this;
    }

    public final int remaining() {
        return limit - position;
    }

    public final boolean hasRemaining() {
        return position < limit;
    }

    public abstract boolean isReadOnly();

    public abstract boolean hasArray();

    public abstract Object array();

    public abstract int arrayOffset();

    public abstract boolean isDirect();
}
