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

import org.teavm.backend.javascript.spi.InjectedBy;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.impl.text.DoubleSynthesizer;
import org.teavm.interop.Import;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Unmanaged;
import org.teavm.jso.JSBody;

@NoSideEffects
public class TDouble extends TNumber implements TComparable<TDouble> {
    public static final double POSITIVE_INFINITY = 1 / 0.0;
    public static final double NEGATIVE_INFINITY = -POSITIVE_INFINITY;
    public static final double NaN = 0 / 0.0;
    public static final double MAX_VALUE = 0x1.FFFFFFFFFFFFFP+1023;
    public static final double MIN_NORMAL = -0x1.0P+1022;
    public static final double MIN_VALUE = 0x0.0000000000001P-1022;
    public static final int MAX_EXPONENT = 1023;
    public static final int MIN_EXPONENT = -1022;
    public static final int SIZE = 64;
    public static final int BYTES = SIZE / Byte.SIZE;
    public static final Class<Double> TYPE = double.class;
    private final double value;

    public TDouble(double value) {
        this.value = value;
    }

    public TDouble(String value) throws TNumberFormatException {
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

    public static TDouble valueOf(String string) {
        return valueOf(parseDouble(string));
    }

    public static double parseDouble(String string) throws NumberFormatException {
        // TODO: parse infinite and different radix

        if (string.isEmpty()) {
            throw new NumberFormatException();
        }
        int start = 0;
        int end = string.length();
        while (string.charAt(start) <= ' ') {
            if (++start == end) {
                throw new NumberFormatException();
            }
        }
        while (string.charAt(end - 1) <= ' ') {
            --end;
        }

        boolean negative = false;
        int index = start;
        if (string.charAt(index) == '-') {
            ++index;
            negative = true;
        } else if (string.charAt(index) == '+') {
            ++index;
        }
        if (index == end) {
            throw new NumberFormatException();
        }
        char c = string.charAt(index);

        long mantissa = 0;
        int exp = -1;
        boolean hasOneDigit = false;
        long mantissaPos = 1000000000000000000L;
        if (c != '.') {
            hasOneDigit = true;
            if (c < '0' || c > '9') {
                throw new NumberFormatException();
            }
            while (index < end && string.charAt(index) == '0') {
                ++index;
            }
            while (index < end) {
                c = string.charAt(index);
                if (c < '0' || c > '9') {
                    break;
                }
                if (mantissaPos > 0) {
                    mantissa = mantissa + (mantissaPos * (c - '0'));
                    mantissaPos = Long.divideUnsigned(mantissaPos, 10);
                }
                ++exp;
                ++index;
            }
        }
        if (index < end && string.charAt(index) == '.') {
            ++index;
            while (index < end) {
                c = string.charAt(index);
                if (c < '0' || c > '9') {
                    break;
                }
                if (mantissa == 0 && c == '0') {
                    exp--;
                } else if (mantissaPos > 0) {
                    mantissa = mantissa + (mantissaPos * (c - '0'));
                    mantissaPos = Long.divideUnsigned(mantissaPos, 10);
                }
                ++index;
                hasOneDigit = true;
            }
            if (!hasOneDigit) {
                throw new NumberFormatException();
            }
        }
        if (index < end) {
            c = string.charAt(index);
            if (c != 'e' && c != 'E') {
                throw new NumberFormatException();
            }
            ++index;
            boolean negativeExp = false;
            if (index == end) {
                throw new NumberFormatException();
            }
            if (string.charAt(index) == '-') {
                ++index;
                negativeExp = true;
            } else if (string.charAt(index) == '+') {
                ++index;
            }
            int numExp = 0;
            hasOneDigit = false;
            while (index < end) {
                c = string.charAt(index);
                if (c < '0' || c > '9') {
                    break;
                }
                numExp = 10 * numExp + (c - '0');
                hasOneDigit = true;
                ++index;
            }
            if (!hasOneDigit) {
                throw new NumberFormatException();
            }
            if (negativeExp) {
                numExp = -numExp;
            }
            exp += numExp;
        }

        return DoubleSynthesizer.synthesizeDouble(mantissa, exp, negative);
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
        return other instanceof TDouble && equals(value, ((TDouble) other).value);
    }

    private static boolean equals(double a, double b) {
        return PlatformDetector.isJavaScript() ? doubleEqualsJs(a, b) : equalsWithBits(a, b);
    }

    @InjectedBy(DoubleGenerator.class)
    private static native boolean doubleEqualsJs(double a, double b);

    private static boolean equalsWithBits(double a, double b) {
        return a != a ? b != b : doubleToRawLongBits(a) == doubleToRawLongBits(b);
    }

    @Override
    public int hashCode() {
        return hashCode(value);
    }

    public static int hashCode(double d) {
        long h = doubleToLongBits(d);
        return (int) (h >>> 32) ^ (int) h;
    }

    @NoSideEffects
    public static int compare(double a, double b) {
        var diff = (a > b ? 1 : 0) - (b > a ? 1 : 0);
        return diff != 0 ? diff : (1 / a > 1 / b ? 1 : 0) - (1 / b > 1 / a ? 1 : 0)
                + (b == b ? 1 : 0) - (a == a ? 1 : 0);
    }

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
    @Import(module = "teavm", name = "isnan")
    @NoSideEffects
    @Unmanaged
    public static native boolean isNaN(double v);

    @JSBody(params = "v", script = "return !isFinite(v);")
    @Import(module = "teavm", name = "isinf")
    @NoSideEffects
    @Unmanaged
    public static native boolean isInfinite(double v);

    @JSBody(params = "v", script = "return isFinite(v);")
    @Import(module = "teavm", name = "isfinite")
    @NoSideEffects
    @Unmanaged
    public static native boolean isFinite(double v);

    @InjectedBy(DoubleGenerator.class)
    @Import(name = "teavm_reinterpretDoubleToLong")
    @NoSideEffects
    @Unmanaged
    public static native long doubleToRawLongBits(double value);

    public static long doubleToLongBits(double value) {
        if (isNaN(value)) {
            return 0x7ff8000000000000L;
        }
        return doubleToRawLongBits(value);
    }

    @InjectedBy(DoubleGenerator.class)
    @Import(name = "teavm_reinterpretLongToDouble")
    @NoSideEffects
    @Unmanaged
    public static native double longBitsToDouble(long bits);

    public static String toHexString(double d) {
        if (isNaN(d)) {
            return "NaN";
        } else if (isInfinite(d)) {
            return d > 0 ? "Infinity" : "-Infinity";
        }
        char[] buffer = new char[30];
        int sz = 0;
        long bits = doubleToLongBits(d);
        boolean subNormal = false;
        boolean negative = (bits & (1L << 63)) != 0;
        int exp = (int) ((bits >>> 52) & 0x7FF) - 1023;
        long mantissa = bits & 0xFFFFFFFFFFFFFL;
        if (exp == -1023) {
            if (mantissa == 0) {
                return negative ? "-0x0.0p0" : "0x0.0p0";
            }
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
        if (negative) {
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

        return new String(buffer, 0, sz);
    }
}
