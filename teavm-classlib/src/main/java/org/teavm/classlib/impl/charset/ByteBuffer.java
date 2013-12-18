package org.teavm.classlib.impl.charset;

/**
 *
 * @author Alexey Andreev
 */
public class ByteBuffer {
    private byte[] data;
    private int end;
    private int pos;

    public ByteBuffer(byte[] data) {
        this(data, 0, data.length);
    }

    public ByteBuffer(byte[] data, int start, int end) {
        this.data = data;
        this.end = end;
        this.pos = start;
    }

    public void put(byte b) {
        data[pos++] = b;
    }

    public void rewind(int start) {
        this.pos = start;
    }

    public int available() {
        return end - pos;
    }

    public void back(int count) {
        pos -= count;
    }

    public boolean end() {
        return pos == end;
    }

    public byte get() {
        return data[pos++];
    }

    public int position() {
        return pos;
    }

    public void put(ByteBuffer buffer) {
        while (buffer.pos < buffer.end) {
            data[pos++] = buffer.data[buffer.pos++];
        }
    }
}
