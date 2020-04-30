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
import org.teavm.classlib.PlatformDetector;
import org.teavm.interop.Import;
import org.teavm.interop.NoSideEffects;
import org.teavm.interop.Unmanaged;

@NoSideEffects
public final class TMath extends TObject {
    public static final double E = 2.71828182845904523536;
    public static final double PI = 3.14159265358979323846;

    private TMath() {
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "sin")
    @Unmanaged
    public static native double sin(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "cos")
    @Unmanaged
    public static native double cos(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "tan")
    @Unmanaged
    public static native double tan(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "asin")
    @Unmanaged
    public static native double asin(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "acos")
    @Unmanaged
    public static native double acos(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "atan")
    @Unmanaged
    public static native double atan(double a);

    public static double toRadians(double angdeg) {
        return angdeg * PI / 180;
    }

    public static double toDegrees(double angrad) {
        return angrad * 180 / PI;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "exp")
    @Unmanaged
    public static native double exp(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "log")
    @Unmanaged
    public static native double log(double a);

    public static double log10(double a) {
        return log(a) / 2.302585092994046 /* log_e 10 */;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "sqrt")
    @Unmanaged
    public static native double sqrt(double a);

    public static double cbrt(double a) {
        return a > 0 ? pow(a, 1.0 / 3) : -pow(-a, 1.0 / 3);
    }

    public static double IEEEremainder(double f1, double f2) {
        int n = (int) (f1 / f2);
        return f1 - n * f2;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "ceil")
    @Unmanaged
    public static native double ceil(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "floor")
    @Unmanaged
    public static native double floor(double a);

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "pow")
    @Unmanaged
    public static native double pow(double x, double y);

    public static double rint(double a) {
        return round(a);
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "atan2")
    @Unmanaged
    public static native double atan2(double y, double x);

    public static int round(float a) {
        return (int) (a + signum(a) * 0.5f);
    }

    public static long round(double a) {
        return (long) (a + signum(a) * 0.5);
    }

    @Unmanaged
    public static double random() {
        return PlatformDetector.isC() ? randomC() : randomImpl();
    }

    @Import(name = "teavm_rand")
    private static native double randomC();

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "random")
    private static native double randomImpl();

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
        if (TDouble.isNaN(d)) {
            return d;
        } else if (TDouble.isInfinite(d)) {
            return TDouble.POSITIVE_INFINITY;
        }

        if (TDouble.isNaN(d)) {
            return d;
        } else if (TDouble.isInfinite(d)) {
            return TDouble.POSITIVE_INFINITY;
        }

        long bits = TDouble.doubleToLongBits(d);
        bits &= 0xEFF0000000000000L;
        if (bits >= 53L << 52L) {
            bits -= 52L << 52L;
        } else {
            int exponent = (int) (bits >> 52);
            bits = 1 << Math.max(0, exponent - 1);
        }
        return TDouble.longBitsToDouble(bits);
    }

    public static float ulp(float d) {
        if (TFloat.isNaN(d)) {
            return d;
        } else if (TFloat.isInfinite(d)) {
            return TFloat.POSITIVE_INFINITY;
        }

        int bits = TFloat.floatToIntBits(d);
        bits &= 0x7F800000;
        if (bits >= 24L << 23L) {
            bits -= 23L << 23L;
        } else {
            int exponent = bits >> 23;
            bits = 1 << Math.max(0, exponent - 1);
        }
        return TFloat.intBitsToFloat(bits);
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
        return sqrt(x * x + y * y);
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
        long bits = TDouble.doubleToLongBits(d);
        int exponent = (int) ((bits >> 52) & 0x7FF);
        return exponent - 1023;
    }

    public static int getExponent(float f) {
        int bits = TFloat.floatToIntBits(f);
        int exponent = (bits >> 23) & 0xF;
        return exponent + 128;
    }

    public static double nextAfter(double start, double direction) {
        if (start == direction) {
            return direction;
        }
        return direction > start ? nextUp(start) : nextDown(start);
    }

    public static float nextAfter(float start, double direction) {
        if (start == direction) {
            return start;
        }
        return direction > start ? nextUp(start) : nextDown(start);
    }

    public static double nextUp(double d) {
        if (TDouble.isNaN(d)) {
            return d;
        }
        if (d == TDouble.POSITIVE_INFINITY) {
            return d;
        }
        long bits = TDouble.doubleToLongBits(d);
        boolean negative = (bits & (1L << 63)) != 0;
        if (negative) {
            bits--;
        } else {
            bits++;
        }
        return TDouble.longBitsToDouble(bits);
    }

    public static float nextUp(float d) {
        if (TFloat.isNaN(d)) {
            return d;
        }
        if (d == TFloat.POSITIVE_INFINITY) {
            return d;
        }
        int bits = TFloat.floatToIntBits(d);
        boolean negative = (bits & (1L << 31)) != 0;
        if (negative) {
            bits--;
        } else {
            bits++;
        }
        return TFloat.intBitsToFloat(bits);
    }

    public static double nextDown(double d) {
        if (TDouble.isNaN(d)) {
            return d;
        }
        if (d == TDouble.NEGATIVE_INFINITY) {
            return d;
        }
        long bits = TDouble.doubleToLongBits(d);
        boolean negative = (bits & (1L << 63)) != 0;
        if (negative) {
            bits++;
        } else {
            bits--;
        }
        return TDouble.longBitsToDouble(bits);
    }

    public static float nextDown(float d) {
        if (TFloat.isNaN(d)) {
            return d;
        }
        if (d == TFloat.POSITIVE_INFINITY) {
            return d;
        }
        int bits = TFloat.floatToIntBits(d);
        boolean negative = (bits & (1L << 31)) != 0;
        if (negative) {
            bits++;
        } else {
            bits--;
        }
        return TFloat.intBitsToFloat(bits);
    }
}
