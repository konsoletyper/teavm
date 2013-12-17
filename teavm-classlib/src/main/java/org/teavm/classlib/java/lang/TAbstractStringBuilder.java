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
    private static final float[] powersOfTen = { 1E1f, 1E2f, 1E4f, 1E8f, 1E16f, 1E32f };
    private static final float[] negPowersOfTen = { 1E-1f, 1E-2f, 1E-4f, 1E-8f, 1E-16f, 1E-32f };
    private static final int[] intPowersOfTen = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000,
            1000000000 };
    private static final int FLOAT_DECIMAL_PRECISION = 7;
    private static final float FLOAT_DECIMAL_FACTOR = 1E6f;
    private static final int FLOAT_MAX_EXPONENT = 38;
    private static final int FLOAT_MAX_POS = 1000000;
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
        if (value == 0) {
            ensureCapacity(length + 3);
            buffer[length++] = '0';
            buffer[length++] = '.';
            buffer[length++] = '0';
            return this;
        } else if (value == -0) {
            ensureCapacity(length + 4);
            buffer[length++] = '-';
            buffer[length++] = '0';
            buffer[length++] = '.';
            buffer[length++] = '0';
            return this;
        } else if (Float.isNaN(value)) {
            ensureCapacity(length + 3);
            buffer[length++] = 'N';
            buffer[length++] = 'a';
            buffer[length++] = 'N';
            return this;
        } else if (Float.isInfinite(value)) {
            if (value > 0) {
                ensureCapacity(8);
            } else {
                ensureCapacity(9);
                buffer[length++] = '-';
            }
            buffer[length++] = 'I';
            buffer[length++] = 'n';
            buffer[length++] = 'f';
            buffer[length++] = 'i';
            buffer[length++] = 'n';
            buffer[length++] = 'i';
            buffer[length++] = 't';
            buffer[length++] = 'y';
            return this;
        }
        // Get absolute value
        boolean negative = false;
        int sz = 1; // Decimal point always included
        if (value < 0) {
            negative = true;
            value = -value;
            ++sz; // including '-' sign of mantissa
        }

        // Split into decimal mantissa and decimal exponent
        int exp = 0;
        int mantissa = 0;
        int intPart = 1;
        int digits = 0;
        if (value >= 1) {
            int bit = 32;
            exp = 0;
            float digit = 1;
            for (int i = powersOfTen.length - 1; i >= 0; --i) {
                if ((exp | bit) <= FLOAT_MAX_EXPONENT && powersOfTen[i] * digit  < value) {
                    digit *= powersOfTen[i];
                    exp |= bit;
                }
                bit >>= 1;
            }
            mantissa = (int)((value / (digit / FLOAT_DECIMAL_FACTOR)) + 0.5f);
        } else {
            ++sz;
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
            exp = -exp;
            mantissa = (int)(((value * FLOAT_MAX_POS) / digit) + 0.5f);
        }

        // Remove trailing zeros
        digits = FLOAT_DECIMAL_PRECISION;
        int zeros = trailingDecimalZeros(mantissa);
        if (zeros > 0) {
            digits -= zeros;
        }

        // Handle special case of exponent close to 0
        if (exp < 7 && exp >= -3) {
            if (exp >= 0) {
                intPart = exp + 1;
                digits = Math.max(digits, intPart + 1);
                exp = 0;
            } else if (exp < 0) {
                mantissa /= intPowersOfTen[-exp];
                digits -= exp;
                exp = 0;
            }
        }
        sz += digits;

        // Extend buffer to store exponent
        if (exp != 0) {
            sz += 2;
            if (exp < 10 || exp > 10) {
                ++sz;
            }
        }

        // Print mantissa
        ensureCapacity(length + sz);
        if (negative) {
            buffer[length++] = '-';
        }
        int pos = FLOAT_MAX_POS;
        for (int i = 0; i < digits; ++i) {
            int intDigit;
            if (pos > 0) {
                intDigit = mantissa / pos;
                mantissa %= pos;
            } else {
                intDigit = 0;
            }
            buffer[length++] = (char)('0' + intDigit);
            value = (value - intDigit) * 10;
            if (--intPart == 0) {
                buffer[length++] = '.';
            }
            pos /= 10;
        }

        // Print exponent
        if (exp != 0) {
            buffer[length++] = 'E';
            if (exp < 0) {
                exp = -exp;
                buffer[length++] = '-';
            }
            if (exp > 10) {
                buffer[length++] = (char)('0' + exp / 10);
            }
            buffer[length++] = (char)('0' + exp % 10);
        }
        return this;
    }

    private static int trailingDecimalZeros(int n) {
        if (n % 1000000000 == 0) {
            return 9;
        }
        int result = 0;
        int zeros = 1;
        if (n % 100000000 == 0) {
            result |= 8;
            zeros *= 100000000;
        }
        if (n % (zeros * 10000) == 0) {
            result |= 4;
            zeros *= 10000;
        }
        if (n % (zeros * 100) == 0) {
            result |= 2;
            zeros *= 100;
        }
        if (n % (zeros * 10) == 0) {
            result |= 1;
        }
        return result;
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

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin > srcEnd) {
            throw new TIndexOutOfBoundsException(TString.wrap("Index out of bounds"));
        }
        while (srcBegin < srcEnd) {
            dst[dstBegin++] = buffer[srcBegin++];
        }
    }

    public void setLength(int newLength) {
        length = 0;
    }
}
