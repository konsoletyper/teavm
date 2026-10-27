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

public final class TStrictMath extends TObject {
    public static final double E = 2.71828182845904523536;
    public static final double PI = 3.14159265358979323846;
    public static final double TAU = 2 * PI;

    private TStrictMath() {
    }

    public static double sin(double a) {
        return TMath.sin(a);
    }

    public static double cos(double a) {
        return TMath.cos(a);
    }

    public static double tan(double a) {
        return TMath.tan(a);
    }

    public static double asin(double a) {
        return TMath.asin(a);
    }

    public static double acos(double a) {
        return TMath.acos(a);
    }

    public static double atan(double a) {
        return TMath.atan(a);
    }

    public static double toRadians(double angdeg) {
        return TMath.toRadians(angdeg);
    }

    public static double toDegrees(double angrad) {
        return TMath.toDegrees(angrad);
    }

    public static double exp(double a) {
        return TMath.exp(a);
    }

    public static double log(double a) {
        return Fdlibm.log(a);
    }

    /**
     * StrictMath.log and log10 must give fdlibm's results bit for bit, as the JDK's do. Math.log calls the host's
     * Math.log, and a host's results can differ in the last bit (V8 in Node 24 does for some inputs).
     * These are ports of __ieee754_log and __ieee754_log10 from fdlibm 5.3's e_log.c ("@(#)e_log.c 1.3 95/01/18")
     * and e_log10.c ("@(#)e_log10.c 1.3 95/01/18", netlib.org/fdlibm), with the same constants and steps, and
     * with those files' notice:
     *
     * <p>Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
     *
     * <p>Developed at SunSoft, a Sun Microsystems, Inc. business. Permission to use, copy, modify, and distribute
     * this software is freely granted, provided that this notice is preserved.
     */
    private static final class Fdlibm {
        private static final double LN2_HI = 6.93147180369123816490e-01;
        private static final double LN2_LO = 1.90821492927058770002e-10;
        private static final double TWO54 = 1.80143985094819840000e+16;
        private static final double LG1 = 6.666666666666735130e-01;
        private static final double LG2 = 3.999999999940941908e-01;
        private static final double LG3 = 2.857142874366239149e-01;
        private static final double LG4 = 2.222219843214978396e-01;
        private static final double LG5 = 1.818357216161805012e-01;
        private static final double LG6 = 1.531383769920937332e-01;
        private static final double LG7 = 1.479819860511658591e-01;
        private static final double IVLN10 = 4.34294481903251816668e-01;
        private static final double LOG10_2HI = 3.01029995663611771306e-01;
        private static final double LOG10_2LO = 3.69423907715893078616e-13;

        private Fdlibm() {
        }

        // Takes bits that the caller already read: on JS, longs are BigInts, so each conversion is costly.
        private static double withHigh(long bits, int high) {
            return Double.longBitsToDouble(((long) high << 32) | (bits & 0xFFFFFFFFL));
        }

