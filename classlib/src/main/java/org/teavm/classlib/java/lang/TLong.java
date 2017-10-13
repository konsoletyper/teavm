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

public class TLong extends TNumber implements TComparable<TLong> {
    public static final long MIN_VALUE = -0x8000000000000000L;
    public static final long MAX_VALUE = 0x7FFFFFFFFFFFFFFFL;
    public static final Class<Long> TYPE = long.class;
    public static final int SIZE = 64;
    private long value;

    public TLong(long value) {
        this.value = value;
    }

    public TLong(TString value) throws TNumberFormatException {
        this(parseLong(value));
    }

    public static TLong valueOf(long value) {
        return new TLong(value);
    }

    public static long parseLong(TString s, int radix) throws TNumberFormatException {
        if (radix < TCharacter.MIN_RADIX || radix > TCharacter.MAX_RADIX) {
            throw new TNumberFormatException(TString.wrap("Illegal radix: " + radix));
        }
        if (s == null || s.isEmpty()) {
            throw new TNumberFormatException(TString.wrap("String is null or empty"));
        }
        boolean negative = false;
        int index = 0;
        switch (s.charAt(0)) {
            case '-':
                negative = true;
                index = 1;
                break;
            case '+':
                index = 1;
                break;
        }
        long value = 0;
        while (index < s.length()) {
            int digit = TCharacter.getNumericValue(s.charAt(index++));
            if (digit < 0) {
                throw new TNumberFormatException(TString.wrap("String contains invalid digits: " + s));
            }
            if (digit >= radix) {
                throw new TNumberFormatException(TString.wrap("String contains digits out of radix " + radix
                        + ": " + s));
            }
            value = radix * value + digit;
            if (value < 0) {
                if (index == s.length() && value == MIN_VALUE && negative) {
                    return MIN_VALUE;
                }
                throw new TNumberFormatException(TString.wrap("The value is too big for int type: " + s));
            }
        }
        return negative ? -value : value;
    }

    public static long parseLong(TString s) throws TNumberFormatException {
        return parseLong(s, 10);
    }

    public static TLong valueOf(TString s, int radix) throws TNumberFormatException {
        return valueOf(parseLong(s, radix));
    }

    public static TLong valueOf(TString s) throws TNumberFormatException {
        return valueOf(parseLong(s));
    }

    public static TLong decode(TString nm) throws TNumberFormatException {
        if (nm == null || nm.isEmpty()) {
            throw new TNumberFormatException(TString.wrap("Can't parse empty or null string"));
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
            throw new TNumberFormatException(TString.wrap("The string does not represent a number"));
        }
        int radix = 10;
        if (nm.charAt(index) == '#') {
            radix = 16;
            ++index;
        } else if (nm.charAt(index) == '0') {
            ++index;
            if (index == nm.length()) {
                return TLong.valueOf(0);
            }
            if (nm.charAt(index) == 'x' || nm.charAt(index) == 'X') {
                radix = 16;
                ++index;
            } else {
                radix = 8;
            }
        }
        if (index >= nm.length()) {
            throw new TNumberFormatException(TString.wrap("The string does not represent a number"));
        }
        long value = 0;
        while (index < nm.length()) {
            int digit = decodeDigit(nm.charAt(index++));
            if (digit >= radix) {
                throw new TNumberFormatException(TString.wrap("The string does not represent a number"));
            }
            value = value * radix + digit;
            if (value < 0) {
                if (negaive && value == MIN_VALUE && index == nm.length()) {
                    return TLong.valueOf(MIN_VALUE);
                }
                throw new TNumberFormatException(TString.wrap("The string represents a too big number"));
            }
        }
        return TLong.valueOf(negaive ? -value : value);
    }

