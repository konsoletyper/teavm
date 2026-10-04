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
    public static final double TAU = 2 * PI;

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

    @Import(name = "fma")
    private static native double fmaC(double a, double b, double c);

    @Import(name = "fmaf")
    private static native float fmaC(float a, float b, float c);

    public static double fma(double a, double b, double c) {
        if (PlatformDetector.isC()) {
            return fmaC(a, b, c);
        }
        // Fast path: emulation of FMA with error-free transformations and rounding to odd, see
        // S. Boldo, G. Melquiond "Emulation of FMA and correctly rounded sums: proved algorithms using rounding
        // to odd". It's only valid when there's no overflow or underflow in intermediate results,
        // otherwise fall back to exact computation with integers. Note that range check also filters out
        // zeros, infinities and NaNs.
        double p = a * b;
        double absP = Math.abs(p);
        if (absP >= 0x1p-900 && absP < 0x1p1000 && Math.abs(c) < 0x1p1000
                && Math.abs(a) < 0x1p995 && Math.abs(b) < 0x1p995) {
            // Exact product p + pl, see Dekker's TwoProduct
            double t = 134217729.0 * a;
            double ah = t - (t - a);
            double al = a - ah;
            t = 134217729.0 * b;
            double bh = t - (t - b);
            double bl = b - bh;
            double pl = ((ah * bh - p) + ah * bl + al * bh) + al * bl;

            // Exact sum s + sl = c + p, see Knuth's TwoSum
            double s = c + p;
            double bv = s - c;
            double sl = (c - (s - bv)) + (p - bv);

            // sl + pl, rounded to odd
            double v = sl + pl;
            bv = v - sl;
            double ve = (sl - (v - bv)) + (pl - bv);
            if (ve != 0) {
                v = roundToOdd(v, ve);
            }

            double result = s + v;
            if (Math.abs(result) >= 0x1p-960) {
                return result;
            }
        }

        if (!Double.isFinite(a) || !Double.isFinite(b) || a == 0 || b == 0) {
            return a * b + c;
        }
        if (!Double.isFinite(c)) {
            return c;
        }
        return fmaSlow(a, b, c);
    }

    public static float fma(float a, float b, float c) {
        if (PlatformDetector.isC()) {
            return fmaC(a, b, c);
        }
        // Product of two floats is exact in double, then sum rounded to odd guarantees that
        // subsequent rounding to float gives correctly rounded result.
        double p = (double) a * b;
        double s = p + c;
        if (Double.isFinite(s)) {
            double bv = s - p;
            double e = (p - (s - bv)) + (c - bv);
            if (e != 0) {
                s = roundToOdd(s, e);
            }
        }
        return (float) s;
    }

    private static double roundToOdd(double value, double error) {
        long bits = Double.doubleToRawLongBits(value);
        if ((bits & 1) == 0) {
            bits += (error > 0) == (value > 0) ? 1 : -1;
            value = Double.longBitsToDouble(bits);
        }
        return value;
    }

    private static double fmaSlow(double a, double b, double c) {
        long aBits = Double.doubleToRawLongBits(a);
        long bBits = Double.doubleToRawLongBits(b);
        long cBits = Double.doubleToRawLongBits(c);

        // 106-bit product of significands
        long ma = significandMagnitude(aBits);
        long mb = significandMagnitude(bBits);
        long a0 = ma & 0xFFFFFFFFL;
        long a1 = ma >>> 32;
        long b0 = mb & 0xFFFFFFFFL;
        long b1 = mb >>> 32;
        long p00 = a0 * b0;
        long mid = a0 * b1 + a1 * b0 + (p00 >>> 32);
        long hi = a1 * b1 + (mid >>> 32);
        long lo = (mid << 32) | (p00 & 0xFFFFFFFFL);

        // Normalize 128-bit numbers so that the highest bit is 125, which leaves room for carry
        int length = bitLength128(hi, lo);
        int top = significandExponent(aBits) + significandExponent(bBits) + length - 1;
        int shift = 126 - length;
        if (shift >= 64) {
            hi = lo << (shift - 64);
            lo = 0;
        } else {
            hi = (hi << shift) | (lo >>> (64 - shift));
            lo <<= shift;
        }
        boolean negative = (aBits ^ bBits) < 0;

        long mc = significandMagnitude(cBits);
        if (mc != 0) {
            int cLength = 64 - Long.numberOfLeadingZeros(mc);
            int cTop = significandExponent(cBits) + cLength - 1;
            long cHi = mc << (126 - cLength - 64);
            long cLo = 0;
            boolean cNegative = cBits < 0;

            if (cTop > top || cTop == top && Long.compareUnsigned(cHi, hi) > 0) {
                long tmp = cHi;
                cHi = hi;
                hi = tmp;
                cLo = lo;
                lo = 0;
                int tmpTop = cTop;
                cTop = top;
                top = tmpTop;
                boolean tmpNegative = cNegative;
                cNegative = negative;
                negative = tmpNegative;
            }

            // Align the smaller number, collecting shifted out bits into sticky bit
            int d = top - cTop;
            if (d > 0) {
                boolean sticky;
                if (d >= 128) {
                    sticky = true;
                    cLo = 0;
                    cHi = 0;
                } else if (d >= 64) {
                    sticky = cLo != 0 || d > 64 && (cHi << (128 - d)) != 0;
                    cLo = cHi >>> (d - 64);
                    cHi = 0;
                } else {
                    sticky = (cLo << (64 - d)) != 0;
                    cLo = (cLo >>> d) | (cHi << (64 - d));
                    cHi >>>= d;
                }
                if (sticky) {
                    cLo |= 1;
                }
            }

            if (negative == cNegative) {
                long sum = lo + cLo;
                hi += cHi + (Long.compareUnsigned(sum, lo) < 0 ? 1 : 0);
                lo = sum;
            } else {
                long diff = lo - cLo;
                hi -= cHi + (Long.compareUnsigned(lo, cLo) < 0 ? 1 : 0);
                lo = diff;
            }
        }
        if (hi == 0 && lo == 0) {
            return 0;
        }

        // Round to 53 bits or to subnormal precision
        int lowestExponent = top - 125;
        length = bitLength128(hi, lo);
        int resultExponent = Math.max(lowestExponent + length - 53, -1074);
        shift = resultExponent - lowestExponent;
        long result;
        if (shift <= 0) {
            result = lo << -shift;
        } else if (shift > length) {
            result = 0;
        } else {
            result = shiftRight128(hi, lo, shift);
            if (testBit128(hi, lo, shift - 1) && ((result & 1) != 0 || hasLowerBits128(hi, lo, shift - 1))) {
                result++;
            }
        }

        // Significand includes implicit bit, so adding it to exponent field gives proper value,
        // even if rounding caused carry to the next power of two or if result is subnormal
        long bits = result + ((long) (resultExponent + 1074) << 52);
        if (resultExponent > 971 || bits >= 0x7FF0000000000000L) {
            bits = 0x7FF0000000000000L;
        }
        if (negative) {
            bits |= 0x8000000000000000L;
        }
        return Double.longBitsToDouble(bits);
    }

    private static int bitLength128(long hi, long lo) {
        return hi != 0 ? 128 - Long.numberOfLeadingZeros(hi) : 64 - Long.numberOfLeadingZeros(lo);
    }

    private static long shiftRight128(long hi, long lo, int shift) {
        if (shift < 64) {
            return (lo >>> shift) | (hi << (64 - shift));
        } else {
            return hi >>> (shift - 64);
        }
    }

    private static boolean testBit128(long hi, long lo, int bit) {
        return bit < 64 ? ((lo >>> bit) & 1) != 0 : ((hi >>> (bit - 64)) & 1) != 0;
    }

    private static boolean hasLowerBits128(long hi, long lo, int count) {
        if (count == 0) {
            return false;
        } else if (count < 64) {
            return (lo << (64 - count)) != 0;
        } else if (count == 64) {
            return lo != 0;
        } else {
            return lo != 0 || (hi << (128 - count)) != 0;
        }
    }

    private static long significandMagnitude(long bits) {
        long result = bits & 0xFFFFFFFFFFFFFL;
        if ((bits & 0x7FF0000000000000L) != 0) {
            result |= 0x10000000000000L;
        }
        return result;
    }

    private static int significandExponent(long bits) {
        int biased = (int) ((bits >>> 52) & 0x7FF);
        return Math.max(biased, 1) - 1075;
    }

    public static double pow(double x, double y) {
        return powImpl(x, y);
    }

    @GeneratedBy(MathNativeGenerator.class)
    @Import(module = "teavmMath", name = "pow")
    @Unmanaged
    private static native double powImpl(double x, double y);

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

    public static int floorDiv(int a, int b) {
        int div = a / b;
        return (a ^ b) < 0 && div * b != a ? div - 1 : div;
    }

    public static int floorDivExact(int a, int b) {
        if (a == Integer.MIN_VALUE && b == -1) {
            throw new ArithmeticException();
        }
        return floorDiv(a, b);
    }

    public static long floorDiv(long a, int b) {
        return floorDiv(a, (long) b);
    }

    public static long floorDiv(long a, long b) {
        long div = a / b;
        return (a ^ b) < 0 && div * b != a ? div - 1 : div;
    }

    public static long floorDivExact(long a, long b) {
        if (a == Long.MIN_VALUE && b == -1) {
            throw new ArithmeticException();
        }
        return floorDiv(a, b);
    }

    public static int ceilDiv(int a, int b) {
        int div = a / b;
        return (a ^ b) >= 0 && div * b != a ? div + 1 : div;
    }

    public static int ceilDivExact(int a, int b) {
        if (a == Integer.MIN_VALUE && b == -1) {
            throw new ArithmeticException();
        }
        return ceilDiv(a, b);
    }

    public static long ceilDiv(long a, int b) {
        return ceilDiv(a, (long) b);
    }

    public static long ceilDiv(long a, long b) {
        long div = a / b;
        return (a ^ b) >= 0 && div * b != a ? div + 1 : div;
    }

    public static long ceilDivExact(long a, long b) {
        if (a == Long.MIN_VALUE && b == -1) {
            throw new ArithmeticException();
        }
        return ceilDiv(a, b);
    }

    public static int floorMod(int a, int b) {
        int mod = a % b;
        return (a ^ b) < 0 && mod != 0 ? mod + b : mod;
    }

    public static int floorMod(long a, int b) {
        return (int) floorMod(a, (long) b);
    }

    public static long floorMod(long a, long b) {
        long mod = a % b;
        return (a ^ b) < 0 && mod != 0 ? mod + b : mod;
    }

    public static int ceilMod(int a, int b) {
        int mod = a % b;
        return (a ^ b) >= 0 && mod != 0 ? mod - b : mod;
    }

    public static int ceilMod(long a, int b) {
        return (int) ceilMod(a, (long) b);
    }

    public static long ceilMod(long a, long b) {
        long mod = a % b;
        return (a ^ b) >= 0 && mod != 0 ? mod - b : mod;
    }

    public static int incrementExact(int a) {
        if (a == Integer.MAX_VALUE) {
            throw new ArithmeticException();
        }
        return a + 1;
    }

    public static long incrementExact(long a) {
        if (a == Long.MAX_VALUE) {
            throw new ArithmeticException();
        }
        return a + 1L;
    }

    public static int decrementExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return a - 1;
    }

    public static long decrementExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return a - 1L;
    }

    public static int negateExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return -a;
    }

    public static long negateExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return -a;
    }

    public static int toIntExact(long value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new ArithmeticException();
        }
        return (int) value;
    }

    public static int addExact(int a, int b) {
        int sum = a + b;
        if ((a ^ sum) < 0 && (a ^ b) >= 0) { // a and b samesigned, but sum is not
            throw new ArithmeticException();
        }
        return sum;
    }

    public static long addExact(long a, long b) {
        long sum = a + b;
        if ((a ^ sum) < 0 && (a ^ b) >= 0) {
            throw new ArithmeticException();
        }
        return sum;
    }

    public static int subtractExact(int a, int b) {
        int result = a - b;
        if ((a ^ result) < 0 && (a ^ b) < 0) {
            throw new ArithmeticException();
        }
        return result;
    }

    public static long subtractExact(long a, long b) {
        long result = a - b;
        if ((a ^ result) < 0 && (a ^ b) < 0) {
            throw new ArithmeticException();
        }
        return result;
    }

    public static int multiplyExact(int a, int b) {
        if (b == 1) {
            return a;
        } else if (a == 1) {
            return b;
        } else if (a == 0 || b == 0) {
            return 0;
        }
        int total = a * b;
        if ((a == Integer.MIN_VALUE && b == -1) || (b == Integer.MIN_VALUE && a == -1) || total / b != a) {
            throw new ArithmeticException();
        }
        return total;
    }

    public static long multiplyExact(long a, int b) {
        return multiplyExact(a, (long) b);
    }

    public static long multiplyExact(long a, long b) {
        if (b == 1) {
            return a;
        } else if (a == 1) {
            return b;
        } else if (a == 0 || b == 0) {
            return 0;
        }
        long total = a * b;
        if ((a == Long.MIN_VALUE && b == -1) || (b == Long.MIN_VALUE && a == -1) || total / b != a) {
            throw new ArithmeticException();
        }
        return total;
    }

    public static int divideExact(int a, int b) {
        if (a == Integer.MIN_VALUE && b == -1) {
            throw new ArithmeticException();
        }
        return a / b;
    }

    public static long divideExact(long a, long b) {
        if (a == Long.MIN_VALUE && b == -1) {
            throw new ArithmeticException();
        }
        return a / b;
    }

    @Unmanaged
    public static double random() {
        if (PlatformDetector.isC()) {
            return randomC();
        } else if (PlatformDetector.isWebAssemblyGC()) {
            return randomWasmGC();
        } else {
            return randomImpl();
        }
    }

    @Import(name = "teavm_rand")
    private static native double randomC();

    @GeneratedBy(MathNativeGenerator.class)
    private static native double randomImpl();

    @Import(module = "teavmMath", name = "random")
    private static native double randomWasmGC();

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

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    @Unmanaged
    private static native float minImpl(double a, double b);

    @Unmanaged
    public static double min(double a, double b) {
        if (PlatformDetector.isJavaScript()) {
            return minImpl(a, b);
        }
        if (a != a) {
            return a;
        }
        if (a == 0.0 && b == 0.0 && 1 / b == Double.NEGATIVE_INFINITY) {
            return b;
        }
        return a <= b ? a : b;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    @Unmanaged
    private static native float maxImpl(double a, double b);

    @Unmanaged
    public static double max(double a, double b) {
        if (PlatformDetector.isJavaScript()) {
            return maxImpl(a, b);
        }
        if (a != a) {
            return a;
        }
        if (a == 0.0 && b == 0.0 && 1 / a == Double.NEGATIVE_INFINITY) {
            return b;
        }
        return a >= b ? a : b;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    @Unmanaged
    private static native float minImpl(float a, float b);

    @Unmanaged
    public static float min(float a, float b) {
        if (PlatformDetector.isJavaScript()) {
            return minImpl(a, b);
        }
        if (a != a) {
            return a;
        }
        if (a == 0 && b == 0 && 1 / b == Float.NEGATIVE_INFINITY) {
            return b;
        }
        return a <= b ? a : b;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    @Unmanaged
    private static native float maxImpl(float a, float b);

    @Unmanaged
    public static float max(float a, float b) {
        if (PlatformDetector.isJavaScript()) {
            return maxImpl(a, b);
        }
        if (a != a) {
            return a;
        }
        if (a == 0 && b == 0 && 1 / a == Float.NEGATIVE_INFINITY) {
            return b;
        }
        return a >= b ? a : b;
    }

    public static int abs(int n) {
        return n >= 0 ? n : -n;
    }

    public static int absExact(int n) {
        if (n == Integer.MIN_VALUE) {
            throw new ArithmeticException();
        } else {
            return abs(n);
        }
    }

    public static long abs(long n) {
        return n >= 0 ? n : -n;
    }

    public static long absExact(long n) {
        if (n == Long.MIN_VALUE) {
            throw new ArithmeticException();
        } else {
            return abs(n);
        }
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    private static native float absImpl(float d);

    @Import(name = "fabs")
    private static native float absC(float d);

    public static float abs(float n) {
        if (PlatformDetector.isJavaScript() || PlatformDetector.isWebAssemblyGC()) {
            return absImpl(n);
        } else if (PlatformDetector.isC()) {
            return absC(n);
        }
        return n <= 0f ? 0f - n : n;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    private static native double absImpl(double d);

    @Import(name = "fabs")
    private static native double absC(double d);

    public static double abs(double n) {
        if (PlatformDetector.isJavaScript() || PlatformDetector.isWebAssemblyGC()) {
            return absImpl(n);
        } else if (PlatformDetector.isC()) {
            return absC(n);
        }
        return n <= 0.0 ? 0.0 - n : n;
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
            bits = 1L << Math.max(0, exponent - 1);
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
        if (bits >= 24 << 23) {
            bits -= 23 << 23;
        } else {
            int exponent = bits >> 23;
            bits = 1 << Math.max(0, exponent - 1);
        }
        return TFloat.intBitsToFloat(bits);
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    private static native double sign(double d);

    public static double signum(double d) {
        if (PlatformDetector.isJavaScript()) {
            return sign(d);
        }
        if (Double.isNaN(d)) {
            return d;
        }
        return d < 0 ? -1 : d > 0 ? 1 : d;
    }

    @GeneratedBy(MathNativeGenerator.class)
    @NoSideEffects
    private static native float sign(float d);

    public static float signum(float d) {
        if (PlatformDetector.isJavaScript()) {
            return sign(d);
        }
        if (Double.isNaN(d)) {
            return d;
        }
        return d < 0 ? -1 : d > 0 ? 1 : d;
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
        return Float.intBitsToFloat((Float.floatToRawIntBits(sign) & Integer.MIN_VALUE)
                | (Float.floatToRawIntBits(magnitude) & Integer.MAX_VALUE));
    }

    public static double copySign(double magnitude, double sign) {
        return Double.longBitsToDouble((Double.doubleToRawLongBits(sign) & Long.MIN_VALUE)
                | (Double.doubleToRawLongBits(magnitude) & Long.MAX_VALUE));
    }

    public static int getExponent(double d) {
        long bits = TDouble.doubleToRawLongBits(d);
        int exponent = (int) ((bits >> 52) & 0x7FF);
        return exponent - 1023;
    }

    public static int getExponent(float f) {
        int bits = TFloat.floatToRawIntBits(f);
        int exponent = (bits >> 23) & 0xFF;
        return exponent - 127;
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
        if (TDouble.isNaN(d) || d == TDouble.POSITIVE_INFINITY) {
            return d;
        }
        if (d == 0.0d) {
            return Double.MIN_VALUE;
        }
        long bits = TDouble.doubleToLongBits(d);
        if (d < 0) {
            bits--;
        } else {
            bits++;
        }
        return TDouble.longBitsToDouble(bits);
    }

    public static float nextUp(float d) {
        if (TFloat.isNaN(d) || d == TFloat.POSITIVE_INFINITY) {
            return d;
        }
        if (d == 0) {
            return Float.MIN_VALUE;
        }
        int bits = TFloat.floatToIntBits(d);
        if (d < 0) {
            bits--;
        } else {
            bits++;
        }
        return TFloat.intBitsToFloat(bits);
    }

    public static double nextDown(double d) {
        if (TDouble.isNaN(d) || d == TDouble.NEGATIVE_INFINITY) {
            return d;
        }
        if (d == 0.0d) {
            return -Double.MIN_VALUE;
        }
        long bits = TDouble.doubleToLongBits(d);
        if (d < 0) {
            bits++;
        } else {
            bits--;
        }
        return TDouble.longBitsToDouble(bits);
    }

    public static float nextDown(float d) {
        if (TFloat.isNaN(d) || d == TFloat.NEGATIVE_INFINITY) {
            return d;
        }
        if (d == 0) {
            return -Float.MIN_VALUE;
        }
        int bits = TFloat.floatToIntBits(d);
        if (d < 0) {
            bits++;
        } else {
            bits--;
        }
        return TFloat.intBitsToFloat(bits);
    }

    public static int clamp(long value, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException();
        }
        return (int) Math.min(max, Math.max(value, min));
    }

    public static long clamp(long value, long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException();
        }
        return Math.min(max, Math.max(value, min));
    }

    public static double clamp(double value, double min, double max) {
        if (!(min < max) && (Double.isNaN(min) || Double.isNaN(max) || Double.compare(min, max) > 0)) {
            throw new IllegalArgumentException();
        }
        return Math.min(max, Math.max(value, min));
    }

    public static float clamp(float value, float min, float max) {
        if (!(min < max) && (Float.isNaN(min) || Float.isNaN(max) || Float.compare(min, max) > 0)) {
            throw new IllegalArgumentException();
        }
        return Math.min(max, Math.max(value, min));
    }

    public static double scalb(double d, int scaleFactor) {
        if (scaleFactor == 0) {
            return d;
        }

        var bits = Double.doubleToRawLongBits(d);
        var exponent = (bits >>> 52) & 0x7FF;

        // infinity and NaN cases
        if (exponent == 0x7FF) {
            return d;
        }

        var mantissa = bits & 0xFFFFFFFFFFFFFL;

        // subnormal case
        if (exponent == 0) {
            if (scaleFactor < 0) {
                mantissa >>>= Math.min(63, -scaleFactor);
                return Double.longBitsToDouble((bits & 0xFFF0000000000000L) | mantissa);
            }
            var significantBits = 64 - Long.numberOfLeadingZeros(mantissa);

            // we still stay subnormal after scaling up
            if (significantBits + scaleFactor <= 52) {
                mantissa <<= Math.min(63, scaleFactor);
                return Double.longBitsToDouble((bits & 0xFFF0000000000000L) | mantissa);
            }

            // rescale up to normal and proceed with
            var shift = 53 - significantBits;
            mantissa = (mantissa << shift) & 0xFFFFFFFFFFFFFL;
            exponent++;
            scaleFactor -= shift;
        }

        // regular case

        if (-scaleFactor >= exponent) {
            // after rescaling down we get subnormal number
            mantissa = (mantissa | 0x10000000000000L) >> Math.min(-scaleFactor - exponent + 1, 53);
            exponent = 0;
        } else if (scaleFactor > 0x7FE - exponent) {
            // after rescaling up we get infinity
            return d < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        } else {
            exponent += scaleFactor;
        }

        return Double.longBitsToDouble((bits & (1L << 63)) | (exponent << 52) | mantissa);
    }

    public static float scalb(float f, int scaleFactor) {
        if (scaleFactor == 0) {
            return f;
        }

        var bits = Float.floatToIntBits(f);
        var exponent = (bits >>> 23) & 0xFF;

        // infinity and NaN cases
        if (exponent == 0xFF) {
            return f;
        }

        var mantissa = bits & 0x7FFFFF;

        // subnormal case
        if (exponent == 0) {
            if (scaleFactor < 0) {
                mantissa >>>= Math.min(31, -scaleFactor);
                return Float.intBitsToFloat((bits & 0x7F800000) | mantissa);
            }
            var significantBits = 32 - Integer.numberOfLeadingZeros(mantissa);

            // we still stay subnormal after scaling up
            if (significantBits + scaleFactor <= 23) {
                mantissa <<= Math.min(31, scaleFactor);
                return Float.intBitsToFloat((bits & 0x7F800000) | mantissa);
            }

            // rescale up to normal and proceed with
            var shift = 24 - significantBits;
            mantissa = (mantissa << shift) & 0x7FFFFF;
            exponent++;
            scaleFactor -= shift;
        }

        // regular case

        if (-scaleFactor >= exponent) {
            // after rescaling down we get subnormal number
            mantissa = (mantissa | 0x800000) >> Math.min(-scaleFactor - exponent + 1, 24);
            exponent = 0;
        } else if (scaleFactor > 0xFE - exponent) {
            // after rescaling up we get infinity
            return f < 0 ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        } else {
            exponent += scaleFactor;
        }

        return Float.intBitsToFloat((bits & (1 << 31)) | (exponent << 23) | mantissa);
    }
}
