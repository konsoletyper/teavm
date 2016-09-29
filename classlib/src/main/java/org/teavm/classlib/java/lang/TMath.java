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

import org.teavm.backend.javascript.spi.GeneratedBy;
import org.teavm.interop.Import;

public final class TMath extends TObject {
    public static final double E = 2.71828182845904523536;
    public static final double PI = 3.14159265358979323846;

    private TMath() {
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "sin")
    public static native double sin(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "cos")
    public static native double cos(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "tan")
    public static native double tan(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "asin")
    public static native double asin(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "acos")
    public static native double acos(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "atan")
    public static native double atan(double a);

    public static double toRadians(double angdeg) {
        return angdeg * PI / 180;
    }

    public static double toDegrees(double angrad) {
        return angrad * 180 / PI;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "exp")
    public static native double exp(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "log")
    public static native double log(double a);

    public static double log10(double a) {
        return log(a) / 2.302585092994046 /* log_e 10 */;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "sqrt")
    public static native double sqrt(double a);

    public static double cbrt(double a) {
        return a > 0 ? pow(a, 1.0 / 3) : -pow(-a, 1.0 / 3);
    }

    public static double IEEEremainder(double f1, double f2) {
        int n = (int) (f1 / f2);
        return f1 - n * f2;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "ceil")
    public static native double ceil(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "floor")
    public static native double floor(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "pow")
    public static native double pow(double x, double y);

    public static double rint(double a) {
        return round(a);
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "atan2")
    public static native double atan2(double y, double x);

    public static int round(float a) {
        return (int) (a + signum(a) * 0.5f);
    }

    public static long round(double a) {
        return (long) (a + signum(a) * 0.5);
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "math", name = "random")
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
        return pow(2, getExponent(d) - 52);
    }

    public static float ulp(float d) {
        return (float) pow(2, getExponent(d) - 23);
    }

    public static double signum(double d) {
        return d > 0 ? 1 : d < -0 ? -1 : d;
    }

    public static float signum(float d) {
        return d > 0 ? 1 : d < -0 ? -1 : d;
    }

    public static double sinh(double x) {
        double e = exp(x);
        return (e - 1 / e) / 2;
    }

    public static double cosh(double x) {
        double e = exp(x);
        return (e + 1 / e) / 2;
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
        double[] negativeExponents2 = ExponentConstants.negativeExponents2;
        if (d > 1) {
            int expBit = 1 << (exponents.length - 1);
            for (int i = exponents.length - 1; i >= 0; --i) {
                if (d >= exponents[i]) {
                    d *= negativeExponents[i];
                    exp |= expBit;
                }
                expBit >>>= 1;
            }
        } else if (d < 1) {
            int expBit = 1 << (negativeExponents.length - 1);
            int offset = 0;
            if (d < 0x1p-1022) {
                d *= 0x1p52;
                offset = 52;
            }
            for (int i = negativeExponents2.length - 1; i >= 0; --i) {
                if (d < negativeExponents2[i]) {
                    d *= exponents[i];
                    exp |= expBit;
                }
                expBit >>>= 1;
            }
            exp = -(exp + offset);
        }
        return exp;
    }

    public static int getExponent(float f) {
        f = abs(f);
        int exp = 0;
        float[] exponents = FloatExponents.exponents;
        float[] negativeExponents = FloatExponents.negativeExponents;
        float[] negativeExponents2 = FloatExponents.negativeExponents2;
        if (f > 1) {
            int expBit = 1 << (exponents.length - 1);
            for (int i = exponents.length - 1; i >= 0; --i) {
                if (f >= exponents[i]) {
                    f *= negativeExponents[i];
                    exp |= expBit;
                }
                expBit >>>= 1;
            }
        } else if (f < 1) {
            int expBit = 1 << (negativeExponents.length - 1);
            int offset = 0;
            if (f < 0x1p-126) {
                f *= 0x1p23f;
                offset = 23;
            }
            for (int i = negativeExponents2.length - 1; i >= 0; --i) {
                if (f < negativeExponents2[i]) {
                    f *= exponents[i];
                    exp |= expBit;
                }
                expBit >>>= 1;
            }
            exp = -(exp + offset);
        }
        return exp;
    }

    public static double nextAfter(double start, double direction) {
        if (start == direction) {
            return direction;
        }
        return direction > start ? start + ulp(start) : start - ulp(start);
    }

    public static float nextAfter(float start, double direction) {
        if (start == direction) {
            return start;
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
        public static double[] exponents = { 0x1p1, 0x1p2, 0x1p4, 0x1p8, 0x1p16, 0x1p32, 0x1p64, 0x1p128,
                0x1p256, 0x1p512 };
        public static double[] negativeExponents = { 0x1p-1, 0x1p-2, 0x1p-4, 0x1p-8, 0x1p-16, 0x1p-32,
                0x1p-64, 0x1p-128, 0x1p-256, 0x1p-512 };
        public static double[] negativeExponents2 = { 0x1p-0, 0x1p-1, 0x1p-3, 0x1p-7, 0x1p-15, 0x1p-31,
                0x1p-63, 0x1p-127, 0x1p-255, 0x1p-511 };
    }

    private static class FloatExponents {
        public static float[] exponents = { 0x1p1f, 0x1p2f, 0x1p4f, 0x1p8f, 0x1p16f, 0x1p32f, 0x1p64f };
        public static float[] negativeExponents = { 0x1p-1f, 0x1p-2f, 0x1p-4f, 0x1p-8f, 0x1p-16f, 0x1p-32f,
                0x1p-64f };
        public static float[] negativeExponents2 = { 0x1p-0f, 0x1p-1f, 0x1p-3f, 0x1p-7f, 0x1p-15f, 0x1p-31f,
                0x1p-63f };
    }
}
