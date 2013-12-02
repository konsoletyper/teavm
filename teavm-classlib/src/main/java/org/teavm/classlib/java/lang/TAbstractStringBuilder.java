package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.lang.io.TSerializable;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.javascript.ni.Rename;

/**
 *
 * @author Alexey Andreev
 */
class TAbstractStringBuilder extends TObject implements TSerializable {
    private char[] buffer;
    private int length;

    public TAbstractStringBuilder() {
        this(16);
    }

    public TAbstractStringBuilder(int capacity) {
        buffer = new char[capacity];
    }

    protected TAbstractStringBuilder append(TString string) {
        ensureCapacity(length + string.length());
        int j = length;
        for (int i = 0; i < string.length(); ++i) {
            buffer[j++] = string.charAt(i);
        }
        length = j;
        return this;
    }

    protected TAbstractStringBuilder append(int value) {
        boolean positive = true;
        if (value < 0) {
            positive = false;
            value = -value;
        }
        if (value < 10) {
            if (!positive) {
                ensureCapacity(length + 2);
                buffer[length++] = '-';
            } else {
                ensureCapacity(length + 1);
            }
            buffer[length++] = (char)('0' + value);
        } else {
            int pos = 10;
            int sz = 1;
            while (pos < 1000000000 && pos * 10 <= value) {
                pos *= 10;
                ++sz;
            }
            if (!positive) {
                ++sz;
            }
            ensureCapacity(length + sz);
            if (!positive) {
                buffer[length++] = '-';
            }
            while (pos > 0) {
                buffer[length++] = (char)('0' + value / pos);
                value %= pos;
                pos /= 10;
            }
        }
        return this;
    }

    protected TAbstractStringBuilder append(char c) {
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

    @Override
    @Rename("toString")
    public TString toString0() {
        return new TString(buffer, 0, length);
    }
}