        static double log(double x) {
            long bits = Double.doubleToRawLongBits(x);
            int hx = (int) (bits >>> 32);
            int k = 0;
            if (hx < 0x00100000) {
                // x < 2**-1022
                if ((bits & 0x7fffffffffffffffL) == 0) {
                    return -TWO54 / 0.0; // log(+-0) = -inf
                }
                if (hx < 0) {
                    return (x - x) / 0.0; // log(-#) = NaN
                }
                k -= 54;
                x *= TWO54; // subnormal number, scale up x
                bits = Double.doubleToRawLongBits(x);
                hx = (int) (bits >>> 32);
            }
            if (hx >= 0x7ff00000) {
                return x + x;
            }
            k += (hx >> 20) - 1023;
            hx &= 0x000fffff;
            int i = (hx + 0x95f64) & 0x100000;
            x = withHigh(bits, hx | (i ^ 0x3ff00000)); // normalize x or x/2
            k += i >> 20;
            double f = x - 1.0;
            double dk;
            double r;
            if ((0x000fffff & (2 + hx)) < 3) {
                // |f| < 2**-20
                if (f == 0.0) {
                    if (k == 0) {
                        return 0.0;
                    }
                    dk = k;
                    return dk * LN2_HI + dk * LN2_LO;
                }
                r = f * f * (0.5 - 0.33333333333333333 * f);
                if (k == 0) {
                    return f - r;
                }
                dk = k;
                return dk * LN2_HI - ((r - dk * LN2_LO) - f);
            }
            double s = f / (2.0 + f);
            dk = k;
            double z = s * s;
            i = hx - 0x6147a;
            double w = z * z;
            int j = 0x6b851 - hx;
            double t1 = w * (LG2 + w * (LG4 + w * LG6));
            double t2 = z * (LG1 + w * (LG3 + w * (LG5 + w * LG7)));
            i |= j;
            r = t2 + t1;
            if (i > 0) {
                double hfsq = 0.5 * f * f;
                if (k == 0) {
                    return f - (hfsq - s * (hfsq + r));
                }
                return dk * LN2_HI - ((hfsq - (s * (hfsq + r) + dk * LN2_LO)) - f);
            }
            if (k == 0) {
                return f - s * (f - r);
            }
            return dk * LN2_HI - ((s * (f - r) - dk * LN2_LO) - f);
        }

        static double log10(double x) {
            long bits = Double.doubleToRawLongBits(x);
            int hx = (int) (bits >>> 32);
            int k = 0;
            if (hx < 0x00100000) {
                // x < 2**-1022
                if ((bits & 0x7fffffffffffffffL) == 0) {
                    return -TWO54 / 0.0; // log(+-0) = -inf
                }
                if (hx < 0) {
                    return (x - x) / 0.0; // log(-#) = NaN
                }
                k -= 54;
                x *= TWO54; // subnormal number, scale up x
                bits = Double.doubleToRawLongBits(x);
                hx = (int) (bits >>> 32);
            }
            if (hx >= 0x7ff00000) {
                return x + x;
            }
            k += (hx >> 20) - 1023;
            int i = k >>> 31;
            hx = (hx & 0x000fffff) | ((0x3ff - i) << 20);
            double y = k + i;
            x = withHigh(bits, hx);
            double z = y * LOG10_2LO + IVLN10 * log(x);
            return z + y * LOG10_2HI;
        }
    }

    public static double log10(double a) {
        return Fdlibm.log10(a);
    }

    public static double sqrt(double a) {
        return TMath.sqrt(a);
    }

    public static double cbrt(double a) {
        return TMath.cbrt(a);
    }

    public static double IEEEremainder(double f1, double f2) {
        return TMath.IEEEremainder(f1, f2);
    }

    public static double ceil(double a) {
        return TMath.ceil(a);
    }

    public static double floor(double a) {
        return TMath.floor(a);
    }

    public static double rint(double a) {
        return TMath.rint(a);
    }

    public static double atan2(double y, double x) {
        return TMath.atan2(y, x);
    }

    public static double pow(double a, double b) {
        return TMath.pow(a, b);
    }

    public static int round(float a) {
        return TMath.round(a);
    }

    public static long round(double a) {
        return TMath.round(a);
    }

    public static int floorDiv(int a, int b) {
        return TMath.floorDiv(a, b);
    }

    public static long floorDiv(long a, int b) {
        return TMath.floorDiv(a, b);
    }

    public static long floorDiv(long a, long b) {
        return TMath.floorDiv(a, b);
    }

    public static int floorMod(int a, int b) {
        return TMath.floorMod(a, b);
    }

    public static int floorMod(long a, int b) {
        return TMath.floorMod(a, b);
    }

    public static long floorMod(long a, long b) {
        return TMath.floorMod(a, b);
    }

    public static int addExact(int a, int b) {
        return TMath.addExact(a, b);
    }

    public static long addExact(long a, long b) {
        return TMath.addExact(a, b);
    }

    public static int subtractExact(int a, int b) {
        return TMath.subtractExact(a, b);
    }

    public static long subtractExact(long a, long b) {
        return TMath.subtractExact(a, b);
    }