    private static int decodeDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'z') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 10;
        } else {
            return 255;
        }
    }
    @Override
    public int intValue() {
        return (int) value;
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

    public static String toString(long i, int radix) {
        return new TStringBuilder().insert(0, i, radix).toString();
    }

    public static String toHexString(long i) {
        return toString(i, 16);
    }

    public static String toOctalString(long i) {
        return toString(i, 8);
    }

    public static String toBinaryString(long i) {
        return toString(i, 2);
    }

    public static String toString(long value) {
        return new TStringBuilder().append(value).toString();
    }

    @Override
    public String toString() {
        return toString(value);
    }

    @Override
    public int hashCode() {
        return hashCode(value);
    }

    private static int hashCode(long value) {
        return (int) value ^ (int) (value >>> 32);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof TLong && ((TLong) other).value == value;
    }

    public static native int compare(long a, long b);

    @Override
    public int compareTo(TLong other) {
        return compare(value, other.value);
    }

    public static TLong getLong(TString nm) {
        return getLong(nm, null);
    }

    public static TLong getLong(TString nm, long val) {
        return getLong(nm, TLong.valueOf(val));
    }

    public static TLong getLong(TString nm, TLong val) {
        TString result = nm != null ? TString.wrap(TSystem.getProperty(nm.toString())) : null;
        try {
            return result != null ? TLong.valueOf(result) : val;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static long highestOneBit(long i) {
        return 0x8000000000000000L >>> numberOfLeadingZeros(i);
    }

    public static int numberOfLeadingZeros(long i) {
        if (i == 0) {
            return SIZE;
        }
        int n = 0;
        if (i >>> 32 != 0) {
            i >>>= 32;
            n |= 32;
        }
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

    public static int numberOfTrailingZeros(long i) {
        if (i == 0) {
            return SIZE;
        }
        int n = 0;
        if (i << 32 != 0) {
            i <<= 32;
            n |= 32;
        }
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


    public static long lowestOneBit(long i) {
        return 1L << numberOfTrailingZeros(i);
    }

    public static int bitCount(long i) {
        i = ((i & 0xAAAAAAAAAAAAAAAAL) >> 1)  + (i & 0x5555555555555555L);
        i = ((i & 0xCCCCCCCCCCCCCCCCL) >> 2)  + (i & 0x3333333333333333L);
        i = ((i & 0x3030303030303030L) >> 4)  + (i & 0x0303030303030303L);
        i = ((i & 0x0700070007000700L) >> 8)  + (i & 0x0007000700070007L);
        i = ((i & 0x000F0000000F0000L) >> 16) + (i & 0x0000000F0000000FL);
        i = ((i & 0x0000001F00000000L) >> 32) + (i & 0x000000000000001FL);
        return (int) i;
    }

    public static long rotateLeft(long i, int distance) {
        distance &= 0x3F;
        return (i << distance) | (i >>> (64 - distance));
    }

    public static long rotateRight(long i, int distance) {
        distance &= 0x3F;
        return (i >>> distance) | (i << (64 - distance));
    }

    public static long reverse(long i) {
        i = ((i & 0xAAAAAAAAAAAAAAAAL) >> 1)  | ((i & 0x5555555555555555L) << 1);
        i = ((i & 0xCCCCCCCCCCCCCCCCL) >> 2)  | ((i & 0x3333333333333333L) << 2);
        i = ((i & 0xF0F0F0F0F0F0F0F0L) >> 4)  | ((i & 0x0F0F0F0F0F0F0F0FL) << 4);
        i = ((i & 0xFF00FF00FF00FF00L) >> 8)  | ((i & 0x00FF00FF00FF00FFL) << 8);
        i = ((i & 0xFFFF0000FFFF0000L) >> 16) | ((i & 0x0000FFFF0000FFFFL) << 16);
        i = ((i & 0xFFFF0000FFFF0000L) >> 32) | ((i & 0x0000FFFF0000FFFFL) << 32);
        return i;
    }

    public static long reverseBytes(long i) {
        i = ((i & 0xFF00FF00FF00FF00L) >> 8)  | ((i & 0x00FF00FF00FF00FFL) << 8);
        i = ((i & 0xFFFF0000FFFF0000L) >> 16) | ((i & 0x0000FFFF0000FFFFL) << 16);
        i = (i >> 32) | (i << 32);
        return i;
    }

    public static int signum(long i) {
        return (int) ((i >> 63) | (-i >>> 63));
    }
}
