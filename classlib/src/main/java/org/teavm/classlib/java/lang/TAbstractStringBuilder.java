/*
 *  Copyright 2013 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.util.TArrays;

class TAbstractStringBuilder extends TObject implements TSerializable, TCharSequence {
    private static float[] powersOfTen = { 1E1f, 1E2f, 1E4f, 1E8f, 1E16f, 1E32f };
    private static double[] doublePowersOfTen = { 1E1, 1E2, 1E4, 1E8, 1E16, 1E32, 1E64, 1E128, 1E256 };
    private static float[] negPowersOfTen = { 1E-1f, 1E-2f, 1E-4f, 1E-8f, 1E-16f, 1E-32f };
    private static double[] negDoublePowersOfTen = { 1E-1, 1E-2, 1E-4, 1E-8, 1E-16, 1E-32,
            1E-64, 1E-128, 1E-256 };
    private static int[] intPowersOfTen = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000,
            1000000000 };
    private static long[] longPowersOfTen = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000,
            1000000000, 10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L,
            1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L };
    private static final long[] longLogPowersOfTen = { 1, 10, 100, 10000, 100000000, 10000000000000000L, };
    private static final int FLOAT_DECIMAL_PRECISION = 7;
    private static final int DOUBLE_DECIMAL_PRECISION = 16;
    private static final float FLOAT_DECIMAL_FACTOR = 1E6f;
    private static final double DOUBLE_DECIMAL_FACTOR = 1E15;
    private static final int FLOAT_MAX_EXPONENT = 38;
    private static final int DOUBLE_MAX_EXPONENT = 308;
    private static final int FLOAT_MAX_POS = 1000000;
    private static final long DOUBLE_MAX_POS = 1000000000000000L;
    char[] buffer;
    private int length;

    public TAbstractStringBuilder() {
        this(16);
    }

    public TAbstractStringBuilder(int capacity) {
        buffer = new char[capacity];
    }

    public TAbstractStringBuilder(TString value) {
        this((TCharSequence) value);
    }

    public TAbstractStringBuilder(TCharSequence value) {
        buffer = new char[value.length()];
        for (int i = 0; i < buffer.length; ++i) {
            buffer[i] = value.charAt(i);
        }
        length = value.length();
    }

    protected TAbstractStringBuilder append(TString string) {
        return insert(length, string);
    }

    protected TAbstractStringBuilder insert(int index, TString string) {
        if (index < 0 || index > length) {
            throw new TStringIndexOutOfBoundsException();
        }
        if (string == null) {
            string = TString.wrap("null");
        } else if (string.isEmpty()) {
            return this;
        }
        ensureCapacity(length + string.length());
        for (int i = length - 1; i >= index; --i) {
            buffer[i + string.length()] = buffer[i];
        }
        length += string.length();
        int j = index;
        for (int i = 0; i < string.length(); ++i) {
            buffer[j++] = string.charAt(i);
        }
        return this;
    }

    protected TAbstractStringBuilder append(int value) {
        return append(value, 10);
    }

    protected TAbstractStringBuilder insert(int index, int value) {
        return insert(index, value, 10);
    }

    TAbstractStringBuilder append(int value, int radix) {
        return insert(length, value, radix);
    }

    TAbstractStringBuilder insert(int target, int value, int radix) {
        boolean positive = true;
        if (value < 0) {
            positive = false;
            value = -value;
        }
        if (value < radix) {
            if (!positive) {
                insertSpace(target, target + 2);
                buffer[target++] = '-';
            } else {
                insertSpace(target, target + 1);
            }
            buffer[target++] = TCharacter.forDigit(value, radix);
        } else {
            int pos = 1;
            int sz = 1;
            int posLimit = TInteger.MAX_VALUE / radix;
            while (pos * radix <= value) {
                pos *= radix;
                ++sz;
                if (pos > posLimit) {
                    break;
                }
            }
            if (!positive) {
                ++sz;
            }
            insertSpace(target, target + sz);
            if (!positive) {
                buffer[target++] = '-';
            }
            while (pos > 0) {
                buffer[target++] = TCharacter.forDigit(value / pos, radix);
                value %= pos;
                pos /= radix;
            }
        }
        return this;
    }

    protected TAbstractStringBuilder append(long value) {
        return insert(length, value);
    }

    protected TAbstractStringBuilder insert(int target, long value) {
        return insert(target, value, 10);
    }

    protected TAbstractStringBuilder insert(int target, long value, int radix) {
        boolean positive = true;
        if (value < 0) {
            positive = false;
            value = -value;
        }
        if (value < radix) {
            if (!positive) {
                insertSpace(target, target + 2);
                buffer[target++] = '-';
            } else {
                insertSpace(target, target + 1);
            }
            buffer[target++] = Character.forDigit((int) value, radix);
        } else {
            int sz = 1;
            long pos = 1;
            while (pos * radix > pos && pos * radix <= value) {
                pos *= radix;
                ++sz;
            }
            if (!positive) {
                ++sz;
            }
            insertSpace(target, target + sz);
            if (!positive) {
                buffer[target++] = '-';
            }
            while (pos > 0) {
                buffer[target++] = TCharacter.forDigit((int) (value / pos), radix);
                value %= pos;
                pos /= radix;
            }
        }
        return this;
    }

    protected TAbstractStringBuilder append(float value) {
        return insert(length, value);
    }

    protected TAbstractStringBuilder insert(int target, float value) {
        if (value == 0) {
            insertSpace(target, target + 3);
            buffer[target++] = '0';
            buffer[target++] = '.';
            buffer[target++] = '0';
            return this;
        } else if (value == -0) {
            insertSpace(target, target + 4);
            buffer[target++] = '-';
            buffer[target++] = '0';
            buffer[target++] = '.';
            buffer[target++] = '0';
            return this;
        } else if (TFloat.isNaN(value)) {
            insertSpace(target, target + 3);
            buffer[target++] = 'N';
            buffer[target++] = 'a';
            buffer[target++] = 'N';
            return this;
        } else if (TFloat.isInfinite(value)) {
            if (value > 0) {
                insertSpace(target, target + 8);
            } else {
                insertSpace(target, target + 9);
                buffer[target++] = '-';
            }
            buffer[target++] = 'I';
            buffer[target++] = 'n';
            buffer[target++] = 'f';
            buffer[target++] = 'i';
            buffer[target++] = 'n';
            buffer[target++] = 'i';
            buffer[target++] = 't';
            buffer[target++] = 'y';
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
                if ((exp | bit) <= FLOAT_MAX_EXPONENT && powersOfTen[i] * digit <= value) {
                    digit *= powersOfTen[i];
                    exp |= bit;
                }
                bit >>= 1;
            }
            mantissa = (int) ((value / (digit / FLOAT_DECIMAL_FACTOR)) + 0.5f);
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
            exp = -exp;
            mantissa = (int) (((value * FLOAT_MAX_POS) / digit) + 0.5f);

            while (mantissa >= 10000000) {
                mantissa /= 10;
                exp--;
            }
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

        // Extend buffer to store exponent
        if (exp != 0) {
            sz += 2;
            if (exp <= -10 || exp >= 10) {
                ++sz;
            }
            if (exp < 0) {
                ++sz;
            }
        }

        if (exp != 0 && digits == intPart) {
            digits++;
        }
        sz += digits;

        // Print mantissa
        insertSpace(target, target + sz);
        if (negative) {
            buffer[target++] = '-';
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
            buffer[target++] = (char) ('0' + intDigit);
            if (--intPart == 0) {
                buffer[target++] = '.';
            }
            pos /= 10;
        }

        // Print exponent
        if (exp != 0) {
            buffer[target++] = 'E';
            if (exp < 0) {
                exp = -exp;
                buffer[target++] = '-';
            }
            if (exp >= 10) {
                buffer[target++] = (char) ('0' + exp / 10);
            }
            buffer[target++] = (char) ('0' + exp % 10);
        }
        return this;
    }

    protected TAbstractStringBuilder append(double value) {
        return insert(length, value);
    }

    protected TAbstractStringBuilder insert(int target, double value) {
        if (value == 0) {
            insertSpace(target, target + 3);
            buffer[target++] = '0';
            buffer[target++] = '.';
            buffer[target++] = '0';
            return this;
        } else if (value == -0) {
            insertSpace(target, target + 4);
            buffer[target++] = '-';
            buffer[target++] = '0';
            buffer[target++] = '.';
            buffer[target++] = '0';
            return this;
        } else if (TDouble.isNaN(value)) {
            insertSpace(target, target + 3);
            buffer[target++] = 'N';
            buffer[target++] = 'a';
            buffer[target++] = 'N';
            return this;
        } else if (TDouble.isInfinite(value)) {
            if (value > 0) {
                insertSpace(target, target + 8);
            } else {
                insertSpace(target, target + 9);
                buffer[target++] = '-';
            }
            buffer[target++] = 'I';
            buffer[target++] = 'n';
            buffer[target++] = 'f';
            buffer[target++] = 'i';
            buffer[target++] = 'n';
            buffer[target++] = 'i';
            buffer[target++] = 't';
            buffer[target++] = 'y';
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
        long mantissa = 0;
        int intPart = 1;
        int digits = 0;
        if (value >= 1) {
            int bit = 256;
            exp = 0;
            double digit = 1;
            for (int i = doublePowersOfTen.length - 1; i >= 0; --i) {
                if ((exp | bit) <= DOUBLE_MAX_EXPONENT && doublePowersOfTen[i] * digit <= value) {
                    digit *= doublePowersOfTen[i];
                    exp |= bit;
                }
                bit >>= 1;
            }
            mantissa = (long) (((value / digit) * DOUBLE_DECIMAL_FACTOR) + 0.5);
        } else {
            int bit = 256;
            exp = 0;
            double digit = 1;
            for (int i = negDoublePowersOfTen.length - 1; i >= 0; --i) {
                if ((exp | bit) <= DOUBLE_MAX_EXPONENT && negDoublePowersOfTen[i] * digit * 10 > value) {
                    digit *= negDoublePowersOfTen[i];
                    exp |= bit;
                }
                bit >>= 1;
            }
            exp = -exp;
            mantissa = (long) (((value * DOUBLE_MAX_POS) / digit) + 0.5);

            while (mantissa >= 10000000000000000L) {
                mantissa /= 10;
                exp--;
            }
        }

        // Remove trailing zeros
        digits = DOUBLE_DECIMAL_PRECISION;
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
                mantissa /= longPowersOfTen[-exp];
                digits -= exp;
                exp = 0;
            }
        }

        // Extend buffer to store exponent
        if (exp != 0) {
            sz += 2;
            if (exp <= -10 || exp >= 10) {
                ++sz;
            }
            if (exp <= -100 || exp >= 100) {
                ++sz;
            }
            if (exp < 0) {
                ++sz;
            }
        }

        if (exp != 0 && digits == intPart) {
            digits++;
        }
        sz += digits;

        // Print mantissa
        insertSpace(target, target + sz);
        if (negative) {
            buffer[target++] = '-';
        }
        long pos = DOUBLE_MAX_POS;
        for (int i = 0; i < digits; ++i) {
            int intDigit;
            if (pos > 0) {
                intDigit = (int) (mantissa / pos);
                mantissa %= pos;
            } else {
                intDigit = 0;
            }
            buffer[target++] = (char) ('0' + intDigit);
            if (--intPart == 0) {
                buffer[target++] = '.';
            }
            pos /= 10;
        }

        // Print exponent
        if (exp != 0) {
            buffer[target++] = 'E';
            if (exp < 0) {
                exp = -exp;
                buffer[target++] = '-';
            }
            if (exp >= 100) {
                buffer[target++] = (char) ('0' + exp / 100);
                exp %= 100;
                buffer[target++] = (char) ('0' + exp / 10);
            } else if (exp >= 10) {
                buffer[target++] = (char) ('0' + exp / 10);
            }
            buffer[target++] = (char) ('0' + exp % 10);
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

    private static int trailingDecimalZeros(long n) {
        long zeros = 1;
        int result = 0;
        int bit = 16;
        for (int i = longLogPowersOfTen.length - 1; i >= 0; --i) {
            if (n % (zeros * longLogPowersOfTen[i]) == 0) {
                result |= bit;
                zeros *= longLogPowersOfTen[i];
            }
            bit >>>= 1;
        }
        return result;
    }

    protected TAbstractStringBuilder append(char c) {
        return insert(length, c);
    }

    protected TAbstractStringBuilder insert(int index, char c) {
        insertSpace(index, index + 1);
        buffer[index++] = c;
        return this;
    }

    protected TAbstractStringBuilder appendCodePoint(int codePoint) {
        if (codePoint < TCharacter.MIN_SUPPLEMENTARY_CODE_POINT) {
            return append((char) codePoint);
        }
        ensureCapacity(length + 2);
        buffer[length++] = TCharacter.highSurrogate(codePoint);
        buffer[length++] = TCharacter.lowSurrogate(codePoint);
        return this;
    }

    protected TAbstractStringBuilder append(TObject obj) {
        return insert(length, obj);
    }

    protected TAbstractStringBuilder insert(int index, TObject obj) {
        return insert(index, TString.wrap(obj != null ? obj.toString() : "null"));
    }

    protected TAbstractStringBuilder append(boolean b) {
        return insert(length, b);
    }

    protected TAbstractStringBuilder insert(int index, boolean b) {
        return insert(index, b ? TString.wrap("true") : TString.wrap("false"));
    }

    public void ensureCapacity(int capacity) {
        if (buffer.length >= capacity) {
            return;
        }
        int newLength = buffer.length < Integer.MAX_VALUE / 2
                ? Math.max(capacity, Math.max(buffer.length * 2, 5))
                : Integer.MAX_VALUE;
        buffer = TArrays.copyOf(buffer, newLength);
    }

    public void trimToSize() {
        if (buffer.length > length) {
            buffer = TArrays.copyOf(buffer, length);
        }
    }

    public int capacity() {
        return buffer.length;
    }

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

    protected TAbstractStringBuilder append(TCharSequence s, int start, int end) {
        return insert(length, s, start, end);
    }

    protected TAbstractStringBuilder insert(int index, TCharSequence s, int start, int end) {
        if (start > end || end > s.length() || start < 0) {
            throw new TIndexOutOfBoundsException();
        }
        insertSpace(index, index + end - start);
        for (int i = start; i < end; ++i) {
            buffer[index++] = s.charAt(i);
        }
        return this;
    }

    protected TAbstractStringBuilder append(TCharSequence s) {
        return append(s, 0, s.length());
    }

    protected TAbstractStringBuilder append(TStringBuffer s) {
        return append((TCharSequence) s);
    }

    protected TAbstractStringBuilder insert(int index, TCharSequence s) {
        return insert(index, s, 0, s.length());
    }

    protected TAbstractStringBuilder append(char[] chars, int offset, int len) {
        return insert(length, chars, offset, len);
    }

    protected TAbstractStringBuilder insert(int index, char[] chars) {
        return insert(index, chars, 0, chars.length);
    }

    protected TAbstractStringBuilder insert(int index, char[] chars, int offset, int len) {
        insertSpace(index, index + len);
        len += offset;
        while (offset < len) {
            buffer[index++] = chars[offset++];
        }
        return this;
    }

    protected TAbstractStringBuilder append(char[] chars) {
        return append(chars, 0, chars.length);
    }

    @Override
    public TCharSequence subSequence(int start, int end) {
        return substring(start, end);
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
        length = newLength;
    }

    public TAbstractStringBuilder deleteCharAt(int index) {
        if (index < 0 || index >= length) {
            throw new TStringIndexOutOfBoundsException();
        }
        length--;
        for (int i = index; i < length; ++i) {
            buffer[i] = buffer[i + 1];
        }
        return this;
    }

    public TAbstractStringBuilder delete(int start, int end) {
        if (start > end || start > length) {
            throw new TStringIndexOutOfBoundsException();
        }
        if (start == end) {
            return this;
        }
        int sz = length - end;
        length -= end - start;
        for (int i = 0; i < sz; ++i) {
            buffer[start++] = buffer[end++];
        }
        return this;
    }

    public TAbstractStringBuilder replace(int start, int end, TString str) {
        int oldSize = end - start;
        if (str.length() > oldSize) {
            insertSpace(end, start + str.length());
        } else if (str.length() < oldSize) {
            delete(start + str.length(), end);
        }
        for (int i = 0; i < str.length(); ++i) {
            buffer[start++] = str.charAt(i);
        }
        return this;
    }

    private void insertSpace(int start, int end) {
        int sz = length - start;
        ensureCapacity(length + end - start);
        for (int i = sz - 1; i >= 0; --i) {
            buffer[end + i] = buffer[start + i];
        }
        length += end - start;
    }

    public int indexOf(TString str) {
        return indexOf(str, 0);
    }

    public int indexOf(TString str, int fromIndex) {
        int sz = length - str.length();
        outer: for (int i = fromIndex; i < sz; ++i) {
            for (int j = 0; j < str.length(); ++j) {
                if (buffer[i + j] != str.charAt(j)) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public int lastIndexOf(TString str) {
        return lastIndexOf(str, length + 1);
    }

    public int lastIndexOf(TString str, int fromIndex) {
        outer: for (int i = fromIndex; i >= 0; --i) {
            for (int j = 0; j < str.length(); ++j) {
                if (buffer[i + j] != str.charAt(j)) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public TAbstractStringBuilder reverse() {
        int half = length / 2;
        for (int i = 0; i < half; ++i) {
            char tmp = buffer[i];
            buffer[i] = buffer[length - i - 1];
            buffer[length - i - 1] = tmp;
        }
        return this;
    }

    public TString substring(int from, int to) {
        if (from > to || from < 0 || to > length) {
            throw new TIndexOutOfBoundsException();
        }
        return new TString(buffer, from, to - from);
    }

    public TString substring(int from) {
        return substring(from, length);
    }

    public void setCharAt(int index, char ch) {
        if (index > length) {
            throw new TIndexOutOfBoundsException();
        }
        buffer[index] = ch;
    }

    public int offsetByCodePoints(int index, int codePointOffset) {
        return TCharacter.offsetByCodePoints(this, index, codePointOffset);
    }

    public int codePointCount(int beginIndex, int endIndex) {
        return TCharacter.codePointCount(this, beginIndex, endIndex);
    }

    public int codePointAt(int index) {
        return TCharacter.codePointAt(this, index);
    }

    public int codePointBefore(int index) {
        return TCharacter.codePointBefore(this, index);
    }
}