    public static int multiplyExact(int a, int b) {
        return TMath.multiplyExact(a, b);
    }

    public static long multiplyExact(long a, int b) {
        return TMath.multiplyExact(a, b);
    }

    public static long multiplyExact(long a, long b) {
        return TMath.multiplyExact(a, b);
    }

    public static int divideExact(int a, int b) {
        return TMath.divideExact(a, b);
    }

    public static long divideExact(long a, long b) {
        return TMath.divideExact(a, b);
    }

    public static int incrementExact(int a) {
        return TMath.incrementExact(a);
    }

    public static long incrementExact(long a) {
        return TMath.incrementExact(a);
    }

    public static int decrementExact(int a) {
        return TMath.decrementExact(a);
    }

    public static long decrementExact(long a) {
        return TMath.decrementExact(a);
    }

    public static int negateExact(int a) {
        return TMath.negateExact(a);
    }

    public static long negateExact(long a) {
        return TMath.negateExact(a);
    }

    public static int toIntExact(long value) {
        return TMath.toIntExact(value);
    }

    public static double random() {
        return TMath.random();
    }

    public static int abs(int a) {
        return TMath.abs(a);
    }

    public static long abs(long a) {
        return TMath.abs(a);
    }

    public static float abs(float a) {
        return TMath.abs(a);
    }

    public static double abs(double a) {
        return TMath.abs(a);
    }

    public static int max(int a, int b) {
        return TMath.max(a, b);
    }

    public static long max(long a, long b) {
        return TMath.max(a, b);
    }

    public static float max(float a, float b) {
        return TMath.max(a, b);
    }

    public static double max(double a, double b) {
        return TMath.max(a, b);
    }

    public static int min(int a, int b) {
        return TMath.min(a, b);
    }

    public static long min(long a, long b) {
        return TMath.min(a, b);
    }

    public static float min(float a, float b) {
        return TMath.min(a, b);
    }

    public static double min(double a, double b) {
        return TMath.min(a, b);
    }

    public static double ulp(double d) {
        return TMath.ulp(d);
    }

    public static float ulp(float f) {
        return TMath.ulp(f);
    }

    public static double signum(double d) {
        return TMath.signum(d);
    }

    public static float signum(float f) {
        return TMath.signum(f);
    }

    public static double sinh(double x) {
        return TMath.sinh(x);
    }

    public static double cosh(double x) {
        return TMath.cosh(x);
    }

    public static double tanh(double x) {
        return TMath.tanh(x);
    }

    public static double hypot(double x, double y) {
        return TMath.hypot(x, y);
    }

    public static double fma(double a, double b, double c) {
        return TMath.fma(a, b, c);
    }

    public static float fma(float a, float b, float c) {
        return TMath.fma(a, b, c);
    }

    public static double expm1(double x) {
        return TMath.expm1(x);
    }

    public static double log1p(double x) {
        return TMath.log1p(x);
    }

    public static double copySign(double magnitude, double sign) {
        return TMath.copySign(magnitude, sign);
    }

    public static float copySign(float magnitude, float sign) {
        return TMath.copySign(magnitude, sign);
    }

    public static int getExponent(float f) {
        return TMath.getExponent(f);
    }

    public static int getExponent(double d) {
        return TMath.getExponent(d);
    }

    public static double nextAfter(double start, double direction) {
        return TMath.nextAfter(start, direction);
    }

    public static float nextAfter(float start, double direction) {
        return TMath.nextAfter(start, direction);
    }

    public static double nextUp(double d) {
        return TMath.nextUp(d);
    }

    public static float nextUp(float f) {
        return TMath.nextUp(f);
    }

    public static int clamp(long value, int min, int max) {
        return TMath.clamp(value, min, max);
    }

    public static long clamp(long value, long min, long max) {
        return TMath.clamp(value, min, max);
    }

    public static double clamp(double value, double min, double max) {
        return TMath.clamp(value, min, max);
    }

    public static float clamp(float value, float min, float max) {
        return TMath.clamp(value, min, max);
    }
}
