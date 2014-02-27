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
public class TDouble extends TNumber implements TComparable<TDouble> {
    public static double POSITIVE_INFINITY = 1 / 0.0;
    public static double NEGATIVE_INFINITY = -POSITIVE_INFINITY;
    private double value;

    public TDouble(double value) {
        this.value = value;
    }

    @Override
    public double doubleValue() {
        return value;
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
        return (float)value;
    }

    public static TDouble valueOf(double d) {
        return new TDouble(d);
    }

    public static TString toString(double d) {
        return TString.wrap(new TStringBuilder().append(d).toString());
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

    private static double decimalExponent(int n) {
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
    @Rename("toString")
    public TString toString0() {
        return toString(value);
    }

    @Override
    public boolean equals(TObject other) {
        if (this == other) {
            return true;
        }
        return other instanceof TDouble && ((TDouble)other).value == value;
    }

    public static int compare(double a, double b) {
        return a > b ? 1 : a < b ? -1 : 0;
    }

    @Override
    public int compareTo(TDouble other) {
        return compare(value, other.value);
    }

    @GeneratedBy(DoubleNativeGenerator.class)
    public static native boolean isNaN(double v);

    @GeneratedBy(DoubleNativeGenerator.class)
    public static native boolean isInfinite(double v);
}
