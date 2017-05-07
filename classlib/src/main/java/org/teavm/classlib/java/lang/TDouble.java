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

import org.teavm.interop.Import;
import org.teavm.jso.JSBody;

public class TDouble extends TNumber implements TComparable<TDouble> {
    public static final double POSITIVE_INFINITY = 1 / 0.0;
    public static final double NEGATIVE_INFINITY = -POSITIVE_INFINITY;
    public static final double NaN = getNaN();
    public static final double MAX_VALUE = 0x1.FFFFFFFFFFFFFP+1023;
    public static final double MIN_NORMAL = -0x1.0P+1022;
    public static final double MIN_VALUE = 0x0.0000000000001P-1022;
    public static final int MAX_EXPONENT = 1023;
    public static final int MIN_EXPONENT = -1022;
    public static final int SIZE = 64;
    public static final Class<Double> TYPE = double.class;
    private double value;

    public TDouble(double value) {
        this.value = value;
    }

    public TDouble(TString value) throws TNumberFormatException {
        this.value = parseDouble(value);
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return (long) value;
    }

    @Override
    public float floatValue() {
        return (float) value;
    }

    public static TDouble valueOf(double d) {
        return new TDouble(d);
    }

    public static String toString(double d) {
        return new TStringBuilder().append(d).toString();
    }

    public static TDouble valueOf(TString string) {
        return valueOf(parseDouble(string));
    }

    public static double parseDouble(TString string) throws TNumberFormatException {
        // TODO: parse infinite and different radix
        string = string.trim();
        boolean negative = false;
        int index = 0;
        if (string.charAt(index) == '-') {
            ++index;
            negative = true;
        } else if (string.charAt(index) == '+') {
            ++index;
        }
        char c = string.charAt(index);
        if (c < '0' || c > '9') {
            throw new TNumberFormatException();
        }
        long mantissa = 0;
        int exp = 0;
        while (string.charAt(index) == '0') {
            if (++index == string.length()) {
                return 0;
            }
        }
        while (index < string.length()) {
            c = string.charAt(index);
            if (c < '0' || c > '9') {
                break;
            }
            if (mantissa < 1E17) {
                mantissa = mantissa * 10 + (c - '0');
            } else {
                ++exp;
            }
            ++index;
        }
        if (index < string.length() && string.charAt(index) == '.') {
            ++index;
            boolean hasOneDigit = false;
            while (index < string.length()) {
                c = string.charAt(index);
                if (c < '0' || c > '9') {
                    break;
                }
                if (mantissa < 1E17) {
                    mantissa = mantissa * 10 + (c - '0');
                    --exp;
                }
                ++index;
                hasOneDigit = true;
            }
            if (!hasOneDigit) {
                throw new TNumberFormatException();
            }
        }
        if (index < string.length()) {
            c = string.charAt(index);
            if (c != 'e' && c != 'E') {
                throw new TNumberFormatException();
            }
            ++index;
            boolean negativeExp = false;
            if (string.charAt(index) == '-') {
                ++index;
                negativeExp = true;
            } else if (string.charAt(index) == '+') {
                ++index;
            }
            int numExp = 0;
            boolean hasOneDigit = false;
            while (index < string.length()) {
                c = string.charAt(index);
                if (c < '0' || c > '9') {
                    break;
                }
                numExp = 10 * numExp + (c - '0');
                hasOneDigit = true;
                ++index;
            }
            if (!hasOneDigit) {
                throw new TNumberFormatException();
            }
            if (negativeExp) {
                numExp = -numExp;
            }
            exp += numExp;
        }
        if (exp > 308 || exp == 308 && mantissa > 17976931348623157L) {
            return !negative ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
        }
        if (negative) {
            mantissa = -mantissa;
        }
        return mantissa * decimalExponent(exp);
    }

    public static double decimalExponent(int n) {
        double d;
        if (n < 0) {
            d = 0.1;
            n = -n;
        } else {
            d = 10;
        }
        double result = 1;
        while (n != 0) {
            if (n % 2 != 0) {
                result *= d;
            }
            d *= d;
            n /= 2;
        }
        return result;
    }

    @Override
    public String toString() {
        return toString(value);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof TDouble && ((TDouble) other).value == value;
    }

    @Override
    public int hashCode() {
        long h = doubleToLongBits(value);
        return (int) (h >>> 32) ^ (int) h;
    }

    public static native int compare(double a, double b);

    @Override
    public int compareTo(TDouble other) {
        return compare(value, other.value);
    }

    public boolean isNaN() {
        return isNaN(value);
    }

    public boolean isInfinite() {
        return isInfinite(value);
    }

    @JSBody(params = "v", script = "return isNaN(v);")
    @Import(module = "runtime", name = "isNaN")
    public static native boolean isNaN(double v);

