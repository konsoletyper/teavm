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

import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.Rename;

/**
 *
 * @author Alexey Andreev
 */
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
    public static final TClass<TFloat> TYPE = TClass.floatClass();
    private float value;

    public TFloat(float value) {
        this.value = value;
    }

    public TFloat(double value) {
        this((float)value);
    }

    public TFloat(TString value) throws TNumberFormatException {
        this(parseFloat(value));
    }

    @Override
    public int intValue() {
        return (int)value;
    }

    @Override
    public long longValue() {
        return (long)value;
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

    public static TString toString(float d) {
        return TString.wrap(new TStringBuilder().append(d).toString());
    }

    @Override
    @Rename("toString")
    public TString toString0() {
        return toString(value);
    }

    @Override
    public boolean equals(TObject other) {
        if (this == other) {
            return true;
        }
        return other instanceof TFloat && ((TFloat)other).value == value;
    }

    @GeneratedBy(FloatNativeGenerator.class)
    public static native boolean isNaN(float v);

    @GeneratedBy(FloatNativeGenerator.class)
    public static native boolean isInfinite(float v);

    @GeneratedBy(FloatNativeGenerator.class)
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

    public static int compare(float f1, float f2) {
        return f1 > f2 ? 1 : f2 < f1 ? -1 : 0;
    }

    @Override
    public int compareTo(TFloat other) {
        return compare(value, other.value);
    }
}
