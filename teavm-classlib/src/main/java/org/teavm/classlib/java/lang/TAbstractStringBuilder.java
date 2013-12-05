package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.lang.io.TSerializable;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.javascript.ni.Rename;

/**
 *
 * @author Alexey Andreev
 */
class TAbstractStringBuilder extends TObject implements TSerializable, TCharSequence {
    char[] buffer;
    int length;

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

    protected TAbstractStringBuilder appendCodePoint(int codePoint) {
        if (codePoint < TString.SUPPLEMENTARY_PLANE) {
            return append((char)codePoint);
        }
        ensureCapacity(length + 2);
        codePoint -= TString.SUPPLEMENTARY_PLANE;
        buffer[length++] = TString.highSurrogate(codePoint);
        buffer[length++] = TString.lowSurrogate(codePoint);
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

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= length) {
            throw new TIndexOutOfBoundsException();
        }
        return buffer[index];
    }

    public TAbstractStringBuilder append(TCharSequence s, int start, int end) {
        if (start > end || end > s.length() || start < 0) {
            throw new TIndexOutOfBoundsException();
        }
        ensureCapacity(end - start);
        for (int i = start; i < end; ++i) {
            buffer[length++] = s.charAt(i);
        }
        return this;
    }

    public TAbstractStringBuilder append(TCharSequence s) {
        return append(s, 0, s.length());
    }

    @Override
    public TCharSequence subSequence(int start, int end) {
        // TODO: implement
        throw new TUnsupportedOperationException();
    }
}