    @JSBody(script = "return NaN;")
    @Import(module = "runtime", name = "getNaN")
    private static native double getNaN();

    @JSBody(params = "v", script = "return !isFinite(v);")
    @Import(module = "runtime", name = "isInfinite")
    public static native boolean isInfinite(double v);

    public static long doubleToRawLongBits(double value) {
        return doubleToLongBits(value);
    }

    public static long doubleToLongBits(double value) {
        if (value == POSITIVE_INFINITY) {
            return 0x7FF0000000000000L;
        } else if (value == NEGATIVE_INFINITY) {
            return 0xFFF0000000000000L;
        } else if (isNaN(value)) {
            return 0x7FF8000000000000L;
        }
        double abs = TMath.abs(value);
        int exp = TMath.getExponent(abs);
        int negExp = -exp + 52;
        if (exp < -1022) {
            exp = -1023;
            negExp = 1022 + 52;
        }
        double doubleMantissa;
        if (negExp <= 1022) {
            doubleMantissa = abs * binaryExponent(negExp);
        } else {
            doubleMantissa = abs * 0x1p1022 * binaryExponent(negExp - 1022);
        }
        long mantissa = (long) (doubleMantissa + 0.5) & 0xFFFFFFFFFFFFFL;
        return mantissa | ((exp + 1023L) << 52) | (value < 0 || 1 / value == NEGATIVE_INFINITY ? (1L << 63) : 0);
    }

    public static double longBitsToDouble(long bits) {
        if ((bits & 0x7FF0000000000000L) == 0x7FF0000000000000L) {
            if (bits == 0x7FF0000000000000L) {
                return POSITIVE_INFINITY;
            } else if (bits == 0xFFF0000000000000L) {
                return NEGATIVE_INFINITY;
            } else {
                return NaN;
            }
        }
        boolean negative = (bits & (1L << 63)) != 0;
        int rawExp = (int) ((bits >> 52) & 0x7FFL);
        long mantissa = bits & 0xFFFFFFFFFFFFFL;
        if (rawExp == 0) {
            mantissa <<= 1;
        } else {
            mantissa |= 1L << 52;
        }
        double value = mantissa * binaryExponent(rawExp - 1023 - 52);
        return !negative ? value : -value;
    }

    public static TString toHexString(double d) {
        if (isNaN(d)) {
            return TString.wrap("NaN");
        } else if (isInfinite(d)) {
            return d > 0 ? TString.wrap("Infinity") : TString.wrap("-Infinity");
        }
        char[] buffer = new char[30];
        int sz = 0;
        long bits = doubleToLongBits(d);
        boolean subNormal = false;
        int exp = (int) ((bits >>> 52) & 0x7FF) - 1023;
        long mantissa = bits & 0xFFFFFFFFFFFFFL;
        if (exp == -1023) {
            ++exp;
            subNormal = true;
        }
        for (int i = 0; i < 13; ++i) {
            int digit = (int) (mantissa & 0xF);
            if (digit > 0 || sz > 0) {
                buffer[sz++] = TCharacter.forDigit(digit, 16);
            }
            mantissa >>>= 4;
        }
        if (sz == 0) {
            buffer[sz++] = '0';
        }
        buffer[sz++] = '.';

        buffer[sz++] = subNormal ? '0' : '1';
        buffer[sz++] = 'x';
        buffer[sz++] = '0';
        if ((bits & (1L << 63)) != 0) {
            buffer[sz++] = '-';
        }
        int half = sz / 2;
        for (int i = 0; i < half; ++i) {
            char tmp = buffer[i];
            buffer[i] = buffer[sz - i - 1];
            buffer[sz - i - 1] = tmp;
        }

        buffer[sz++] = 'p';
        if (exp < 0) {
            exp = -exp;
            buffer[sz++] = '-';
        }
        int pos = 1000;
        boolean first = true;
        for (int i = 0; i < 4; ++i) {
            int digit = exp / pos;
            if (digit > 0 || !first) {
                buffer[sz++] = TCharacter.forDigit(digit, 10);
                first = false;
            }
            exp %= pos;
            pos /= 10;
        }
        if (first) {
            buffer[sz++] = '0';
        }

        return new TString(buffer, 0, sz);
    }

    private static double binaryExponent(int n) {
        double result = 1;
        if (n >= 0) {
            double d = 2;
            while (n != 0) {
                if (n % 2 != 0) {
                    result *= d;
                }
                n /= 2;
                d *= d;
            }
        } else {
            n = -n;
            double d = 0.5;
            while (n != 0) {
                if (n % 2 != 0) {
                    result *= d;
                }
                n /= 2;
                d *= d;
            }
        }
        return result;
    }
}
