package org.teavm.classlib.impl.charset;

/**
 *
 * @author Alexey Andreev
 */
public class CharBuffer {
    private char[] data;
    private int end;
    private int pos;

    public CharBuffer(char[] data, int start, int end) {
        this.data = data;
        this.end = end;
        this.pos = start;
    }

    public CharBuffer(char[] data) {
        this(data, 0, data.length);
    }

    public void put(char b) {
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

    public char get() {
        return data[pos++];
    }

    public int position() {
        return pos;
    }

    public void put(CharBuffer buffer) {
        while (buffer.pos < buffer.end) {
            data[pos++] = buffer.data[buffer.pos++];
        }
    }
}
