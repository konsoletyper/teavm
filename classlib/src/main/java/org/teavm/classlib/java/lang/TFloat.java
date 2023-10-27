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

import org.teavm.classlib.impl.text.FloatSynthesizer;
import org.teavm.interop.Import;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Unmanaged;
import org.teavm.jso.JSBody;

public class TFloat extends TNumber implements TComparable<TFloat> {
    public static final float POSITIVE_INFINITY = 1 / 0.0f;
    public static final float NEGATIVE_INFINITY = -POSITIVE_INFINITY;
    public static final float NaN = 0f / 0f;
    public static final float MAX_VALUE = 0x1.fffffeP+127f;
    public static final float MIN_VALUE = 0x1.0p-126f;
    public static final float MIN_NORMAL = 0x0.000002P-126f;
    public static final int MAX_EXPONENT = 127;
    public static final int MIN_EXPONENT = -126;
    public static final int SIZE = 32;
    public static final int BYTES = SIZE / Byte.SIZE;
    public static final Class<Float> TYPE = float.class;
    private final float value;

    public TFloat(float value) {
        this.value = value;
    }

    public TFloat(double value) {
        this((float) value);
    }

    public TFloat(String value) throws TNumberFormatException {
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
        return other instanceof TFloat && equals(value, ((TFloat) other).value);
    }

    private static boolean equals(float a, float b) {
        return a != a ? b != b : floatToRawIntBits(a) == floatToRawIntBits(b);
    }

    @Override
    public int hashCode() {
        return hashCode(value);
    }

    public static int hashCode(float f) {
        return floatToIntBits(f);
    }

    @JSBody(params = "v", script = "return isNaN(v);")
    @Import(module = "teavm", name = "isnan")
    @NoSideEffects
    @Unmanaged
    public static native boolean isNaN(float v);

    @JSBody(params = "v", script = "return !isFinite(v);")
    @Import(module = "teavm", name = "isinf")
    @NoSideEffects
    @Unmanaged
    public static native boolean isInfinite(float v);

    @JSBody(params = "v", script = "return isFinite(v);")
    @Import(module = "teavm", name = "isfinite")
    @NoSideEffects
    @Unmanaged
    public static native boolean isFinite(float v);

    public static float parseFloat(String string) throws NumberFormatException {
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

        int mantissa = 0;
        int exp = -1;
        int mantissaPos = 100000000;

        boolean hasOneDigit = false;
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
                    mantissaPos = Integer.divideUnsigned(mantissaPos, 10);
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
                    mantissaPos = Integer.divideUnsigned(mantissaPos, 10);
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

        return FloatSynthesizer.synthesizeFloat(mantissa, exp, negative);
    }

    public static TFloat valueOf(String s) throws TNumberFormatException {
        return valueOf(parseFloat(s));
    }

    public boolean isNaN() {
        return isNaN(value);
    }

    public boolean isInfinite() {
        return isInfinite(value);
    }

    @NoSideEffects
    public static int compare(float a, float b) {
        var diff = (a > b ? 1 : 0) - (b > a ? 1 : 0);
        return diff != 0 ? diff : (1 / a > 1 / b ? 1 : 0) - (1 / b > 1 / a ? 1 : 0)
                + (b == b ? 1 : 0) - (a == a ? 1 : 0);
    }

    @Override
    public int compareTo(TFloat other) {
        return compare(value, other.value);
    }

    @JSBody(params = "value", script = "return $rt_floatToRawIntBits(value);")
    @Import(name = "teavm_reinterpretFloatToInt")
    @NoSideEffects
    @Unmanaged
    public static native int floatToRawIntBits(float value);

    public static int floatToIntBits(float value) {
        if (isNaN(value)) {
            return 0x7fc00000;
        }
        return floatToRawIntBits(value);
    }

    @JSBody(params = "bits", script = "return $rt_intBitsToFloat(bits);")
    @Import(name = "teavm_reinterpretIntToFloat")
    @NoSideEffects
    @Unmanaged
    public static native float intBitsToFloat(int bits);

    public static String toHexString(float f) {
        if (isNaN(f)) {
            return "NaN";
        } else if (isInfinite(f)) {
            return f > 0 ? "Infinity" : "-Infinity";
        }
        char[] buffer = new char[18];
        int sz = 0;
        int bits = floatToIntBits(f);
        boolean subNormal = false;
        boolean negative = (bits & (1 << 31)) != 0;
        int exp = ((bits >>> 23) & 0xFF) - 127;
        int mantissa = (bits & 0x7FFFFF) << 1;
        if (exp == -127) {
            if (mantissa == 0) {
                return negative ? "-0x0.0p0" : "0x0.0p0";
            }
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

        return new String(buffer, 0, sz);
    }
}
