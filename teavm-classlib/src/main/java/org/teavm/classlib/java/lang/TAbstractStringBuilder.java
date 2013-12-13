package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.lang.io.TSerializable;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.javascript.ni.Remove;
import org.teavm.javascript.ni.Rename;

/**
 *
 * @author Alexey Andreev
 */
class TAbstractStringBuilder extends TObject implements TSerializable, TCharSequence {
    private static float[] powersOfTen = { 1E1f, 1E2f, 1E4f, 1E8f, 1E16f, 1E32f };
    private static float[] negPowersOfTen = { 1E-1f, 1E-2f, 1E-4f, 1E-8f, 1E-16f, 1E-32f };
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

    protected TAbstractStringBuilder append(long value) {
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
            long pos = 10;
            int sz = 1;
            while (pos < 1000000000000000000L && pos * 10 <= value) {
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

    protected TAbstractStringBuilder append(float value) {
        boolean negative = false;
        int sz = 10;
        if (value < 0) {
            negative = true;
            value = -value;
            ++sz;
        }
        int exp = 0;
        if (value > 1) {
            int bit = 32;
            exp = 0;
            float digit = 1;
            for (int i = powersOfTen.length - 1; i >= 0; --i) {
                if ((exp | bit) <= 38 && powersOfTen[i] * digit < value) {
                    digit *= powersOfTen[i];
                    exp |= bit;
                }
                bit >>= 1;
            }
            value /= digit;
        } else {
            int bit = 32;
            exp = 0;
            float digit = 1;
            for (int i = negPowersOfTen.length - 1; i >= 0; --i) {
                if ((exp | bit) <= 38 && negPowersOfTen[i] * digit * 10 > value) {
                    digit *= negPowersOfTen[i];
                    exp |= bit;
                }
                bit >>= 1;
            }
            value /= digit;
            exp = -exp;
            ++sz;
        }
        value += 5E-7F;
        if (exp > 10) {
            ++sz;
        }
        ensureCapacity(length + sz);
        if (negative) {
            buffer[length++] = '-';
        }
        int intDigit = (int)value;
        buffer[length++] = (char)('0' + intDigit);
        buffer[length++] = '.';
        value = (value - intDigit) * 10;
        int zeros = 0;
        for (int i = 0; i < 6; ++i) {
            intDigit = (int)value;
            if (intDigit == 0) {
                ++zeros;
            } else {
                zeros = 0;
            }
            buffer[length++] = (char)('0' + intDigit);
            value = (value - intDigit) * 10;
        }
        length -= Math.min(zeros, 5);
        buffer[length++] = 'E';
        if (exp < 0) {
            exp = -exp;
            buffer[length++] = '-';
        }
        if (exp > 10) {
            buffer[length++] = (char)('0' + exp / 10);
        }
        buffer[length++] = (char)('0' + exp % 10);
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

    @Remove
    @Override
    public String toString() {
        return new String(buffer, 0, length);
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
