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

import org.teavm.classlib.impl.text.DoubleAnalyzer;
import org.teavm.classlib.impl.text.FloatAnalyzer;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.util.TArrays;

class TAbstractStringBuilder extends TObject implements TSerializable, TCharSequence {
    static class Constants {
        static int[] intPowersOfTen = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000,
                1000000000 };
        static long[] longPowersOfTen = { 1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000,
                1000000000, 10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L,
                1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L };
        static final long[] longLogPowersOfTen = { 1, 10, 100, 10000, 100000000, 10000000000000000L, };

        static final DoubleAnalyzer.Result doubleAnalysisResult = new DoubleAnalyzer.Result();
        static final FloatAnalyzer.Result floatAnalysisResult = new FloatAnalyzer.Result();
    }

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

    protected TAbstractStringBuilder append(String string) {
        return insert(length, string);
    }

    protected TAbstractStringBuilder insert(int index, String string) {
        if (index < 0 || index > length) {
            throw new TStringIndexOutOfBoundsException();
        }
        if (string == null) {
            string = "null";
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

        FloatAnalyzer.Result number = Constants.floatAnalysisResult;
        FloatAnalyzer.analyze(value, number);
        int mantissa = number.mantissa;
        int exp = number.exponent;
        boolean negative = number.sign;
        int intPart = 1;
        int sz = 1; // Decimal point always included
        if (negative) {
            ++sz; // including '-' sign of mantissa
        }

        // Remove trailing zeros
        int digits = FloatAnalyzer.PRECISION;
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
            } else {
                mantissa /= Constants.intPowersOfTen[-exp];
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
        int pos = FloatAnalyzer.MAX_POS;
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

        DoubleAnalyzer.Result number = Constants.doubleAnalysisResult;
        DoubleAnalyzer.analyze(value, number);
        long mantissa = number.mantissa;
        int exp = number.exponent;
        boolean negative = number.sign;
        int intPart = 1;

        // Get absolute value
        int sz = 1; // Decimal point always included
        if (negative) {
            ++sz; // including '-' sign of mantissa
        }

        // Remove trailing zeros
        int digits = DoubleAnalyzer.DECIMAL_PRECISION;
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
            } else {
                mantissa /= Constants.longPowersOfTen[-exp];
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
        long pos = DoubleAnalyzer.DOUBLE_MAX_POS;
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
        for (int i = Constants.longLogPowersOfTen.length - 1; i >= 0; --i) {
            if (n % (zeros * Constants.longLogPowersOfTen[i]) == 0) {
                result |= bit;
                zeros *= Constants.longLogPowersOfTen[i];
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
        return insert(index, obj != null ? obj.toString() : "null");
    }

    protected TAbstractStringBuilder append(boolean b) {
        return insert(length, b);
    }

    protected TAbstractStringBuilder insert(int index, boolean b) {
        return insert(index, b ? "true" : "false");
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
            throw new IndexOutOfBoundsException("Index out of bounds");
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
