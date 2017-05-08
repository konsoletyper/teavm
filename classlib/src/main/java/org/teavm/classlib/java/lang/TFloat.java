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

public class TFloat extends TNumber implements TComparable<TFloat> {
    public static final float POSITIVE_INFINITY = 1 / 0.0f;
    public static final float NEGATIVE_INFINITY = -POSITIVE_INFINITY;
    public static final float NaN = getNaN();
    public static final float MAX_VALUE = 0x1.fffffeP+127f;
    public static final float MIN_VALUE = 0x1.0p-126f;
    public static final float MIN_NORMAL = 0x0.000002P-126f;
    public static final int MAX_EXPONENT = 127;
    public static final int MIN_EXPONENT = -126;
    public static final int SIZE = 32;
    public static final Class<Float> TYPE = float.class;
    private float value;

    public TFloat(float value) {
        this.value = value;
    }

    public TFloat(double value) {
        this((float) value);
    }

    public TFloat(TString value) throws TNumberFormatException {
        this(parseFloat(value));
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
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public static TFloat valueOf(float d) {
        return new TFloat(d);
    }

    public static String toString(float d) {
        return new TStringBuilder().append(d).toString();
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
        return other instanceof TFloat && ((TFloat) other).value == value;
    }

    @Override
    public int hashCode() {
        return floatToIntBits(value);
    }

    @JSBody(params = "v", script = "return isNaN(v);")
    @Import(module = "runtime", name = "isNaN")
    public static native boolean isNaN(float v);

    public static boolean isInfinite(float v) {
        return !isFinite(v);
    }

    @JSBody(params = "v", script = "return isFinite(v);")
    @Import(module = "runtime", name = "isFinite")
    private static native boolean isFinite(float v);

    @JSBody(script = "return NaN;")
    @Import(module = "runtime", name = "getNaN")
    private static native float getNaN();

    public static float parseFloat(TString string) throws TNumberFormatException {
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
        int mantissa = 0;
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
            if (mantissa < 1E8) {
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
                if (mantissa < 1E38) {
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
        if (exp > 38 || exp == 38 && mantissa > 34028234) {
            return !negative ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
        }
        if (negative) {
            mantissa = -mantissa;
        }
        return mantissa * decimalExponent(exp);
    }

    private static float decimalExponent(int n) {
        float d;
        if (n < 0) {
            d = 0.1f;
            n = -n;
        } else {
            d = 10;
        }
        float result = 1;
        while (n != 0) {
            if (n % 2 != 0) {
                result *= d;
            }
            d *= d;
            n /= 2;
        }
        return result;
    }

    public static TFloat valueOf(TString s) throws TNumberFormatException {
        return valueOf(parseFloat(s));
    }

    public boolean isNaN() {
        return isNaN(value);
    }

    public boolean isInfinite() {
        return isInfinite(value);
    }

    public static native int compare(float f1, float f2);

    @Override
    public int compareTo(TFloat other) {
        return compare(value, other.value);
    }

    public static int floatToRawIntBits(float value) {
        return floatToIntBits(value);
    }

    public static int floatToIntBits(float value) {
        if (value == POSITIVE_INFINITY) {
            return 0x7F800000;
        } else if (value == NEGATIVE_INFINITY) {
            return 0xFF800000;
        } else if (isNaN(value)) {
            return 0x7FC00000;
        }
        float abs = TMath.abs(value);
        int exp = TMath.getExponent(abs);
        int negExp = -exp + 23;
        if (exp < -126) {
            exp = -127;
            negExp = 126 + 23;
        }
        float doubleMantissa;
        if (negExp <= 126) {
            doubleMantissa = abs * binaryExponent(negExp);
        } else {
            doubleMantissa = abs * 0x1p126f * binaryExponent(negExp - 126);
        }
        int mantissa = (int) (doubleMantissa + 0.5f) & 0x7FFFFF;
        return mantissa | ((exp + 127) << 23) | (value < 0 || 1 / value == NEGATIVE_INFINITY  ? (1 << 31) : 0);
    }

    public static float intBitsToFloat(int bits) {
        if ((bits & 0x7F800000) == 0x7F800000) {
            if (bits == 0x7F800000) {
                return POSITIVE_INFINITY;
            } else if (bits == 0xFF800000) {
                return NEGATIVE_INFINITY;
            } else {
                return NaN;
            }
        }
        boolean negative = (bits & (1 << 31)) != 0;
        int rawExp = (bits >> 23) & 0xFF;
        int mantissa = bits & 0x7FFFFF;
        if (rawExp == 0) {
            mantissa <<= 1;
        } else {
            mantissa |= 1L << 23;
        }
        float value = mantissa * binaryExponent(rawExp - 127 - 23);
        return !negative ? value : -value;
    }

    private static float binaryExponent(int n) {
        float result = 1;
        if (n >= 0) {
            float d = 2;
            while (n != 0) {
                if (n % 2 != 0) {
                    result *= d;
                }
                n /= 2;
                d *= d;
            }
        } else {
            n = -n;
            float d = 0.5f;
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

    public static TString toHexString(float f) {
        if (isNaN(f)) {
            return TString.wrap("NaN");
        } else if (isInfinite(f)) {
            return f > 0 ? TString.wrap("Infinity") : TString.wrap("-Infinity");
        }
        char[] buffer = new char[18];
        int sz = 0;
        int bits = floatToIntBits(f);
        boolean subNormal = false;
        int exp = ((bits >>> 23) & 0xFF) - 127;
        int mantissa = (bits & 0x7FFFFF) << 1;
        if (exp == -127) {
            ++exp;
            subNormal = true;
        }
        for (int i = 0; i < 6; ++i) {
            int digit = mantissa & 0xF;
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
        if ((bits & (1L << 31)) != 0) {
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
        int pos = 100;
        boolean first = true;
        for (int i = 0; i < 3; ++i) {
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
}
