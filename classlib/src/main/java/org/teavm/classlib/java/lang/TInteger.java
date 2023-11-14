/*
 *  Copyright 2014 Alexey Andreev.
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

import static org.teavm.classlib.impl.IntegerUtil.toUnsignedLogRadixString;
import java.util.Objects;
import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.interop.NoSideEffects;

public class TInteger extends TNumber implements TComparable<TInteger> {
    public static final int SIZE = 32;
    public static final int BYTES = SIZE / Byte.SIZE;
    public static final int MIN_VALUE = 0x80000000;
    public static final int MAX_VALUE = 0x7FFFFFFF;
    public static final Class<Integer> TYPE = int.class;
    private static TInteger[] integerCache;
    private final int value;

    public TInteger(int value) {
        this.value = value;
    }

    public TInteger(String s) throws NumberFormatException {
        this(parseInt(s));
    }

    public static String toString(int i, int radix) {
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            radix = 10;
        }
        return new TAbstractStringBuilder(20).append(i, radix).toString();
    }

    public static int hashCode(int value) {
        return value;
    }

    public static String toHexString(int i) {
        return toUnsignedLogRadixString(i, 4);
    }

    public static String toOctalString(int i) {
        return toUnsignedLogRadixString(i, 3);
    }

    public static String toBinaryString(int i) {
        return toUnsignedLogRadixString(i, 1);
    }

    public static String toString(int i) {
        return toString(i, 10);
    }

    public static int parseInt(String s, int radix) throws TNumberFormatException {
        if (s == null) {
            throw new TNumberFormatException("String is null");
        }
        return parseIntImpl(s, 0, s.length(), radix);
    }

    public static int parseInt(CharSequence s, int beginIndex, int endIndex, int radix) throws TNumberFormatException {
        return parseIntImpl(Objects.requireNonNull(s), beginIndex, endIndex, radix);
    }

    private static int parseIntImpl(CharSequence s, int beginIndex, int endIndex, int radix)
            throws TNumberFormatException {
        if (beginIndex == endIndex) {
            throw new TNumberFormatException("String is empty");
        }
        if (radix < TCharacter.MIN_RADIX || radix > TCharacter.MAX_RADIX) {
            throw new TNumberFormatException("Illegal radix: " + radix);
        }
        boolean negative = false;
        int index = beginIndex;
        switch (s.charAt(index)) {
            case '-':
                negative = true;
                index++;
                break;
            case '+':
                index++;
                break;
        }
        int value = 0;
        int maxValue = 1 + TInteger.MAX_VALUE / radix;
        if (index == endIndex) {
            throw new TNumberFormatException();
        }
        while (index < endIndex) {
            int digit = decodeDigit(s.charAt(index++));
            if (digit < 0) {
                throw new TNumberFormatException("String contains invalid digits: "
                        + s.subSequence(beginIndex, endIndex));
            }
            if (digit >= radix) {
                throw new TNumberFormatException("String contains digits out of radix " + radix + ": "
                        + s.subSequence(beginIndex, endIndex));
            }
            if (value > maxValue) {
                throw new TNumberFormatException("The value is too big for integer type");
            }
            value = radix * value + digit;
            if (value < 0) {
                if (index == endIndex && value == MIN_VALUE && negative) {
                    return MIN_VALUE;
                }
                throw new TNumberFormatException("The value is too big for int type: "
                        + s.subSequence(beginIndex, endIndex));
            }
        }
        return negative ? -value : value;
    }

    public static int parseInt(String s) throws TNumberFormatException {
        return parseInt(s, 10);
    }

    public static TInteger valueOf(String s, int radix) throws TNumberFormatException {
        return valueOf(parseInt(s, radix));
    }

    public static TInteger valueOf(String s) throws TNumberFormatException {
        return valueOf(s, 10);
    }

    public static TInteger valueOf(int i) {
        if (i >= -128 && i <= 127) {
            ensureIntegerCache();
            return integerCache[i + 128];
        }
        return new TInteger(i);
    }

    private static void ensureIntegerCache() {
        if (integerCache == null) {
            integerCache = new TInteger[256];
            for (int j = 0; j < integerCache.length; ++j) {
                integerCache[j] = new TInteger(j - 128);
            }
        }
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public String toString() {
        return toString(value);
    }

    @Override
    public int hashCode() {
        return TInteger.hashCode(value);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof TInteger && ((TInteger) other).value == value;
    }

    public static TInteger getInteger(TString nm) {
        return getInteger(nm, null);
    }

    public static TInteger getInteger(TString nm, int val) {
        return getInteger(nm, TInteger.valueOf(val));
    }

    public static TInteger getInteger(TString nm, TInteger val) {
        String result = nm != null ? TSystem.getProperty(nm.toString()) : null;
        try {
            return result != null ? TInteger.valueOf(result) : val;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static TInteger decode(String nm) throws TNumberFormatException {
        if (nm.isEmpty()) {
            throw new TNumberFormatException("Can't parse empty string");
        }
        int index = 0;
        boolean negaive = false;
        if (nm.charAt(index) == '+') {
            ++index;
        } else if (nm.charAt(index) == '-') {
            ++index;
            negaive = true;
        }
        if (index >= nm.length()) {
            throw new TNumberFormatException("The string does not represent a number");
        }
        int radix = 10;
        if (nm.charAt(index) == '#') {
            radix = 16;
            ++index;
        } else if (nm.charAt(index) == '0') {
            ++index;
            if (index == nm.length()) {
                return TInteger.valueOf(0);
            }
            if (nm.charAt(index) == 'x' || nm.charAt(index) == 'X') {
                radix = 16;
                ++index;
            } else {
                radix = 8;
            }
        }
        if (index >= nm.length()) {
            throw new TNumberFormatException("The string does not represent a number");
        }
        int value = 0;
        int maxValue = 1 + TInteger.MAX_VALUE / radix;
        while (index < nm.length()) {
            int digit = decodeDigit(nm.charAt(index++));
            if (digit < 0 || digit >= radix) {
                throw new TNumberFormatException("The string does not represent a number");
            }
            if (value > maxValue) {
                throw new TNumberFormatException("The value is too big for integer type");
            }
            value = value * radix + digit;
            if (value < 0) {
                if (negaive && value == MIN_VALUE && index == nm.length()) {
                    return TInteger.valueOf(MIN_VALUE);
                }
                throw new TNumberFormatException("The string represents a too big number");
            }
        }
        return TInteger.valueOf(negaive ? -value : value);
    }

    private static int decodeDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'z') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 10;
        } else {
            return -1;
        }
    }

    @Override
    public int compareTo(TInteger other) {
        return compare(value, other.value);
    }

    @NoSideEffects
    public static native int compare(int x, int y);

    public static int numberOfLeadingZeros(int i) {
        if (i == 0) {
            return SIZE;
        }
        int n = 0;
        if (i >>> 16 != 0) {
            i >>>= 16;
            n |= 16;
        }
        if (i >>> 8 != 0) {
            i >>>= 8;
            n |= 8;
        }
        if (i >>> 4 != 0) {
            i >>>= 4;
            n |= 4;
        }
        if (i >>> 2 != 0) {
            i >>>= 2;
            n |= 2;
        }
        if (i >>> 1 != 0) {
            i >>>= 1;
            n |= 1;
        }
        return SIZE - n - 1;
    }

    public static int numberOfTrailingZeros(int i) {
        if (i == 0) {
            return SIZE;
        }
        int n = 0;
        if (i << 16 != 0) {
            i <<= 16;
            n |= 16;
        }
        if (i << 8 != 0) {
            i <<= 8;
            n |= 8;
        }
        if (i << 4 != 0) {
            i <<= 4;
            n |= 4;
        }
        if (i << 2 != 0) {
            i <<= 2;
            n |= 2;
        }
        if (i << 1 != 0) {
            i <<= 1;
            n |= 1;
        }
        return SIZE - n - 1;
    }

    public static int highestOneBit(int i) {
        return i & (0x80000000 >>> numberOfLeadingZeros(i));
    }

    public static int lowestOneBit(int i) {
        return -i & i;
    }

    public static int bitCount(int i) {
        i = ((i & 0xAAAAAAAA) >>> 1)  + (i & 0x55555555);
        i = ((i & 0xCCCCCCCC) >>> 2)  + (i & 0x33333333);
        i = ((i & 0x70707070) >>> 4)  + (i & 0x07070707);
        i = ((i & 0x0F000F00) >>> 8)  + (i & 0x000F000F);
        i = ((i & 0x001F0000) >>> 16) + (i & 0x0000001F);
        return i;
    }

    public static int rotateLeft(int i, int distance) {
        distance &= 0x1F;
        return (i << distance) | (i >>> (32 - distance));
    }

    public static int rotateRight(int i, int distance) {
        distance &= 0x1F;
        return (i >>> distance) | (i << (32 - distance));
    }

    public static int reverse(int i) {
        i = ((i & 0xAAAAAAAA) >>> 1)  | ((i & 0x55555555) << 1);
        i = ((i & 0xCCCCCCCC) >>> 2)  | ((i & 0x33333333) << 2);
        i = ((i & 0xF0F0F0F0) >>> 4)  | ((i & 0x0F0F0F0F) << 4);
        i = ((i & 0xFF00FF00) >>> 8)  | ((i & 0x00FF00FF) << 8);
        i = ((i & 0xFFFF0000) >>> 16) | ((i & 0x0000FFFF) << 16);
        return i;
    }

    public static int reverseBytes(int i) {
        i = ((i & 0xFF00FF00) >>> 8) | ((i & 0x00FF00FF) << 8);
        i = (i >>> 16) + (i << 16);
        return i;
    }

    public static int signum(int i) {
        return (i >> 31) | (-i >>> 31);
    }

    @InjectedBy(IntegerNativeGenerator.class)
    @NoSideEffects
    public static native int divideUnsigned(int dividend, int divisor);

    @InjectedBy(IntegerNativeGenerator.class)
    @NoSideEffects
    public static native int remainderUnsigned(int dividend, int divisor);

    @InjectedBy(IntegerNativeGenerator.class)
    @NoSideEffects
    public static native int compareUnsigned(int a, int b);
}
