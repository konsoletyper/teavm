package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.lang.io.TSerializable;
import org.teavm.classlib.java.util.TArrays;

/**
 *
 * @author Alexey Andreev
 */
class AbstractStringBuilder extends TObject implements TSerializable {
    private char[] buffer;
    private int length;

    public AbstractStringBuilder() {
        this(16);
    }

    public AbstractStringBuilder(int capacity) {
        buffer = new char[capacity];
    }

    protected AbstractStringBuilder append(TString string) {
        ensureCapacity(length + string.length());
        int j = length;
        for (int i = 0; i < string.length(); ++i) {
            buffer[j++] = string.charAt(i);
        }
        length = j;
        return this;
    }

    protected AbstractStringBuilder append(int value) {
        if (value < 0) {
            append('-');
            value = -value;
        }
        if (value < 10) {
            append((char)('0' + value));
        } else {
            int pos = 10;
            int sz = 1;
            while (pos <= 1000000000 && pos <= value) {
                pos *= 10;
                ++sz;
            }
            ensureCapacity(length + sz);
            while (pos > 0) {
                buffer[length++] = (char)('0' + value / pos);
                value %= pos;
                pos /= 10;
            }
        }
        return this;
    }

    protected AbstractStringBuilder append(char c) {
        ensureCapacity(length + 1);
        buffer[length++] = c;
        return this;
    }

    private void ensureCapacity(int capacity) {
        if (buffer.length >= capacity) {
            return;
        }
        buffer = TArrays.copyOf(buffer, capacity * 2 + 1);
    }
}
