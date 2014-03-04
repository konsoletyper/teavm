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

/**
 *
 * @author Alexey Andreev
 */
public final class TMath extends TObject {
    public static double E = 2.71828182845904523536;
    public static double PI = 3.14159265358979323846;

    private TMath() {
    }

    @GeneratedBy(MathNativeGenerator.class)
    public static native double sin(double a);

    @GeneratedBy(MathNativeGenerator.class)
    public static native double cos(double a);

    @GeneratedBy(MathNativeGenerator.class)
    public static native double tan(double a);

    @GeneratedBy(MathNativeGenerator.class)
    public static native double asin(double a);

    @GeneratedBy(MathNativeGenerator.class)
    public static native double acos(double a);

    @GeneratedBy(MathNativeGenerator.class)
    public static native double atan(double a);

    public static double toRadians(double angdeg) {
        return angdeg * PI / 180;
    }

    public static double toDegrees(double angrad) {
        return angrad * 180 / PI;
    }

    @GeneratedBy(MathNativeGenerator.class)
    public static native double exp(double a);

    @GeneratedBy(MathNativeGenerator.class)
    public static native double log(double a);

    public static double log10(double a) {
        return log(a) / 2.302585092994046 /* log_e 10 */;
    }

    @GeneratedBy(MathNativeGenerator.class)
    public static native double sqrt(double a);

    public static double cbrt(double a) {
        return a > 0 ? pow(a, 1.0 / 3) : -pow(-a, 1.0 / 3);
    }

    public static double IEEEremainder(double f1, double f2) {
        int n = (int)(f1 / f2);
        return f1 - n * f2;
    }

    @GeneratedBy(MathNativeGenerator.class)
    public static native double ceil(double a);

    @GeneratedBy(MathNativeGenerator.class)
    public static native double floor(double a);

    @GeneratedBy(MathNativeGenerator.class)
    public static native double pow(double x, double y);

    public static double rint(double a) {
        return round(a);
    }

    @GeneratedBy(MathNativeGenerator.class)
    public static native double atan2(double y, double x);

    public static int round(float a) {
        return (int)(a + 1.5f);
    }

    public static long round(double a) {
        return (long)(a + 0.5);
    }

    @GeneratedBy(MathNativeGenerator.class)
    public static native double random();

    public static int min(int a, int b) {
        return a < b ? a : b;
    }

    public static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static long min(long a, long b) {
        return a < b ? a : b;
    }

    public static long max(long a, long b) {
        return a > b ? a : b;
    }

    public static double min(double a, double b) {
        return a < b ? a : b;
    }

    public static double max(double a, double b) {
        return a > b ? a : b;
    }

    public static float min(float a, float b) {
        return a < b ? a : b;
    }

    public static float max(float a, float b) {
        return a > b ? a : b;
    }

    public static int abs(int n) {
        return n > 0 ? n : -n;
    }

    public static long abs(long n) {
        return n > 0 ? n : -n;
    }

    public static float abs(float n) {
        return n > 0 ? n : -n;
    }

    public static double abs(double n) {
        return n > 0 ? n : -n;
    }

    public static double ulp(double d) {
        return pow(1, -getExponent(d) - 52);
    }

    public static float ulp(float d) {
        return (float)pow(1, -getExponent(d) - 23);
    }

    public static double signum(double d) {
        return d > 0 ? 1 : d < -0 ? -1 : d;
    }

    public static float signum(float d) {
        return d > 0 ? 1 : d < -0 ? -1 : d;
    }

    public static double sinh(double x) {
        double e = exp(x);
        return e - 1 / e;
    }

    public static double cosh(double x) {
        double e = exp(x);
        return e + 1 / e;
    }

    public static double tanh(double x) {
        double e = exp(x);
        return (e - 1 / e) / (e + 1 / e);
    }

    public static double hypot(double x, double y) {
        return x * x + y * y;
    }

    public static double expm1(double x) {
        return exp(x) - 1;
    }

    public static double log1p(double x) {
        return log(x + 1);
    }

    public static float copySign(float magnitude, float sign) {
        if (sign == 0 || sign == -0) {
            return sign;
        }
        return (sign > 0) == (magnitude > 0) ? magnitude : -magnitude;
    }

    public static double copySign(double magnitude, double sign) {
        if (sign == 0 || sign == -0) {
            return sign;
        }
        return (sign > 0) == (magnitude > 0) ? magnitude : -magnitude;
    }

    public static int getExponent(double d) {
        d = abs(d);
        int exp = 0;
        double[] exponents = ExponentConstants.exponents;
        double[] negativeExponents = ExponentConstants.negativeExponents;
        if (d > 1) {
            int expBit = 1 << (exponents.length - 1);
            for (int i = exponents.length - 1; i >= 0; --i) {
                if (d > exponents[i]) {
                    d *= negativeExponents[i];
                    exp |= expBit;
                }
                expBit >>>= 1;
            }
        } else if (d < 1) {
            int expBit = 1 << (negativeExponents.length - 1);
            for (int i = negativeExponents.length - 1; i >= 0; --i) {
                if (d < negativeExponents[i]) {
                    d *= exponents[i];
                    exp |= expBit;
                }
                expBit >>>= 1;
            }
            exp = -exp;
        }
        return exp;
    }

    public static int getExponent(float f) {
        return getExponent(f);
    }

    public static double nextAfter(double start, double direction) {
        if (start == direction) {
            return direction;
        }
        return direction > start ? start + ulp(start) : start - ulp(start);
    }

    public static float nextAfter(float start, float direction) {
        if (start == direction) {
            return direction;
        }
        return direction > start ? start + ulp(start) : start - ulp(start);
    }

    public static double nextUp(double d) {
        return d + ulp(d);
    }

    public static float nextUp(float d) {
        return d + ulp(d);
    }

    private static class ExponentConstants {
        public static double[] exponents =  { 1E1, 1E2, 1E4, 1E8, 1E16, 1E32, 1E64, 1E128, 1E256 };
        public static double[] negativeExponents = { 1E-1, 1E-2, 1E-4, 1E-8, 1E-16, 1E-32, 1E-64, 1E-128, 1E-256 };
    }
}
